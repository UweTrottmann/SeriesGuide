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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
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
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

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

    private DetailsAdapter mAdapter;

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

        mAdapter = new DetailsAdapter(getActivity(), null, 0);

        setListAdapter(mAdapter);

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
        int itemId = item.getItemId();
        if (itemId == R.id.menu_rate_trakt) {
            onShareEpisode(ShareMethod.RATE_TRAKT, true);
            fireTrackerEvent("Rate (trakt)");
            return true;
        } else if (itemId == R.id.menu_share) {
            onShareEpisode(ShareMethod.OTHER_SERVICES, true);
            fireTrackerEvent("Share (apps)");
            return true;
        } else if (itemId == R.id.menu_manage_lists) {
            ListsDialogFragment.showListsDialog(String.valueOf(getEpisodeId()), 3,
                    getFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onShareEpisode(ShareMethod shareMethod, boolean isInvalidateOptionsMenu) {
        // Episode of this fragment is always the first item in the cursor
        final Cursor episode = (Cursor) mAdapter.getItem(0);
        if (episode != null) {
            Bundle shareData = new Bundle();
            String episodestring = ShareUtils.onCreateShareString(getActivity(), episode);
            String sharestring = getString(R.string.share_checkout);
            sharestring += " \"" + episode.getString(DetailsQuery.SHOW_TITLE);
            sharestring += " - " + episodestring + "\"";
            shareData.putString(ShareItems.EPISODESTRING, episodestring);
            shareData.putString(ShareItems.SHARESTRING, sharestring);
            shareData.putInt(ShareItems.EPISODE, episode.getInt(DetailsQuery.NUMBER));
            shareData.putInt(ShareItems.SEASON, episode.getInt(DetailsQuery.SEASON));
            shareData.putInt(ShareItems.TVDBID, episode.getInt(DetailsQuery.REF_SHOW_ID));

            // IMDb id
            String imdbId = episode.getString(DetailsQuery.IMDBID);
            if (TextUtils.isEmpty(imdbId)) {
                // fall back to show IMDb id
                imdbId = episode.getString(DetailsQuery.SHOW_IMDBID);
            }
            shareData.putString(ShareItems.IMDBID, imdbId);

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

    /**
     * Non-static class (!) so we can access fields of
     * {@link EpisodeDetailsFragment}. Displays one row, aka one episode.
     */
    private class DetailsAdapter extends CursorAdapter {

        private LayoutInflater mLayoutInflater;

        public DetailsAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);

            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mLayoutInflater.inflate(R.layout.episodedetails, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            mShowId = cursor.getInt(DetailsQuery.REF_SHOW_ID);
            mSeasonNumber = cursor.getInt(DetailsQuery.SEASON);
            mEpisodeNumber = cursor.getInt(DetailsQuery.NUMBER);
            final String showTitle = cursor.getString(DetailsQuery.SHOW_TITLE);
            final String episodeString = ShareUtils.onCreateShareString(
                    getSherlockActivity(), cursor);
            final long airTime = cursor.getLong(DetailsQuery.FIRSTAIREDMS);

            // Title and description
            ((TextView) view.findViewById(R.id.title))
                    .setText(cursor.getString(DetailsQuery.TITLE));
            ((TextView) view.findViewById(R.id.description)).setText(cursor
                    .getString(DetailsQuery.OVERVIEW));

            // Show title button
            TextView showtitle = (TextView) view.findViewById(R.id.showTitle);
            showtitle.setText(showTitle);
            showtitle.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), OverviewActivity.class);
                    i.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, mShowId);
                    startActivity(i);
                }
            });

            // Show poster background
            if (getArguments().getBoolean("showposter")) {
                final ImageView background = (ImageView) getActivity().findViewById(
                        R.id.episodedetails_background);
                Utils.setPosterBackground(background,
                        cursor.getString(DetailsQuery.SHOW_POSTER), getActivity());
            }

            // Air day and time
            TextView airdateText = (TextView) view.findViewById(R.id.airDay);
            TextView airtimeText = (TextView) view.findViewById(R.id.airTime);
            if (airTime != -1) {
                airdateText.setText(Utils.formatToDate(airTime, getActivity()));
                String[] dayAndTime = Utils.formatToTimeAndDay(airTime, getActivity());
                airtimeText.setText(dayAndTime[2] + " (" + dayAndTime[1] + ")");
            } else {
                airdateText.setText(R.string.unknown);
                airtimeText.setText("");
            }

            // Last edit date
            TextView lastEdit = (TextView) view.findViewById(R.id.lastEdit);
            long lastEditRaw = cursor.getLong(DetailsQuery.LASTEDIT);
            if (lastEditRaw > 0) {
                lastEdit.setText(DateUtils.formatDateTime(context, lastEditRaw * 1000,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
            } else {
                lastEdit.setText(R.string.unknown);
            }

            // DVD episode number
            Utils.setValueOrPlaceholder(view.findViewById(R.id.dvdNumber),
                    cursor.getString(DetailsQuery.DVDNUMBER));
            // Directors
            String directors = Utils.splitAndKitTVDBStrings(cursor
                    .getString(DetailsQuery.DIRECTORS));
            Utils.setValueOrPlaceholder(view.findViewById(R.id.directors), directors);
            // Writers
            String writers = Utils.splitAndKitTVDBStrings(cursor
                    .getString(DetailsQuery.WRITERS));
            Utils.setValueOrPlaceholder(view.findViewById(R.id.writers), writers);
            /*
             * Guest stars - don't display an unknown label if there are none,
             * because then there are none
             */
            String guestStars = Utils.splitAndKitTVDBStrings(cursor
                    .getString(DetailsQuery.GUESTSTARS));
            ((TextView) view.findViewById(R.id.guestStars)).setText(guestStars);

            // Episode image
            FrameLayout imageContainer = (FrameLayout) view.findViewById(R.id.imageContainer);
            String imagePath = cursor.getString(DetailsQuery.IMAGE);
            onLoadImage(imagePath, imageContainer);

            // Watched button
            mWatched = cursor.getInt(DetailsQuery.WATCHED) == 1 ? true : false;
            ImageButton seenButton = (ImageButton) view.findViewById(R.id.watchedButton);
            seenButton.setImageResource(mWatched ? R.drawable.ic_watched
                    : R.drawable.ic_action_watched);
            seenButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    fireTrackerEvent("Toggle watched");
                    onToggleWatched();
                }
            });
            CheatSheet.setup(seenButton, mWatched ? R.string.unmark_episode
                    : R.string.mark_episode);

            // Collected button
            mCollected = cursor.getInt(DetailsQuery.COLLECTED) == 1 ? true : false;
            ImageButton collectedButton = (ImageButton) view.findViewById(R.id.collectedButton);
            collectedButton.setImageResource(mCollected ? R.drawable.ic_collected
                    : R.drawable.ic_action_collect);
            collectedButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    fireTrackerEvent("Toggle collected");
                    onToggleCollected();
                }
            });
            CheatSheet.setup(collectedButton, mCollected ? R.string.uncollect
                    : R.string.collect);

            // Calendar button
            final int runtime = cursor.getInt(DetailsQuery.SHOW_RUNTIME);
            View calendarButton = view.findViewById(R.id.calendarButton);
            calendarButton.setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fireTrackerEvent("Add to calendar");
                            ShareUtils.onAddCalendarEvent(getSherlockActivity(), showTitle,
                                    episodeString, airTime, runtime);
                        }
                    });
            CheatSheet.setup(calendarButton);

            // TVDb rating
            RelativeLayout rating = (RelativeLayout) view.findViewById(R.id.ratingbar);
            String ratingText = cursor.getString(DetailsQuery.RATING);
            if (ratingText != null && ratingText.length() != 0) {
                RatingBar ratingBar = (RatingBar) rating.findViewById(R.id.bar);
                TextView ratingValue = (TextView) rating.findViewById(R.id.value);
                ratingBar.setProgress((int) (Double.valueOf(ratingText) / 0.1));
                ratingValue.setText(ratingText + "/10");
            }
            // fetch trakt ratings
            mTraktTask = new TraktSummaryTask(getSherlockActivity(), rating).episode(
                    cursor.getInt(DetailsQuery.REF_SHOW_ID),
                    cursor.getInt(DetailsQuery.SEASON),
                    cursor.getInt(DetailsQuery.NUMBER));
            AndroidUtils.executeAsyncTask(mTraktTask, new Void[] {});

            // TVDb button
            final String seasonId = cursor.getString(DetailsQuery.REF_SEASON_ID);
            view.findViewById(R.id.buttonTVDB).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri
                            .parse(Constants.TVDB_EPISODE_URL_1 + mShowId
                                    + Constants.TVDB_EPISODE_URL_2 + seasonId
                                    + Constants.TVDB_EPISODE_URL_3 + getEpisodeId()));
                    startActivity(i);
                }
            });

            // IMDb button
            String imdbId = cursor.getString(DetailsQuery.IMDBID);
            if (TextUtils.isEmpty(imdbId)) {
                // fall back to show IMDb id
                imdbId = cursor.getString(DetailsQuery.SHOW_IMDBID);
            }
            Utils.setUpImdbButton(imdbId, view.findViewById(R.id.buttonShowInfoIMDB), TAG,
                    getActivity());

            // trakt shouts button
            final String episodeTitle = cursor.getString(DetailsQuery.TITLE);
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
            final String showImdbid = cursor.getString(DetailsQuery.SHOW_IMDBID);
            view.findViewById(R.id.checkinButton).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // display a check-in dialog
                    CheckInDialogFragment f = CheckInDialogFragment.newInstance(showImdbid,
                            mShowId, mSeasonNumber, mEpisodeNumber, episodeString);
                    f.show(getFragmentManager(), "checkin-dialog");
                }
            });

        }
    }

    interface DetailsQuery {

        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Shows.REF_SHOW_ID, Episodes.OVERVIEW,
                Episodes.NUMBER, Episodes.SEASON, Episodes.WATCHED, Episodes.FIRSTAIREDMS,
                Episodes.DIRECTORS, Episodes.GUESTSTARS, Episodes.WRITERS,
                Tables.EPISODES + "." + Episodes.RATING, Episodes.IMAGE, Episodes.DVDNUMBER,
                Episodes.TITLE, Shows.TITLE, Shows.IMDBID, Shows.RUNTIME, Shows.POSTER,
                Seasons.REF_SEASON_ID, Episodes.COLLECTED, Episodes.IMDBID, Episodes.LASTEDIT
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

        int IMDBID = 20;

        int LASTEDIT = 21;
    }

    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), Episodes.buildEpisodeWithShowUri(String
                .valueOf(getEpisodeId())), DetailsQuery.PROJECTION, null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onFlagCompleted(FlagAction action, int showId, int itemId, boolean isSuccessful) {
        if (isSuccessful && isAdded()) {
            getLoaderManager().restartLoader(EPISODE_LOADER, null, this);
        }
    }
}
