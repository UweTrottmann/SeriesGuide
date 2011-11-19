
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ShareUtils.TraktCredentialsDialogFragment;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowSeason;
import com.jakewharton.trakt.services.ShowService.EpisodeSeenBuilder;
import com.jakewharton.trakt.services.ShowService.EpisodeUnseenBuilder;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TraktSync extends AsyncTask<Void, Void, Integer> {

    private static final int SUCCESS_WORK = 100;

    private static final int SUCCESS_NOWORK = 101;

    private static final int FAILED_CREDENTIALS = 102;

    private static final int FAILED_API = 103;

    private FragmentActivity mContext;

    private String mResult;

    private boolean mIsSyncToTrakt;

    private View mContainer;

    private boolean mIsSyncingUnseen;

    public TraktSync(FragmentActivity activity, View container, boolean isSyncToTrakt,
            boolean isSyncingUnseen) {
        mContext = activity;
        mContainer = container;
        mIsSyncToTrakt = isSyncToTrakt;
        mIsSyncingUnseen = isSyncingUnseen;
    }

    @Override
    protected void onPreExecute() {
        if (mIsSyncToTrakt) {
            mContainer.findViewById(R.id.progressBarToTraktSync).setVisibility(View.VISIBLE);
        } else {
            mContainer.findViewById(R.id.progressBarToDeviceSync).setVisibility(View.VISIBLE);
        }
        mContainer.findViewById(R.id.syncToDeviceButton).setEnabled(false);
        mContainer.findViewById(R.id.syncToTraktButton).setEnabled(false);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (!ShareUtils.isTraktCredentialsValid(mContext)) {
            return FAILED_CREDENTIALS;
        }

        // get trakt.tv credentials
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext
                .getApplicationContext());
        ServiceManager manager = new ServiceManager();
        final String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
        String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, "");
        try {
            password = SimpleCrypto.decrypt(password, mContext);
        } catch (Exception e1) {
            // password could not be decrypted
            return FAILED_CREDENTIALS;
        }
        manager.setAuthentication(username, password);
        manager.setApiKey(Constants.TRAKT_API_KEY);

        if (mIsSyncToTrakt) {
            return syncToTrakt(manager);
        } else {
            return syncToSeriesGuide(manager, username);
        }
    }

    private Integer syncToSeriesGuide(ServiceManager manager, String username) {
        mResult = "";

        List<TvShow> shows;
        try {
            // get watched episodes from trakt
            shows = manager.userService().libraryShowsWatched(username).fire();
        } catch (TraktException te) {
            return FAILED_API;
        } catch (ApiException e) {
            return FAILED_API;
        }

        // get show ids in local database
        Cursor showTvdbIds = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[] {
            Shows._ID
        }, null, null, null);

        // assume we have a local list of which shows to sync (later...)
        while (showTvdbIds.moveToNext()) {
            String tvdbId = showTvdbIds.getString(0);
            for (TvShow tvShow : shows) {
                if (tvdbId.equalsIgnoreCase(tvShow.getTvdbId())) {
                    if (mResult.length() != 0) {
                        mResult += ", ";
                    }

                    if (mIsSyncingUnseen) {
                        ContentValues values = new ContentValues();
                        values.put(Episodes.WATCHED, false);
                        mContext.getContentResolver().update(
                                Episodes.buildEpisodesOfShowUri(tvdbId), values, null, null);
                    }

                    final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

                    // go through watched seasons, try to match them with local
                    // season
                    List<TvShowSeason> seasons = tvShow.getSeasons();
                    for (TvShowSeason season : seasons) {
                        Cursor seasonMatch = mContext.getContentResolver().query(
                                Seasons.buildSeasonsOfShowUri(tvdbId), new String[] {
                                    Seasons._ID
                                }, Seasons.COMBINED + "=?", new String[] {
                                    season.getSeason().toString()
                                }, null);

                        // if we found a season, go on with its episodes
                        if (seasonMatch.moveToFirst()) {
                            String seasonId = seasonMatch.getString(0);

                            // build episodes update query to mark seen episodes

                            for (Integer episode : season.getEpisodes().getNumbers()) {
                                batch.add(ContentProviderOperation
                                        .newUpdate(Episodes.buildEpisodesOfSeasonUri(seasonId))
                                        .withSelection(Episodes.NUMBER + "=?", new String[] {
                                            episode.toString()
                                        }).withValue(Episodes.WATCHED, true).build());
                            }

                        }

                        seasonMatch.close();
                    }

                    // last chance to abort before doing work
                    if (isCancelled()) {
                        showTvdbIds.close();
                        return null;
                    }

                    try {
                        mContext.getContentResolver().applyBatch(SeriesContract.CONTENT_AUTHORITY,
                                batch);
                    } catch (RemoteException e) {
                        // Failed binder transactions aren't recoverable
                        throw new RuntimeException("Problem applying batch operation", e);
                    } catch (OperationApplicationException e) {
                        // Failures like constraint violation aren't
                        // recoverable
                        throw new RuntimeException("Problem applying batch operation", e);
                    }

                    mResult += tvShow.getTitle();

                    // remove synced show
                    shows.remove(tvShow);
                    break;
                }
            }
        }

        showTvdbIds.close();
        if (mResult.length() != 0) {
            return SUCCESS_WORK;
        } else {
            return SUCCESS_NOWORK;
        }
    }

    private Integer syncToTrakt(ServiceManager manager) {
        // get show ids in local database for which syncing is enabled
        Cursor showTvdbIds = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[] {
            Shows._ID
        }, Shows.SYNCENABLED + "=1", null, null);

        if (showTvdbIds.getCount() == 0) {
            return SUCCESS_NOWORK;
        }

        while (showTvdbIds.moveToNext()) {
            String tvdbId = showTvdbIds.getString(0);
            EpisodeSeenBuilder builder = manager.showService().episodeSeen(Integer.valueOf(tvdbId));

            // build seen episodes trakt post
            Cursor seenEpisodes = mContext.getContentResolver().query(
                    Episodes.buildEpisodesOfShowUri(tvdbId), new String[] {
                            Episodes.SEASON, Episodes.NUMBER
                    }, Episodes.WATCHED + "=?", new String[] {
                        "1"
                    }, null);
            if (seenEpisodes.getCount() == 0) {
                builder = null;
            } else {
                while (seenEpisodes.moveToNext()) {
                    int season = seenEpisodes.getInt(0);
                    int episode = seenEpisodes.getInt(1);
                    builder.episode(season, episode);
                }
            }
            seenEpisodes.close();

            // build unseen episodes trakt post
            EpisodeUnseenBuilder builderUnseen = null;
            if (mIsSyncingUnseen) {
                builderUnseen = manager.showService().episodeUnseen(Integer.valueOf(tvdbId));
                Cursor unseenEpisodes = mContext.getContentResolver().query(
                        Episodes.buildEpisodesOfShowUri(tvdbId), new String[] {
                                Episodes.SEASON, Episodes.NUMBER
                        }, Episodes.WATCHED + "=?", new String[] {
                            "0"
                        }, null);
                if (unseenEpisodes.getCount() == 0) {
                    builderUnseen = null;
                } else {
                    while (unseenEpisodes.moveToNext()) {
                        int season = unseenEpisodes.getInt(0);
                        int episode = unseenEpisodes.getInt(1);
                        builderUnseen.episode(season, episode);
                    }
                }
                unseenEpisodes.close();
            }

            // last chance to abort
            if (isCancelled()) {
                showTvdbIds.close();
                return null;
            }

            try {
                // mark episodes of show
                if (builder != null) {
                    builder.fire();
                }
                if (mIsSyncingUnseen && builderUnseen != null) {
                    builderUnseen.fire();
                }
            } catch (TraktException te) {
                return FAILED_API;
            } catch (ApiException e) {
                return FAILED_API;
            }
        }

        showTvdbIds.close();
        return SUCCESS_WORK;
    }

    @Override
    protected void onCancelled() {
        Toast.makeText(mContext, "Sync cancelled", Toast.LENGTH_LONG).show();
        restoreViewStates();
    }

    @Override
    protected void onPostExecute(Integer result) {
        String message = "";
        int duration = Toast.LENGTH_SHORT;

        switch (result) {
            case SUCCESS_WORK:
                message = "Finished syncing";
                if (mResult != null) {
                    message += " (" + mResult + ")";
                }
                break;
            case SUCCESS_NOWORK:
                message = "There was nothing to sync.";
                break;
            case FAILED_CREDENTIALS:
                message = "Your credentials are incomplete. Please enter them again.";
                duration = Toast.LENGTH_LONG;
                TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                        .newInstance();
                FragmentTransaction ft = mContext.getSupportFragmentManager().beginTransaction();
                newFragment.show(ft, "traktcredentialsdialog");
                break;
            case FAILED_API:
                message = "Could not communicate with trakt servers. Try again later.";
                duration = Toast.LENGTH_LONG;
                break;
        }

        Toast.makeText(mContext, message, duration).show();
        restoreViewStates();
    }

    private void restoreViewStates() {
        if (mIsSyncToTrakt) {
            mContainer.findViewById(R.id.progressBarToTraktSync).setVisibility(View.GONE);
        } else {
            mContainer.findViewById(R.id.progressBarToDeviceSync).setVisibility(View.GONE);
        }
        mContainer.findViewById(R.id.syncToDeviceButton).setEnabled(true);
        mContainer.findViewById(R.id.syncToTraktButton).setEnabled(true);
    }
}
