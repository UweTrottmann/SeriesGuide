
package com.battlelancer.seriesguide.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.services.ShowService;
import com.jakewharton.trakt.services.ShowService.EpisodeSeenBuilder;
import com.jakewharton.trakt.services.ShowService.EpisodeUnlibraryBuilder;
import com.jakewharton.trakt.services.ShowService.EpisodeUnseenBuilder;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

/**
 * Helps flag episodes in the local database and on trakt.tv.
 */
public class FlagTask extends AsyncTask<Void, Integer, Integer> {

    private static final int FAILED = -1;

    private static final int OFFLINE = -2;

    private static final int SUCCESS = 0;

    private static final String TAG = "FlagTask";

    public interface OnFlagListener {
        /**
         * Called if flags were sent to trakt or failed to got sent and were not
         * set locally. If trakt is not used, called once flags are set in the
         * local database.
         */
        public void onFlagCompleted(FlagAction action, int showId, int itemId, boolean isSuccessful);
    }

    public enum FlagAction {
        EPISODE_WATCHED,

        EPISODE_COLLECTED,

        EPISODE_WATCHED_PREVIOUS,

        SEASON_WATCHED,

        SEASON_COLLECTED,

        SHOW_WATCHED,

        SHOW_COLLECTED;
    }

    private Context mContext;

    private int mShowId;

    private int mItemId;

    private int mSeason;

    private int mEpisode;

    private long mFirstAired;

    private boolean mIsFlag;

    private FlagAction mAction;

    private OnFlagListener mListener;

    private boolean mIsTraktInvolved;

    public FlagTask(Context context, int showId, OnFlagListener listener) {
        mContext = context;
        mShowId = showId;
        mListener = listener;
    }

    /**
     * Set the item id of the episode, season or show.
     */
    public FlagTask setItemId(int itemId) {
        mItemId = itemId;
        return this;
    }

    /**
     * Set whether to add or remove the flag. This value is ignored when using
     * {@link #episodeWatchedPrevious()} as it always adds the watched flag.
     */
    public FlagTask setFlag(boolean flag) {
        mIsFlag = flag;
        return this;
    }

    public FlagTask episodeWatched(int seasonNumber, int episodeNumber) {
        mSeason = seasonNumber;
        mEpisode = episodeNumber;
        mAction = FlagAction.EPISODE_WATCHED;
        return this;
    }

    public FlagTask episodeCollected(int seasonNumber, int episodeNumber) {
        mSeason = seasonNumber;
        mEpisode = episodeNumber;
        mAction = FlagAction.EPISODE_COLLECTED;
        return this;
    }

    /**
     * Flag all episodes previous to this one as watched. Previous in terms of
     * air date.
     * 
     * @param firstaired
     */
    public FlagTask episodeWatchedPrevious(long firstaired) {
        mFirstAired = firstaired;
        mAction = FlagAction.EPISODE_WATCHED_PREVIOUS;
        return this;
    }

    public FlagTask seasonWatched(int seasonNumber) {
        mSeason = seasonNumber;
        mEpisode = -1;
        mAction = FlagAction.SEASON_WATCHED;
        return this;
    }

    public FlagTask seasonCollected(int seasonNumber) {
        mSeason = seasonNumber;
        mEpisode = -1;
        mAction = FlagAction.SEASON_COLLECTED;
        return this;
    }

    public FlagTask showWatched() {
        mAction = FlagAction.SHOW_WATCHED;
        return this;
    }

    public FlagTask showCollected() {
        mAction = FlagAction.SHOW_COLLECTED;
        return this;
    }

    /**
     * Run the task on the thread pool.
     */
    public void execute() {
        AndroidUtils.executeAsyncTask(this, new Void[] {});
    }

    @Override
    protected Integer doInBackground(Void... params) {
        mIsTraktInvolved = ServiceUtils.isTraktCredentialsValid(mContext);

        // do trakt stuff
        // check for valid trakt credentials
        if (mIsTraktInvolved) {
            if (!AndroidUtils.isNetworkConnected(mContext)) {
                return OFFLINE;
            }

            ServiceManager manager = ServiceUtils.getTraktServiceManagerWithAuth(mContext, false);
            if (manager == null) {
                return FAILED;
            }

            ShowService showService = manager.showService();
            try {
                switch (mAction) {
                    case EPISODE_WATCHED:
                        // flag a single episode watched
                        if (mIsFlag) {
                            showService.episodeSeen(mShowId).episode(mSeason, mEpisode).fire();
                        } else {
                            showService.episodeUnseen(mShowId).episode(mSeason, mEpisode).fire();
                        }
                        break;
                    case EPISODE_COLLECTED:
                        // flag a single episode collected
                        if (mIsFlag) {
                            showService.episodeLibrary(mShowId).episode(mSeason, mEpisode).fire();
                        } else {
                            showService.episodeUnlibrary(mShowId).episode(mSeason, mEpisode).fire();
                        }
                        break;
                    case SEASON_WATCHED:
                        // flag a whole season watched
                        if (mIsFlag) {
                            showService.seasonSeen(mShowId).season(mSeason).fire();
                        } else {
                            removeEpisodeWatchedFlags(showService).fire();
                        }
                        break;
                    case SEASON_COLLECTED:
                        // flag a whole season collected
                        if (mIsFlag) {
                            showService.seasonLibrary(mShowId).season(mSeason).fire();
                        } else {
                            removeEpisodeCollectedFlags(showService).fire();
                        }
                        break;
                    case SHOW_WATCHED:
                        // flag a whole show watched
                        if (mIsFlag) {
                            showService.showSeen(mShowId).fire();
                        } else {
                            removeEpisodeWatchedFlags(showService).fire();
                        }
                        break;
                    case SHOW_COLLECTED:
                        // flag a whole show collected
                        if (mIsFlag) {
                            showService.showLibrary(mShowId).fire();
                        } else {
                            removeEpisodeCollectedFlags(showService).fire();
                        }
                        break;
                    case EPISODE_WATCHED_PREVIOUS:
                        // flag episodes up to one episode
                        addEpisodeWatchedFlags(showService).fire();
                        break;
                    default:
                        break;
                }
            } catch (TraktException e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
                return FAILED;
            } catch (ApiException e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
                return FAILED;
            }
        }

        // update local database if trakt did not fail or if it is not used
        updateDatabase(mItemId);
        setLastWatchedEpisode();

        return SUCCESS;
    }

    private EpisodeSeenBuilder addEpisodeWatchedFlags(ShowService showService) {
        EpisodeSeenBuilder builder = showService.episodeSeen(mShowId);

        // determine uri
        Uri uri;
        String selection;
        switch (mAction) {
            case EPISODE_WATCHED_PREVIOUS:
                if (mFirstAired <= 0) {
                    return builder;
                }
                uri = Episodes.buildEpisodesOfShowUri(String.valueOf(mShowId));
                selection = Episodes.FIRSTAIREDMS + "<" + mFirstAired + " AND "
                        + Episodes.FIRSTAIREDMS
                        + ">0";
                break;
            default:
                return builder;
        }

        // query and add episodes to builder
        final Cursor episodes = mContext.getContentResolver().query(
                uri,
                new String[] {
                        Episodes.SEASON, Episodes.NUMBER
                }, selection, null, null);
        if (episodes != null) {
            while (episodes.moveToNext()) {
                builder.episode(episodes.getInt(0), episodes.getInt(1));
            }
            episodes.close();
        }

        return builder;
    }

    private EpisodeUnseenBuilder removeEpisodeWatchedFlags(ShowService showService) {
        EpisodeUnseenBuilder builder = showService.episodeUnseen(mShowId);

        // determine uri
        Uri uri;
        switch (mAction) {
            case SEASON_WATCHED:
                uri = Episodes.buildEpisodesOfSeasonUri(String.valueOf(mItemId));
                break;
            case SHOW_WATCHED:
                uri = Episodes.buildEpisodesOfShowUri(String.valueOf(mShowId));
                break;
            default:
                return builder;
        }

        // query and add episodes to builder
        final Cursor episodes = mContext.getContentResolver().query(
                uri,
                new String[] {
                        Episodes.SEASON, Episodes.NUMBER
                }, null, null, null);
        if (episodes != null) {
            while (episodes.moveToNext()) {
                builder.episode(episodes.getInt(0), episodes.getInt(1));
            }
            episodes.close();
        }

        return builder;
    }

    private EpisodeUnlibraryBuilder removeEpisodeCollectedFlags(ShowService showService) {
        EpisodeUnlibraryBuilder builder = showService.episodeUnlibrary(mShowId);

        // determine uri
        Uri uri;
        switch (mAction) {
            case SEASON_COLLECTED:
                uri = Episodes.buildEpisodesOfSeasonUri(String.valueOf(mItemId));
                break;
            case SHOW_COLLECTED:
                uri = Episodes.buildEpisodesOfShowUri(String.valueOf(mShowId));
                break;
            default:
                return builder;
        }

        // query and add episodes to builder
        final Cursor episodes = mContext.getContentResolver().query(
                uri,
                new String[] {
                        Episodes.SEASON, Episodes.NUMBER
                }, null, null, null);
        if (episodes != null) {
            while (episodes.moveToNext()) {
                builder.episode(episodes.getInt(0), episodes.getInt(1));
            }
            episodes.close();
        }

        return builder;
    }

    /** Lower season or if season is equal has to have a lower episode number. */
    private static final String SELECTION_PREVIOUS_WATCHED = Episodes.SEASON + "<? OR "
            + "(" + Episodes.SEASON + "=? AND " + Episodes.NUMBER + "<?)";

    private static final String ORDER_PREVIOUS_WATCHED = Episodes.FIRSTAIREDMS + " DESC,"
            + Episodes.SEASON + " DESC,"
            + Episodes.NUMBER + " DESC";

    private static final String[] PROJECTION_EPISODE = new String[] {
            Episodes._ID
    };

    /**
     * Determines the latest watched episode and stores it for the show.
     */
    private void setLastWatchedEpisode() {
        int lastWatchedId = -1;

        if (mIsFlag) {
            // adding watched flag
            switch (mAction) {
                case EPISODE_WATCHED:
                case EPISODE_WATCHED_PREVIOUS:
                    // take the given episode id
                    lastWatchedId = mItemId;
                    break;
                case SEASON_WATCHED:
                    // get the last flagged episode of the season
                    final Cursor seasonEpisodes = mContext.getContentResolver().query(
                            Episodes.buildEpisodesOfSeasonUri(String.valueOf(mItemId)),
                            PROJECTION_EPISODE, null, null, Episodes.NUMBER + " DESC");
                    if (seasonEpisodes != null) {
                        if (!seasonEpisodes.moveToFirst()) {
                            lastWatchedId = seasonEpisodes.getInt(0);
                        }

                        seasonEpisodes.close();
                    }
                    break;
                case EPISODE_COLLECTED:
                case SHOW_WATCHED:
                default:
                    // we don't care
                    return;
            }
        } else {
            // removing watched flag
            switch (mAction) {
                case EPISODE_WATCHED:
                    final Cursor show = mContext.getContentResolver().query(
                            Shows.buildShowUri(String.valueOf(mShowId)),
                            new String[] {
                                    Shows._ID, Shows.LASTWATCHEDID
                            }, null, null, null);
                    if (show != null) {
                        // identical to last watched episode?
                        if (show.moveToFirst() && show.getInt(1) == mItemId) {
                            // get latest watched before this one
                            String season = String.valueOf(mSeason);
                            final Cursor latestWatchedEpisode = mContext.getContentResolver()
                                    .query(Episodes.buildEpisodesOfShowUri(String
                                            .valueOf(mShowId)),
                                            PROJECTION_EPISODE, SELECTION_PREVIOUS_WATCHED,
                                            new String[] {
                                                    season, season, String.valueOf(mEpisode)
                                            }, ORDER_PREVIOUS_WATCHED);
                            if (latestWatchedEpisode != null) {
                                if (latestWatchedEpisode.moveToFirst()) {
                                    lastWatchedId = latestWatchedEpisode.getInt(0);
                                }

                                latestWatchedEpisode.close();
                            }
                        }

                        show.close();
                    }
                    break;
                case SEASON_WATCHED:
                case SHOW_WATCHED:
                    // just reset
                    lastWatchedId = 0;
                    break;
                case EPISODE_COLLECTED:
                case EPISODE_WATCHED_PREVIOUS:
                default:
                    // not relevant to us
                    return;
            }
        }

        if (lastWatchedId != -1) {
            // set latest watched
            ContentValues values = new ContentValues();
            values.put(Shows.LASTWATCHEDID, lastWatchedId);
            mContext.getContentResolver().update(Shows.buildShowUri(String.valueOf(mShowId)),
                    values,
                    null, null);
        }
    }

    private void updateDatabase(int itemId) {
        // determine flag column
        String column;
        switch (mAction) {
            case EPISODE_WATCHED:
            case EPISODE_WATCHED_PREVIOUS:
            case SEASON_WATCHED:
            case SHOW_WATCHED:
                column = Episodes.WATCHED;
                break;
            case EPISODE_COLLECTED:
            case SEASON_COLLECTED:
            case SHOW_COLLECTED:
                column = Episodes.COLLECTED;
                break;
            default:
                return;
        }

        // determine query uri
        Uri uri;
        switch (mAction) {
            case EPISODE_WATCHED:
            case EPISODE_COLLECTED:
                uri = Episodes.buildEpisodeUri(String.valueOf(itemId));
                break;
            case SEASON_WATCHED:
            case SEASON_COLLECTED:
                uri = Episodes.buildEpisodesOfSeasonUri(String.valueOf(itemId));
                break;
            case EPISODE_WATCHED_PREVIOUS:
            case SHOW_WATCHED:
            case SHOW_COLLECTED:
                uri = Episodes.buildEpisodesOfShowUri(String.valueOf(mShowId));
                break;
            default:
                return;
        }

        // build and execute query
        ContentValues values = new ContentValues();
        if (mAction == FlagAction.EPISODE_WATCHED_PREVIOUS) {
            // mark all previously aired episodes
            if (mFirstAired > 0) {
                values.put(column, true);
                mContext.getContentResolver().update(
                        uri,
                        values,
                        Episodes.FIRSTAIREDMS + "<" + mFirstAired + " AND " + Episodes.FIRSTAIREDMS
                                + ">0", null);
            }
        } else {
            values.put(column, mIsFlag);
            mContext.getContentResolver().update(uri, values, null, null);
        }

        // notify the content provider for udpates
        mContext.getContentResolver().notifyChange(Episodes.CONTENT_URI, null);
    }

    @Override
    protected void onPostExecute(Integer result) {
        // display a small toast if submission to trakt was successful
        if (mIsTraktInvolved) {
            int message;
            switch (mAction) {
                case EPISODE_WATCHED:
                case SEASON_WATCHED:
                    if (mIsFlag) {
                        message = R.string.trakt_seen;
                    } else {
                        message = R.string.trakt_notseen;
                    }
                    break;
                case EPISODE_COLLECTED:
                case SEASON_COLLECTED:
                    if (mIsFlag) {
                        message = R.string.trakt_collected;
                    } else {
                        message = R.string.trakt_notcollected;
                    }
                    break;
                default:
                    message = 0;
                    break;
            }

            int status = 0;
            int duration = 0;
            switch (result) {
                case SUCCESS: {
                    status = R.string.trakt_submitsuccess;
                    duration = Toast.LENGTH_SHORT;
                    break;
                }
                case FAILED: {
                    status = R.string.trakt_submitfailed;
                    duration = Toast.LENGTH_LONG;
                    break;
                }
                case OFFLINE: {
                    status = R.string.offline;
                    duration = Toast.LENGTH_LONG;
                    break;
                }
            }

            if (mAction == FlagAction.SHOW_WATCHED || mAction == FlagAction.SHOW_COLLECTED
                    || mAction == FlagAction.EPISODE_WATCHED_PREVIOUS) {
                // simple ack
                Toast.makeText(mContext,
                        mContext.getString(status),
                        duration).show();
            } else {
                // detailed ack
                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(mContext);
                String number = Utils.getEpisodeNumber(prefs, mSeason, mEpisode);
                Toast.makeText(mContext,
                        mContext.getString(message, number) + " " + mContext.getString(status),
                        duration).show();
            }
        }

        if (mListener != null) {
            mListener.onFlagCompleted(mAction, mShowId, mItemId, result == SUCCESS ? true : false);
        }
    }
}
