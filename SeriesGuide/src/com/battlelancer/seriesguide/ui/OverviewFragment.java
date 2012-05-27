/*
 * Copyright 2011 Uwe Trottmann
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
 */

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FetchArtTask;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.ShareUtils.ShareMethod;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class OverviewFragment extends SherlockFragment implements OnTraktActionCompleteListener {

    private boolean mDualPane;

    private Series mShow;

    private ImageCache imageCache;

    protected long mEpisodeId;

    private FetchArtTask mArtTask;

    private TraktSummaryTask mTraktTask;

    final private Bundle mShareData = new Bundle();

    private long mAirtime;

    private boolean mCollected;

    private int mSeasonNumber;

    private int mEpisodeNumber;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {
        String SHOW_TVDBID = "show_tvdbid";
    }

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent("Overview", "Click", label, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.overview_fragment, container, false);
        v.findViewById(R.id.showinfo).setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                onShowShowInfo();
            }
        });
        View seasonsButton = v.findViewById(R.id.gotoseasons);
        if (seasonsButton != null) {
            seasonsButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    showSeasons();
                }
            });
        }

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Overview");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getShowId() == 0) {
            getActivity().finish();
        }

        imageCache = ImageCache.getInstance(getActivity());

        fillShowData();

        setHasOptionsMenu(true);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View seasonsFragment = getActivity().findViewById(R.id.fragment_seasons);
        mDualPane = seasonsFragment != null && seasonsFragment.getVisibility() == View.VISIBLE;

        if (mDualPane) {
            showSeasons();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        onLoadEpisode();
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
        inflater.inflate(R.menu.overview_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search: {
                getActivity().onSearchRequested();
                return true;
            }
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
        ShareUtils.onShareEpisode(getActivity(), mShareData, shareMethod, this);

        if (isInvalidateOptionsMenu) {
            // invalidate the options menu so a potentially new
            // quick share action is displayed
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    private void showSeasons() {
        if (mDualPane) {
            // Check if fragment is shown, create new if needed.
            SeasonsFragment seasons = (SeasonsFragment) getFragmentManager().findFragmentById(
                    R.id.fragment_seasons);
            if (seasons == null) {
                // Make new fragment to show this selection.
                seasons = SeasonsFragment.newInstance(getShowId());

                // Execute a transaction, replacing any existing
                // fragment with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.fragment_slide_left_enter,
                        R.anim.fragment_slide_left_exit);
                ft.replace(R.id.fragment_seasons, seasons);
                ft.commit();
            }
        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            Intent intent = new Intent();
            intent.setClass(getActivity(), SeasonsActivity.class);
            intent.putExtra(SeasonsFragment.InitBundle.SHOW_TVDBID, getShowId());
            startActivity(intent);
            getSherlockActivity().overridePendingTransition(R.anim.fragment_slide_left_enter,
                    R.anim.fragment_slide_left_exit);
        }

    }

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void fillShowData() {
        mShow = DBUtils.getShow(getActivity(), String.valueOf(getShowId()));

        if (mShow == null) {
            return;
        }

        // Save info for sharing
        mShareData.putString(ShareItems.IMDBID, mShow.getImdbId());
        mShareData.putInt(ShareItems.TVDBID, Integer.valueOf(mShow.getId()));

        // Show name
        TextView showname = (TextView) getActivity().findViewById(R.id.seriesname);
        showname.setText(mShow.getSeriesName());

        // Running state
        TextView status = (TextView) getActivity().findViewById(R.id.showStatus);
        if (mShow.getStatus() == 1) {
            status.setTextColor(Color.GREEN);
            status.setText(getString(R.string.show_isalive));
        } else if (mShow.getStatus() == 0) {
            status.setTextColor(Color.GRAY);
            status.setText(getString(R.string.show_isnotalive));
        }

        // Poster
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
            // using alpha seems not to work on eclair, so only set a
            // background on froyo+ then
            final ImageView background = (ImageView) getActivity().findViewById(R.id.background);
            Bitmap bg = imageCache.get(mShow.getPoster());
            if (bg != null) {
                BitmapDrawable drawable = new BitmapDrawable(getResources(), bg);
                drawable.setAlpha(50);
                background.setImageDrawable(drawable);
            }
        }

        // Airtime and Network
        String timeAndNetwork = "";
        if (mShow.getAirsDayOfWeek().length() != 0 && mShow.getAirsTime() != -1) {
            String[] values = Utils.parseMillisecondsToTime(mShow.getAirsTime(),
                    mShow.getAirsDayOfWeek(), getActivity());
            timeAndNetwork += values[1] + " " + values[0];
        } else {
            timeAndNetwork += getString(R.string.show_noairtime);
        }
        if (mShow.getNetwork().length() != 0) {
            timeAndNetwork += " " + getString(R.string.show_network) + " " + mShow.getNetwork();
        }
        TextView showmeta = (TextView) getActivity().findViewById(R.id.showmeta);
        showmeta.setText(timeAndNetwork);
    }

    private void fillEpisodeData() {
        final Activity context = getActivity();
        if (context == null) {
            return;
        }

        TextView nextheader = (TextView) context.findViewById(R.id.nextheader);
        TextView episodetitle = (TextView) context.findViewById(R.id.TextViewEpisodeTitle);
        TextView numbers = (TextView) context.findViewById(R.id.TextViewEpisodeNumbers);

        // // start share string
        String sharestring = getString(R.string.share_checkout);
        String episodestring = "";
        sharestring += " \"" + ((TextView) context.findViewById(R.id.seriesname)).getText();

        if (mEpisodeId != 0) {
            episodetitle.setVisibility(View.VISIBLE);
            numbers.setVisibility(View.VISIBLE);
            LinearLayout episodemeta = (LinearLayout) context.findViewById(R.id.episodemeta);
            episodemeta.setVisibility(View.VISIBLE);

            // final Bundle episode = mDbHelper.getEpisodeDetails(episodeid);
            final Cursor episode = context.getContentResolver().query(
                    Episodes.buildEpisodeUri(String.valueOf(mEpisodeId)), EpisodeQuery.PROJECTION,
                    null, null, null);
            episode.moveToFirst();

            // Airdate
            mAirtime = episode.getLong(EpisodeQuery.FIRSTAIREDMS);
            if (mAirtime != -1) {
                final String[] dayAndTime = Utils.formatToTimeAndDay(mAirtime, context);
                nextheader.setText(dayAndTime[2] + " (" + dayAndTime[1] + "):");
            }

            // create share string
            episodestring = ShareUtils.onCreateShareString(context, episode);

            onLoadEpisodeDetails(episode);

            episode.close();
        } else {
            // no next episode, display single line info text, remove other
            // views
            nextheader.setText("  " + getString(R.string.no_nextepisode));
            episodetitle.setVisibility(View.GONE);
            numbers.setVisibility(View.GONE);
            LinearLayout episodemeta = (LinearLayout) context.findViewById(R.id.episodemeta);
            episodemeta.setVisibility(View.GONE);
        }

        // animate view into visibility
        final View overviewContainer = context.findViewById(R.id.overview_container);
        if (overviewContainer.getVisibility() == View.GONE) {
            final View progressContainer = context.findViewById(R.id.progress_container);
            progressContainer.startAnimation(AnimationUtils.loadAnimation(context,
                    android.R.anim.fade_out));
            overviewContainer.startAnimation(AnimationUtils.loadAnimation(context,
                    android.R.anim.fade_in));
            progressContainer.setVisibility(View.GONE);
            overviewContainer.setVisibility(View.VISIBLE);
        }

        // finish share string
        sharestring += " - " + episodestring + "\" via @SeriesGuide";
        mShareData.putString(ShareItems.SHARESTRING, sharestring);
        mShareData.putString(ShareItems.EPISODESTRING, episodestring);
    }

    protected void onLoadEpisode() {
        new AsyncTask<Void, Void, Integer>() {

            private final static int SUCCESS = 1;

            private final static int ABORT = -1;

            private SherlockFragmentActivity activity;

            @Override
            protected Integer doInBackground(Void... params) {
                activity = getSherlockActivity();
                if (activity == null) {
                    return ABORT;
                }

                mEpisodeId = DBUtils.updateLatestEpisode(activity, String.valueOf(getShowId()));

                return SUCCESS;
            }

            @Override
            protected void onPostExecute(Integer result) {
                // TODO do not reload all information every time, we would only
                // need to load the new collected flag status and relative time
                // (maybe ratings, too)
                if (result == SUCCESS && isAdded()) {
                    activity.invalidateOptionsMenu();
                    fillEpisodeData();
                }
            }

        }.execute();
    }

    protected void onLoadEpisodeDetails(final Cursor episode) {
        mSeasonNumber = episode.getInt(EpisodeQuery.SEASON);
        mEpisodeNumber = episode.getInt(EpisodeQuery.NUMBER);

        // populate share bundle
        mShareData.putInt(ShareItems.TVDBID, getShowId());
        mShareData.putInt(ShareItems.SEASON, mSeasonNumber);
        mShareData.putInt(ShareItems.EPISODE, mEpisodeNumber);

        // Episode title
        final String episodeTitle = episode.getString(EpisodeQuery.TITLE);
        ((TextView) getView().findViewById(R.id.TextViewEpisodeTitle)).setText(episodeTitle);

        // Season and episode number
        ((TextView) getView().findViewById(R.id.TextViewEpisodeNumbers))
                .setText(getString(R.string.season) + " " + mSeasonNumber + " "
                        + getString(R.string.episode) + " " + mEpisodeNumber);

        // Check in button
        getView().findViewById(R.id.checkinButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckInDialogFragment f = CheckInDialogFragment.newInstance(
                        mShareData.getString(ShareItems.IMDBID), getShowId(), mSeasonNumber,
                        mEpisodeNumber, mShareData.getString(ShareItems.EPISODESTRING));
                f.show(getFragmentManager(), "checkin-dialog");
            }
        });

        // Watched button
        getView().findViewById(R.id.watchedButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMarkWatched();
            }
        });

        // Collected button
        mCollected = episode.getInt(EpisodeQuery.COLLECTED) == 1 ? true : false;

        ImageButton collectedButton = (ImageButton) getView().findViewById(R.id.collectedButton);
        collectedButton.setImageResource(mCollected ? R.drawable.ic_collected
                : R.drawable.ic_action_collect);
        collectedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleCollected();
                ((ImageButton) v).setImageResource(mCollected ? R.drawable.ic_collected
                        : R.drawable.ic_action_collect);
            }
        });

        // Calendar button
        getView().findViewById(R.id.calendarButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fireTrackerEvent("Add to calendar");
                ShareUtils.onAddCalendarEvent(getActivity(), mShow.getSeriesName(),
                        mShareData.getString(ShareItems.EPISODESTRING), mAirtime,
                        mShow.getRuntime());
            }
        });

        // Description, DVD episode number, Directors, Writers
        ((TextView) getView().findViewById(R.id.TextViewEpisodeDescription)).setText(episode
                .getString(EpisodeQuery.OVERVIEW));
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.textViewEpisodeDVDnumber),
                episode.getString(EpisodeQuery.DVDNUMBER));
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewEpisodeDirectors),
                Utils.splitAndKitTVDBStrings(episode.getString(EpisodeQuery.DIRECTORS)));
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewEpisodeWriters),
                Utils.splitAndKitTVDBStrings(episode.getString(EpisodeQuery.WRITERS)));

        // Guest stars
        // don't display an unknown string if there are no gueststars, because
        // then there are none
        ((TextView) getView().findViewById(R.id.TextViewEpisodeGuestStars)).setText(Utils
                .splitAndKitTVDBStrings(episode.getString(EpisodeQuery.GUESTSTARS)));

        // TVDb rating
        String ratingText = episode.getString(EpisodeQuery.RATING);
        if (ratingText != null && ratingText.length() != 0) {
            ((RatingBar) getView().findViewById(R.id.bar)).setProgress((int) (Double
                    .valueOf(ratingText) / 0.1));
            ((TextView) getView().findViewById(R.id.value)).setText(ratingText + "/10");
        }

        // IMDb and TVDb button
        final String seasonId = episode.getString(EpisodeQuery.REF_SEASON_ID);
        getView().findViewById(R.id.buttonShowInfoIMDB).setVisibility(View.GONE);
        getView().findViewById(R.id.buttonTVDB).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TVDB_EPISODE_URL_1
                        + getShowId() + Constants.TVDB_EPISODE_URL_2 + seasonId
                        + Constants.TVDB_EPISODE_URL_3 + mEpisodeId));
                startActivity(i);
            }
        });

        // trakt shouts button
        getView().findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mDualPane) {
                    Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                    i.putExtras(TraktShoutsActivity.createInitBundle(getShowId(), mSeasonNumber,
                            mEpisodeNumber, episodeTitle));
                    startActivity(i);
                } else {
                    TraktShoutsFragment newFragment = TraktShoutsFragment.newInstance(episodeTitle,
                            getShowId(), mSeasonNumber, mEpisodeNumber);
                    newFragment.show(getFragmentManager(), "shouts-dialog");
                }
            }
        });

        // Episode image
        String imagePath = episode.getString(EpisodeQuery.IMAGE);
        onLoadImage(imagePath);

        // trakt rating
        mTraktTask = new TraktSummaryTask(getSherlockActivity(), getView()).episode(getShowId(),
                mSeasonNumber, mEpisodeNumber);
        if (Utils.isHoneycombOrHigher()) {
            mTraktTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            mTraktTask.execute();
        }
    }

    protected void onLoadImage(String imagePath) {
        final FrameLayout container = (FrameLayout) getActivity().findViewById(R.id.imageContainer);

        // clean up a previous task
        if (mArtTask != null) {
            mArtTask.cancel(true);
            mArtTask = null;
        }
        mArtTask = (FetchArtTask) new FetchArtTask(imagePath, container, getActivity()).execute();
    }

    private void onMarkWatched() {
        DBUtils.markEpisode(getActivity(), String.valueOf(mEpisodeId), true);
        DBUtils.markSeenOnTrakt(getActivity(), getShowId(), mSeasonNumber, mEpisodeNumber, true);

        Toast.makeText(getActivity(), getString(R.string.mark_episode), Toast.LENGTH_SHORT).show();

        // load new episode, update seasons (if shown)
        onLoadEpisode();
        onUpdateSeasons();
    }

    private void onToggleCollected() {
        mCollected = !mCollected;
        DBUtils.collectEpisode(getActivity(), String.valueOf(mEpisodeId), mCollected);
        DBUtils.markCollectedOnTrakt(getActivity(), getShowId(), mSeasonNumber, mEpisodeNumber,
                mCollected);
    }

    private void onUpdateSeasons() {
        SeasonsFragment seasons = (SeasonsFragment) getFragmentManager().findFragmentById(
                R.id.fragment_seasons);
        if (seasons != null) {
            seasons.updateUnwatchedCounts(false);
        }
    }

    /**
     * Launch show info activity.
     */
    private void onShowShowInfo() {
        Intent i = new Intent(getActivity(), ShowInfoActivity.class);
        i.putExtra(ShowInfoActivity.InitBundle.SHOW_TVDBID, getShowId());
        startActivity(i);
    }

    @Override
    public void onTraktActionComplete(boolean wasSuccessfull) {
        // load new episode, update seasons (if shown)
        onLoadEpisode();
        onUpdateSeasons();
    }

    interface EpisodeQuery {

        String[] PROJECTION = new String[] {
                Episodes._ID, Shows.REF_SHOW_ID, Episodes.OVERVIEW, Episodes.NUMBER,
                Episodes.SEASON, Episodes.WATCHED, Episodes.FIRSTAIREDMS, Episodes.DIRECTORS,
                Episodes.GUESTSTARS, Episodes.WRITERS, Episodes.RATING, Episodes.IMAGE,
                Episodes.DVDNUMBER, Episodes.TITLE, Seasons.REF_SEASON_ID, Episodes.COLLECTED
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

        int REF_SEASON_ID = 14;

        int COLLECTED = 15;
    }
}
