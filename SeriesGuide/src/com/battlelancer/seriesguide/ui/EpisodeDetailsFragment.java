/*
 * Copyright 2012 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.util.FetchArtTask;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.FlagTask.FlagAction;
import com.battlelancer.seriesguide.util.FlagTask.OnFlagListener;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.ShareUtils.ShareMethod;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Displays details about a single episode like summary, ratings and episode
 * image if available.
 */
public class EpisodeDetailsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnFlagListener {

    private static final int EPISODE_LOADER = 3;

    private static final String TAG = "EpisodeDetails";

    private FetchArtTask mArtTask;

    private TraktSummaryTask mTraktTask;

    private SimpleCursorAdapter mAdapter;

    protected boolean mWatched;

    protected boolean mCollected;

    protected int mShowId;

    protected int mSeasonNumber;

    protected int mEpisodeNumber;

    /**
     * Data which has to be passed when creating this fragment.
     */
    public interface InitBundle {
        /**
         * Integer extra.
         */
        String EPISODE_TVDBID = "episode_tvdbid";

        /**
         * Boolean extra.
         */
        String IS_POSTERBACKGROUND = "showposter";
    }

    public static EpisodeDetailsFragment newInstance(int episodeId, boolean isShowingPoster) {
        EpisodeDetailsFragment f = new EpisodeDetailsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(InitBundle.EPISODE_TVDBID, episodeId);
        args.putBoolean("showposter", isShowingPoster);
        f.setArguments(args);

        return f;
    }

    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent(TAG, "Click", label, (long) 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
         * never use this here (on config change the view needed before removing
         * the fragment)
         */
        // if (container == null) {
        // return null;
        // }
        return inflater.inflate(R.layout.episodedetails_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupAdapter();

        getLoaderManager().initLoader(EPISODE_LOADER, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().trackView("Episode Details");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mArtTask != null) {
            mArtTask.cancel(true);
            mArtTask = null;
        }
        if (mTraktTask != null) {
            mTraktTask.cancel(true);
            mTraktTask = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodedetails_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_rate_trakt: {
                fireTrackerEvent("Rate (trakt)");
                onShareEpisode(ShareMethod.RATE_TRAKT, true);
                return true;
            }
            case R.id.menu_share: {
                fireTrackerEvent("Share (apps)");
                onShareEpisode(ShareMethod.OTHER_SERVICES, true);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void onShareEpisode(ShareMethod shareMethod, boolean isInvalidateOptionsMenu) {
        final Cursor episode = (Cursor) getListAdapter().getItem(0);
        if (episode != null && episode.moveToFirst()) {
            Bundle shareData = new Bundle();
            String episodestring = ShareUtils.onCreateShareString(getActivity(), episode);
            String sharestring = getString(R.string.share_checkout);
            sharestring += " \"" + episode.getString(EpisodeDetailsQuery.SHOW_TITLE);
            sharestring += " - " + episodestring + "\"";
            shareData.putString(ShareItems.EPISODESTRING, episodestring);
            shareData.putString(ShareItems.SHARESTRING, sharestring);
            shareData.putString(ShareItems.IMDBID,
                    episode.getString(EpisodeDetailsQuery.SHOW_IMDBID));
            shareData.putInt(ShareItems.EPISODE, episode.getInt(EpisodeDetailsQuery.NUMBER));
            shareData.putInt(ShareItems.SEASON, episode.getInt(EpisodeDetailsQuery.SEASON));
            shareData.putInt(ShareItems.TVDBID, episode.getInt(EpisodeDetailsQuery.REF_SHOW_ID));

            // don't close cursor!
            // episode.close();

            ShareUtils.onShareEpisode(getActivity(), shareData, shareMethod, null);

            if (isInvalidateOptionsMenu) {
                // invalidate the options menu so a potentially new
                // quick share action is displayed
                getSherlockActivity().invalidateOptionsMenu();
            }
        }
    }

    private void setupAdapter() {

        String[] from = new String[] {
                Episodes.TITLE, Episodes.OVERVIEW, Episodes.FIRSTAIREDMS, Episodes.DVDNUMBER,
                Episodes.DIRECTORS, Episodes.GUESTSTARS, Episodes.WRITERS, Episodes.RATING,
                Episodes.IMAGE, Shows.TITLE, Episodes.WATCHED, Episodes.COLLECTED,
                Shows.REF_SHOW_ID

        };
        int[] to = new int[] {
                R.id.TextViewEpisodeTitle, R.id.TextViewEpisodeDescription,
                R.id.episodedetails_root, R.id.textViewEpisodeDVDnumber,
                R.id.TextViewEpisodeDirectors, R.id.TextViewEpisodeGuestStars,
                R.id.TextViewEpisodeWriters, R.id.ratingbar, R.id.imageContainer,
                R.id.textViewEpisodeDetailsShowName, R.id.watchedButton, R.id.collectedButton,
                R.id.episodedetails_root
        };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.episodedetails, null, from, to,
                0);
        mAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, Cursor episode, int columnIndex) {
                if (columnIndex == EpisodeDetailsQuery.WATCHED) {
                    mWatched = episode.getInt(EpisodeDetailsQuery.WATCHED) == 1 ? true : false;

                    // Watched button
                    ImageButton seenButton = (ImageButton) view;
                    seenButton.setImageResource(mWatched ? R.drawable.ic_watched
                            : R.drawable.ic_action_watched);
                    seenButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fireTrackerEvent("Toggle watched");
                            onToggleWatched();
                        }
                    });

                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.COLLECTED) {
                    mCollected = episode.getInt(EpisodeDetailsQuery.COLLECTED) == 1 ? true : false;

                    // Collected button
                    ImageButton seenButton = (ImageButton) view;
                    seenButton.setImageResource(mCollected ? R.drawable.ic_collected
                            : R.drawable.ic_action_collect);
                    seenButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fireTrackerEvent("Toggle collected");
                            onToggleCollected();
                        }
                    });

                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.FIRSTAIREDMS) {
                    TextView airdateText = (TextView) view
                            .findViewById(R.id.TextViewEpisodeFirstAirdate);
                    TextView airtimeText = (TextView) view.findViewById(R.id.episode_airtime);

                    // First airdate
                    final long airtime = episode.getLong(EpisodeDetailsQuery.FIRSTAIREDMS);
                    if (airtime != -1) {
                        airdateText.setText(Utils.formatToDate(airtime, getActivity()));
                        String[] dayAndTime = Utils.formatToTimeAndDay(airtime, getActivity());
                        airtimeText.setText(dayAndTime[2] + " (" + dayAndTime[1] + ")");
                    } else {
                        airdateText.setText(getString(R.string.unknown));
                        airtimeText.setText("");
                    }
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.DVDNUMBER) {
                    // DVD episode number
                    final String value = episode.getString(EpisodeDetailsQuery.DVDNUMBER);
                    Utils.setValueOrPlaceholder(view, value);
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.DIRECTORS) {
                    // Directors
                    final String value = Utils.splitAndKitTVDBStrings(episode
                            .getString(EpisodeDetailsQuery.DIRECTORS));
                    Utils.setValueOrPlaceholder(view, value);
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.GUESTSTARS) {
                    // Guest stars
                    // don't display an unknown label if there are none, because
                    // then there are none
                    final String value = Utils.splitAndKitTVDBStrings(episode
                            .getString(EpisodeDetailsQuery.GUESTSTARS));
                    TextView field = (TextView) view;
                    field.setText(value);
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.WRITERS) {
                    // Writers
                    final String value = Utils.splitAndKitTVDBStrings(episode
                            .getString(EpisodeDetailsQuery.WRITERS));
                    Utils.setValueOrPlaceholder(view, value);
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.RATING) {
                    // TVDb rating
                    RelativeLayout rating = (RelativeLayout) view;
                    String ratingText = episode.getString(EpisodeDetailsQuery.RATING);
                    if (ratingText != null && ratingText.length() != 0) {
                        RatingBar ratingBar = (RatingBar) rating.findViewById(R.id.bar);
                        TextView ratingValue = (TextView) rating.findViewById(R.id.value);
                        ratingBar.setProgress((int) (Double.valueOf(ratingText) / 0.1));
                        ratingValue.setText(ratingText + "/10");
                    }

                    // trakt rating
                    mTraktTask = new TraktSummaryTask(getSherlockActivity(), rating).episode(
                            episode.getInt(EpisodeDetailsQuery.REF_SHOW_ID),
                            episode.getInt(EpisodeDetailsQuery.SEASON),
                            episode.getInt(EpisodeDetailsQuery.NUMBER));
                    AndroidUtils.executeAsyncTask(mTraktTask, new Void[] {
                            null
                    });

                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.IMAGE) {
                    FrameLayout imageContainer = (FrameLayout) view;
                    String imagePath = episode.getString(EpisodeDetailsQuery.IMAGE);
                    onLoadImage(imagePath, imageContainer);
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.SHOW_TITLE) {
                    TextView showtitle = (TextView) view;
                    showtitle.setText(episode.getString(EpisodeDetailsQuery.SHOW_TITLE));
                    final int showId = episode.getInt(EpisodeDetailsQuery.REF_SHOW_ID);
                    showtitle.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            Intent i = new Intent(getActivity(), OverviewActivity.class);
                            i.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, showId);
                            startActivity(i);
                        }
                    });

                    // Poster
                    if (getArguments().getBoolean("showposter")) {
                        final ImageView background = (ImageView) getActivity().findViewById(
                                R.id.episodedetails_background);
                        Utils.setPosterBackground(background,
                                episode.getString(EpisodeDetailsQuery.SHOW_POSTER), getActivity());
                    }

                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.REF_SHOW_ID) {
                    mShowId = episode.getInt(EpisodeDetailsQuery.REF_SHOW_ID);
                    final String seasonTvdbid = episode
                            .getString(EpisodeDetailsQuery.REF_SEASON_ID);
                    mSeasonNumber = episode.getInt(EpisodeDetailsQuery.SEASON);
                    mEpisodeNumber = episode.getInt(EpisodeDetailsQuery.NUMBER);
                    final String episodeString = ShareUtils.onCreateShareString(
                            getSherlockActivity(), episode);

                    // IMDb and TVDb button
                    view.findViewById(R.id.buttonShowInfoIMDB).setVisibility(View.GONE);
                    view.findViewById(R.id.buttonTVDB).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri
                                    .parse(Constants.TVDB_EPISODE_URL_1 + mShowId
                                            + Constants.TVDB_EPISODE_URL_2 + seasonTvdbid
                                            + Constants.TVDB_EPISODE_URL_3 + getEpisodeId()));
                            startActivity(i);
                        }
                    });

                    // trakt shouts button
                    final String episodeTitle = episode.getString(EpisodeDetailsQuery.TITLE);
                    view.findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // see if we are attached to a single-pane activity
                            if (getSherlockActivity() instanceof EpisodeDetailsActivity) {
                                Intent intent = new Intent(getActivity(), TraktShoutsActivity.class);
                                intent.putExtras(TraktShoutsActivity.createInitBundle(mShowId,
                                        mSeasonNumber, mEpisodeNumber, episodeTitle));
                                startActivity(intent);
                            } else {
                                // in a multi-pane layout show the shouts in a
                                // dialog
                                TraktShoutsFragment newFragment = TraktShoutsFragment.newInstance(
                                        episodeTitle, mShowId, mSeasonNumber, mEpisodeNumber);

                                newFragment.show(getFragmentManager(), "shouts-dialog");
                            }
                        }
                    });

                    // Check in button
                    final String showImdbid = episode.getString(EpisodeDetailsQuery.SHOW_IMDBID);
                    view.findViewById(R.id.checkinButton).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // display a check-in dialog
                            CheckInDialogFragment f = CheckInDialogFragment.newInstance(showImdbid,
                                    mShowId, mSeasonNumber, mEpisodeNumber, episodeString);
                            f.show(getFragmentManager(), "checkin-dialog");
                        }
                    });

                    // Calendar button
                    final String showTitle = episode.getString(EpisodeDetailsQuery.SHOW_TITLE);
                    final long airtime = episode.getLong(EpisodeDetailsQuery.FIRSTAIREDMS);
                    final String runtime = episode.getString(EpisodeDetailsQuery.SHOW_RUNTIME);
                    view.findViewById(R.id.calendarButton).setOnClickListener(
                            new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    fireTrackerEvent("Add to calendar");
                                    ShareUtils.onAddCalendarEvent(getSherlockActivity(), showTitle,
                                            episodeString, airtime, runtime);
                                }
                            });
                    return true;
                }
                return false;
            }
        });

        setListAdapter(mAdapter);
    }

    public int getEpisodeId() {
        return getArguments().getInt(InitBundle.EPISODE_TVDBID);
    }

    protected void onLoadImage(String imagePath, FrameLayout container) {
        if (mArtTask == null || mArtTask.getStatus() == AsyncTask.Status.FINISHED) {
            mArtTask = (FetchArtTask) new FetchArtTask(imagePath, container, getActivity());
            AndroidUtils.executeAsyncTask(mArtTask, new Void[] {
                    null
            });
        }
    }

    private void onToggleWatched() {
        mWatched = !mWatched;
        new FlagTask(getActivity(), mShowId, this).episodeWatched(mSeasonNumber, mEpisodeNumber)
                .setItemId(getEpisodeId()).setFlag(mWatched).execute();
    }

    private void onToggleCollected() {
        mCollected = !mCollected;
        new FlagTask(getActivity(), mShowId, this).episodeCollected(mSeasonNumber, mEpisodeNumber)
                .setItemId(getEpisodeId()).setFlag(mCollected).execute();
    }

    interface EpisodeDetailsQuery {

        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Shows.REF_SHOW_ID, Episodes.OVERVIEW,
                Episodes.NUMBER, Episodes.SEASON, Episodes.WATCHED, Episodes.FIRSTAIREDMS,
                Episodes.DIRECTORS, Episodes.GUESTSTARS, Episodes.WRITERS,
                Tables.EPISODES + "." + Episodes.RATING, Episodes.IMAGE, Episodes.DVDNUMBER,
                Episodes.TITLE, Shows.TITLE, Shows.IMDBID, Shows.RUNTIME, Shows.POSTER,
                Seasons.REF_SEASON_ID, Episodes.COLLECTED
        };

        int _ID = 0;

        int REF_SHOW_ID = 1;

        int OVERVIEW = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int WATCHED = 5;

        int FIRSTAIREDMS = 6;

        int DIRECTORS = 7;

        int GUESTSTARS = 8;

        int WRITERS = 9;

        int RATING = 10;

        int IMAGE = 11;

        int DVDNUMBER = 12;

        int TITLE = 13;

        int SHOW_TITLE = 14;

        int SHOW_IMDBID = 15;

        int SHOW_RUNTIME = 16;

        int SHOW_POSTER = 17;

        int REF_SEASON_ID = 18;

        int COLLECTED = 19;
    }

    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), Episodes.buildEpisodeWithShowUri(String
                .valueOf(getEpisodeId())), EpisodeDetailsQuery.PROJECTION, null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onFlagCompleted(FlagAction action, int showId, int itemId, boolean isSuccessful) {
        if (isSuccessful) {
            getLoaderManager().restartLoader(EPISODE_LOADER, null, this);
        }
    }
}
