package com.battlelancer.seriesguide.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.extensions.ActionsFragmentContract;
import com.battlelancer.seriesguide.extensions.EpisodeActionsHelper;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.loaders.EpisodeActionsLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.LanguageTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Shadows;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktRatingsTask;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.FeedbackView;
import com.squareup.picasso.Callback;
import com.uwetrottmann.androidutils.CheatSheet;
import de.greenrobot.event.EventBus;
import java.util.Date;
import java.util.List;
import timber.log.Timber;

/**
 * Displays general information about a show and its next episode.
 */
public class OverviewFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, ActionsFragmentContract {

    private static final String TAG = "Overview";
    private static final String ARG_EPISODE_TVDB_ID = "episodeTvdbId";

    @BindView(R.id.containerOverviewShow) View containerShow;
    @Nullable @BindView(R.id.viewStubOverviewFeedback) ViewStub feedbackViewStub;
    @Nullable @BindView(R.id.feedbackViewOverview) FeedbackView feedbackView;
    @BindView(R.id.containerOverviewEpisode) View containerEpisode;
    @BindView(R.id.containerEpisodeActions) LinearLayout containerActions;
    @BindView(R.id.background) ImageView imageBackground;
    @BindView(R.id.imageViewOverviewEpisode) ImageView imageEpisode;
    @BindView(R.id.viewOverviewShadowStart) View shadowStart;
    @BindView(R.id.viewOverviewShadowEnd) View shadowEnd;

    @BindView(R.id.episodeTitle) TextView textEpisodeTitle;
    @BindView(R.id.episodeTime) TextView textEpisodeTime;
    @BindView(R.id.episodeInfo) TextView textEpisodeNumbers;
    @BindView(R.id.episode_primary_container) View containerEpisodePrimary;
    @BindView(R.id.episode_meta_container) View containerEpisodeMeta;
    @BindView(R.id.dividerHorizontalOverviewEpisodeMeta) View dividerEpisodeMeta;
    @BindView(R.id.progress_container) View containerProgress;
    @BindView(R.id.containerRatings) View containerRatings;
    @BindView(R.id.buttonEpisodeCheckin) Button buttonCheckin;
    @BindView(R.id.buttonEpisodeWatched) Button buttonWatch;
    @BindView(R.id.buttonEpisodeCollected) Button buttonCollect;
    @BindView(R.id.buttonEpisodeSkip) Button buttonSkip;

    @BindView(R.id.TextViewEpisodeDescription) TextView textDescription;
    @BindView(R.id.labelDvd) View labelDvdNumber;
    @BindView(R.id.textViewEpisodeDVDnumber) TextView textDvdNumber;
    @BindView(R.id.labelGuestStars) View labelGuestStars;
    @BindView(R.id.TextViewEpisodeGuestStars) TextView textGuestStars;
    @BindView(R.id.textViewRatingsValue) TextView textRating;
    @BindView(R.id.textViewRatingsVotes) TextView textRatingVotes;
    @BindView(R.id.textViewRatingsUser) TextView textUserRating;

    @BindView(R.id.buttonShowInfoIMDB) View buttonImdb;
    @BindView(R.id.buttonTVDB) View buttonTvdb;
    @BindView(R.id.buttonTrakt) View buttonTrakt;
    @BindView(R.id.buttonWebSearch) View buttonWebSearch;
    @BindView(R.id.buttonShouts) View buttonComments;

    private Handler handler = new Handler();
    private TraktRatingsTask traktRatingsTask;
    private Unbinder unbinder;

    private boolean isEpisodeDataAvailable;
    private Cursor currentEpisodeCursor;
    private int currentEpisodeTvdbId;

    private boolean isShowDataAvailable;
    private Cursor showCursor;
    private int showTvdbId;
    private String showTitle;

    private boolean hasSetEpisodeWatched;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {

        String SHOW_TVDBID = "show_tvdbid";
    }

    public static OverviewFragment newInstance(int showTvdbId) {
        OverviewFragment f = new OverviewFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showTvdbId = getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_overview, container, false);
        unbinder = ButterKnife.bind(this, v);

        v.findViewById(R.id.imageButtonFavorite).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleShowFavorited(v);
                Utils.trackAction(getActivity(), TAG, "Toggle favorited");
            }
        });
        containerEpisode.setVisibility(View.GONE);

        // check-in button
        buttonCheckin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkIn();
            }
        });
        CheatSheet.setup(buttonCheckin);

        // watched button
        buttonWatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                setEpisodeWatched();
            }
        });
        buttonWatch.setEnabled(true);
        CheatSheet.setup(buttonWatch);

        // collected button
        buttonCollect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                toggleEpisodeCollected();
            }
        });

        // skip button
        buttonSkip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                setEpisodeSkipped();
            }
        });
        buttonSkip.setEnabled(true);
        CheatSheet.setup(buttonSkip);

        // ratings
        containerRatings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rateEpisode();
            }
        });
        CheatSheet.setup(containerRatings, R.string.action_rate);

        // hide web search button
        buttonWebSearch.setVisibility(View.GONE);

        Resources res = getResources();
        if (res.getBoolean(R.bool.overview_shadow_start)) {
            Shadows.getInstance().setShadowDrawable(getContext(), shadowStart,
                    GradientDrawable.Orientation.RIGHT_LEFT);
        }
        if (res.getBoolean(R.bool.overview_shadow_end)) {
            Shadows.getInstance().setShadowDrawable(getContext(), shadowEnd,
                    GradientDrawable.Orientation.LEFT_RIGHT);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Are we in a multi-pane layout?
        View seasonsFragment = getActivity().findViewById(R.id.fragment_seasons);
        boolean multiPane = seasonsFragment != null
                && seasonsFragment.getVisibility() == View.VISIBLE;

        // do not display show info header in multi pane layout
        containerShow.setVisibility(multiPane ? View.GONE : View.VISIBLE);

        getLoaderManager().initLoader(OverviewActivity.OVERVIEW_SHOW_LOADER_ID, null, this);
        getLoaderManager().initLoader(OverviewActivity.OVERVIEW_EPISODE_LOADER_ID, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
        loadEpisodeActionsDelayed();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the fragment from
        // being garbage collected. It also prevents our callback from getting invoked even after the
        // fragment is destroyed.
        ServiceUtils.getPicasso(getActivity()).cancelRequest(imageEpisode);

        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (traktRatingsTask != null) {
            traktRatingsTask.cancel(true);
            traktRatingsTask = null;
        }
        if (handler != null) {
            handler.removeCallbacks(episodeActionsRunnable);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.overview_fragment_menu, menu);

        // enable/disable menu items
        MenuItem itemShare = menu.findItem(R.id.menu_overview_share);
        itemShare.setEnabled(isEpisodeDataAvailable);
        itemShare.setVisible(isEpisodeDataAvailable);
        MenuItem itemCalendar = menu.findItem(R.id.menu_overview_calendar);
        itemCalendar.setEnabled(isEpisodeDataAvailable);
        itemCalendar.setVisible(isEpisodeDataAvailable);
        MenuItem itemManageLists = menu.findItem(R.id.menu_overview_manage_lists);
        if (itemManageLists != null) {
            itemManageLists.setEnabled(isEpisodeDataAvailable);
            itemManageLists.setVisible(isEpisodeDataAvailable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_overview_share) {
            shareEpisode();
            return true;
        } else if (itemId == R.id.menu_overview_calendar) {
            createCalendarEvent();
            return true;
        } else if (itemId == R.id.menu_overview_manage_lists) {
            if (isEpisodeDataAvailable) {
                ManageListsDialogFragment.showListsDialog(
                        currentEpisodeCursor.getInt(EpisodeQuery._ID),
                        ListItemTypes.EPISODE, getFragmentManager());
            }
            Utils.trackAction(getActivity(), TAG, "Manage lists");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createCalendarEvent() {
        if (!isShowDataAvailable || !isEpisodeDataAvailable) {
            return;
        }
        final int seasonNumber = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        final int episodeNumber = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        final String episodeTitle = currentEpisodeCursor.getString(EpisodeQuery.TITLE);
        // add calendar event
        ShareUtils.suggestCalendarEvent(
                getActivity(),
                showCursor.getString(ShowQuery.SHOW_TITLE),
                TextTools.getNextEpisodeString(getActivity(), seasonNumber, episodeNumber,
                        episodeTitle),
                currentEpisodeCursor.getLong(EpisodeQuery.FIRST_RELEASE_MS),
                showCursor.getInt(ShowQuery.SHOW_RUNTIME)
        );

        Utils.trackAction(getActivity(), TAG, "Add to calendar");
    }

    private void checkIn() {
        if (!isEpisodeDataAvailable) {
            return;
        }
        int episodeTvdbId = currentEpisodeCursor.getInt(EpisodeQuery._ID);
        // check in
        CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(),
                episodeTvdbId);
        // don't commit fragment change after onPause
        if (f != null && isResumed()) {
            f.show(getFragmentManager(), "checkin-dialog");
            Utils.trackAction(getActivity(), TAG, "Check-In");
        }
    }

    private void setEpisodeSkipped() {
        changeEpisodeFlag(EpisodeFlags.SKIPPED);
        Utils.trackAction(getActivity(), TAG, "Flag Skipped");
    }

    private void setEpisodeWatched() {
        hasSetEpisodeWatched = true;
        changeEpisodeFlag(EpisodeFlags.WATCHED);
        Utils.trackAction(getActivity(), TAG, "Flag Watched");
    }

    private void changeEpisodeFlag(int episodeFlag) {
        if (!isEpisodeDataAvailable) {
            return;
        }
        final int season = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        final int episode = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        EpisodeTools.episodeWatched(getActivity(), showTvdbId,
                currentEpisodeCursor.getInt(EpisodeQuery._ID), season, episode, episodeFlag);
    }

    private void rateEpisode() {
        if (currentEpisodeTvdbId == 0) {
            return;
        }

        EpisodeTools.displayRateDialog(getActivity(), getFragmentManager(), currentEpisodeTvdbId);

        Utils.trackAction(getActivity(), TAG, "Rate (trakt)");
    }

    private void shareEpisode() {
        if (!isEpisodeDataAvailable) {
            return;
        }
        int seasonNumber = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        int episodeNumber = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        String episodeTitle = currentEpisodeCursor.getString(EpisodeQuery.TITLE);

        ShareUtils.shareEpisode(getActivity(), currentEpisodeTvdbId, seasonNumber, episodeNumber,
                showTitle, episodeTitle);

        Utils.trackAction(getActivity(), TAG, "Share");
    }

    private void toggleEpisodeCollected() {
        if (!isEpisodeDataAvailable) {
            return;
        }
        final int season = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        final int episode = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        final boolean isCollected = currentEpisodeCursor.getInt(EpisodeQuery.COLLECTED) == 1;
        EpisodeTools.episodeCollected(getActivity(), showTvdbId,
                currentEpisodeCursor.getInt(EpisodeQuery._ID), season, episode, !isCollected);

        Utils.trackAction(getActivity(), TAG, "Toggle Collected");
    }

    private void toggleShowFavorited(View v) {
        if (v.getTag() == null) {
            return;
        }

        // store new value
        boolean isFavorite = (Boolean) v.getTag();
        ShowTools.get(getActivity()).storeIsFavorite(showTvdbId, !isFavorite);
    }

    public static class EpisodeLoader extends CursorLoader {

        private int showTvdbId;

        public EpisodeLoader(Context context, int showTvdbId) {
            super(context);
            this.showTvdbId = showTvdbId;
            setProjection(EpisodeQuery.PROJECTION);
        }

        @Override
        public Cursor loadInBackground() {
            // get episode id, set query params
            int episodeId = (int) DBUtils.updateLatestEpisode(getContext(), showTvdbId);
            setUri(Episodes.buildEpisodeUri(episodeId));

            return super.loadInBackground();
        }
    }

    interface EpisodeQuery {

        String[] PROJECTION = new String[] {
                Episodes._ID,
                Episodes.NUMBER,
                Episodes.ABSOLUTE_NUMBER,
                Episodes.DVDNUMBER,
                Episodes.SEASON,
                Seasons.REF_SEASON_ID,
                Episodes.IMDBID,
                Episodes.TITLE,
                Episodes.OVERVIEW,
                Episodes.FIRSTAIREDMS,
                Episodes.GUESTSTARS,
                Episodes.RATING_GLOBAL,
                Episodes.RATING_VOTES,
                Episodes.RATING_USER,
                Episodes.WATCHED,
                Episodes.COLLECTED,
                Episodes.IMAGE
        };

        int _ID = 0;
        int NUMBER = 1;
        int ABSOLUTE_NUMBER = 2;
        int DVD_NUMBER = 3;
        int SEASON = 4;
        int SEASON_ID = 5;
        int IMDBID = 6;
        int TITLE = 7;
        int OVERVIEW = 8;
        int FIRST_RELEASE_MS = 9;
        int GUESTSTARS = 10;
        int RATING_GLOBAL = 11;
        int RATING_VOTES = 12;
        int RATING_USER = 13;
        int WATCHED = 14;
        int COLLECTED = 15;
        int IMAGE = 16;
    }

    interface ShowQuery {

        String[] PROJECTION = new String[] {
                Shows._ID,
                Shows.TITLE,
                Shows.STATUS,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.NETWORK,
                Shows.POSTER,
                Shows.IMDBID,
                Shows.RUNTIME,
                Shows.FAVORITE,
                Shows.LANGUAGE
        };

        int SHOW_TITLE = 1;
        int SHOW_STATUS = 2;
        int SHOW_RELEASE_TIME = 3;
        int SHOW_RELEASE_WEEKDAY = 4;
        int SHOW_RELEASE_TIMEZONE = 5;
        int SHOW_RELEASE_COUNTRY = 6;
        int SHOW_NETWORK = 7;
        int SHOW_POSTER = 8;
        int SHOW_IMDBID = 9;
        int SHOW_RUNTIME = 10;
        int SHOW_FAVORITE = 11;
        int SHOW_LANGUAGE = 12;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case OverviewActivity.OVERVIEW_EPISODE_LOADER_ID:
            default:
                return new EpisodeLoader(getActivity(), showTvdbId);
            case OverviewActivity.OVERVIEW_SHOW_LOADER_ID:
                return new CursorLoader(getActivity(), Shows.buildShowUri(String
                        .valueOf(showTvdbId)), ShowQuery.PROJECTION, null, null, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }
        switch (loader.getId()) {
            case OverviewActivity.OVERVIEW_EPISODE_LOADER_ID:
                isEpisodeDataAvailable = data != null && data.moveToFirst();
                currentEpisodeCursor = data;
                populateEpisodeViews(data);
                break;
            case OverviewActivity.OVERVIEW_SHOW_LOADER_ID:
                isShowDataAvailable = data != null && data.moveToFirst();
                showCursor = data;
                if (isShowDataAvailable) {
                    populateShowViews(data);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case OverviewActivity.OVERVIEW_EPISODE_LOADER_ID:
                isEpisodeDataAvailable = false;
                currentEpisodeCursor = null;
                break;
            case OverviewActivity.OVERVIEW_SHOW_LOADER_ID:
                isShowDataAvailable = false;
                showCursor = null;
                break;
        }
    }

    @Override
    public void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event) {
        if (currentEpisodeTvdbId == event.episodeTvdbId) {
            loadEpisodeActionsDelayed();
        }
    }

    private void populateEpisodeViews(Cursor episode) {
        maybeAddFeedbackView();

        if (isEpisodeDataAvailable) {
            // some episode properties
            currentEpisodeTvdbId = episode.getInt(EpisodeQuery._ID);

            // title
            textEpisodeTitle.setText(episode.getString(EpisodeQuery.TITLE));

            // number
            StringBuilder infoText = new StringBuilder();
            infoText.append(getString(R.string.season_number, episode.getInt(EpisodeQuery.SEASON)));
            infoText.append(" ");
            int episodeNumber = episode.getInt(EpisodeQuery.NUMBER);
            infoText.append(getString(R.string.episode_number, episodeNumber));
            int episodeAbsoluteNumber = episode.getInt(EpisodeQuery.ABSOLUTE_NUMBER);
            if (episodeAbsoluteNumber > 0 && episodeAbsoluteNumber != episodeNumber) {
                infoText.append(" (").append(episodeAbsoluteNumber).append(")");
            }
            textEpisodeNumbers.setText(infoText);

            // air date
            long releaseTime = episode.getLong(EpisodeQuery.FIRST_RELEASE_MS);
            if (releaseTime != -1) {
                Date actualRelease = TimeTools.applyUserOffset(getContext(), releaseTime);
                // "Oct 31 (Fri)" or "in 14 mins (Fri)"
                String dateTime;
                if (DisplaySettings.isDisplayExactDate(getContext())) {
                    dateTime = TimeTools.formatToLocalDateShort(getContext(), actualRelease);
                } else {
                    dateTime = TimeTools.formatToLocalRelativeTime(getContext(), actualRelease);
                }
                textEpisodeTime.setText(getString(R.string.release_date_and_day, dateTime,
                        TimeTools.formatToLocalDay(actualRelease)));
            } else {
                textEpisodeTime.setText(null);
            }

            // make title and image clickable
            containerEpisodePrimary.setOnClickListener(new OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onClick(View view) {
                    // display episode details
                    Intent intent = new Intent(getActivity(), EpisodesActivity.class);
                    intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                            currentEpisodeTvdbId);

                    Utils.startActivityWithAnimation(getActivity(), intent, view);
                }
            });
            containerEpisodePrimary.setFocusable(true);

            // collected button
            boolean isCollected = episode.getInt(EpisodeQuery.COLLECTED) == 1;
            Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(buttonCollect, 0,
                    isCollected ? R.drawable.ic_collected
                            : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                    R.attr.drawableCollect), 0, 0);
            buttonCollect.setText(isCollected ? R.string.action_collection_remove
                    : R.string.action_collection_add);
            CheatSheet.setup(buttonCollect, isCollected ? R.string.action_collection_remove
                    : R.string.action_collection_add);

            // prevent checking in if hexagon is enabled
            buttonCheckin.setVisibility(
                    HexagonTools.isSignedIn(getActivity()) ? View.GONE : View.VISIBLE);

            // buttons might have been disabled by action, re-enable
            buttonWatch.setEnabled(true);
            buttonCollect.setEnabled(true);
            buttonSkip.setEnabled(true);

            // load all other info
            populateEpisodeDetails(episode);

            // episode image
            loadEpisodeImage(episode.getString(EpisodeQuery.IMAGE));

            // episode actions
            loadEpisodeActionsDelayed();

            containerEpisodeMeta.setVisibility(View.VISIBLE);
        } else {
            // no next episode: display single line info text, remove other
            // views
            textEpisodeTitle.setText(R.string.no_nextepisode);
            textEpisodeTime.setText(null);
            textEpisodeNumbers.setText(null);
            containerEpisodePrimary.setOnClickListener(null);
            containerEpisodePrimary.setClickable(false);
            containerEpisodePrimary.setFocusable(false);
            containerEpisodeMeta.setVisibility(View.GONE);
            loadEpisodeImage(null);
        }

        // enable/disable applicable menu items
        getActivity().invalidateOptionsMenu();

        // animate view into visibility
        if (containerEpisode.getVisibility() == View.GONE) {
            containerProgress.startAnimation(AnimationUtils
                    .loadAnimation(containerProgress.getContext(), android.R.anim.fade_out));
            containerProgress.setVisibility(View.GONE);
            containerEpisode.startAnimation(AnimationUtils
                    .loadAnimation(containerEpisode.getContext(), android.R.anim.fade_in));
            containerEpisode.setVisibility(View.VISIBLE);
        }
    }

    private void maybeAddFeedbackView() {
        if (feedbackView != null || feedbackViewStub == null
                || !hasSetEpisodeWatched || !AppSettings.shouldAskForFeedback(getContext())) {
            return; // can or should not add feedback view
        }
        feedbackView = (FeedbackView) feedbackViewStub.inflate();
        feedbackViewStub = null;
        if (feedbackView != null) {
            feedbackView.setCallback(new FeedbackView.Callback() {
                @Override
                public void onRate() {
                    if (Utils.launchWebsite(getContext(), getString(R.string.url_store_page))) {
                        removeFeedbackView();
                    }
                }

                @Override
                public void onFeedback() {
                    if (Utils.tryStartActivity(getContext(),
                            HelpActivity.getFeedbackEmailIntent(getContext()), true)) {
                        removeFeedbackView();
                    }
                }

                @Override
                public void onDismiss() {
                    removeFeedbackView();
                }
            });
        }
    }

    private void removeFeedbackView() {
        if (feedbackView == null) {
            return;
        }
        feedbackView.setVisibility(View.GONE);
        AppSettings.setAskedForFeedback(getContext());
    }

    private void populateEpisodeDetails(Cursor episode) {
        // description
        populateEpisodeDescription();

        // dvd number
        boolean isShowingMeta = Utils.setLabelValueOrHide(labelDvdNumber, textDvdNumber,
                episode.getDouble(EpisodeQuery.DVD_NUMBER));
        // guest stars
        isShowingMeta |= Utils.setLabelValueOrHide(labelGuestStars, textGuestStars,
                TextTools.splitAndKitTVDBStrings(episode.getString(EpisodeQuery.GUESTSTARS)));
        // hide divider if no meta is visible
        dividerEpisodeMeta.setVisibility(isShowingMeta ? View.VISIBLE : View.GONE);

        // trakt rating
        textRating.setText(
                TraktTools.buildRatingString(episode.getDouble(EpisodeQuery.RATING_GLOBAL)));
        textRatingVotes.setText(TraktTools.buildRatingVotesString(getActivity(),
                episode.getInt(EpisodeQuery.RATING_VOTES)));

        // user rating
        textUserRating.setText(TraktTools.buildUserRatingString(getActivity(),
                episode.getInt(EpisodeQuery.RATING_USER)));

        // IMDb button
        String imdbId = episode.getString(EpisodeQuery.IMDBID);
        if (TextUtils.isEmpty(imdbId) && showCursor != null) {
            // fall back to show IMDb id
            imdbId = showCursor.getString(ShowQuery.SHOW_IMDBID);
        }
        ServiceUtils.setUpImdbButton(imdbId, buttonImdb, TAG);

        // TVDb button
        final int episodeTvdbId = episode.getInt(EpisodeQuery._ID);
        final int seasonTvdbId = episode.getInt(EpisodeQuery.SEASON_ID);
        ServiceUtils.setUpTvdbButton(showTvdbId, seasonTvdbId, episodeTvdbId, buttonTvdb, TAG);

        // trakt button
        ServiceUtils.setUpTraktEpisodeButton(buttonTrakt, currentEpisodeTvdbId, TAG);

        // trakt shouts button
        final String episodeTitle = episode.getString(EpisodeQuery.TITLE);
        buttonComments.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentEpisodeCursor != null && currentEpisodeCursor.moveToFirst()) {
                    Intent i = new Intent(getActivity(), TraktCommentsActivity.class);
                    i.putExtras(TraktCommentsActivity.createInitBundleEpisode(episodeTitle,
                            currentEpisodeTvdbId
                    ));
                    Utils.startActivityWithAnimation(getActivity(), i, v);
                    Utils.trackAction(v.getContext(), TAG, "Comments");
                }
            }
        });

        // trakt ratings
        loadTraktRatings();
    }

    /**
     * Updates the episode description. Needs both show and episode data loaded.
     */
    private void populateEpisodeDescription() {
        if (!isShowDataAvailable || !isEpisodeDataAvailable) {
            // no show or episode data available
            return;
        }
        String overview = currentEpisodeCursor.getString(EpisodeQuery.OVERVIEW);
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            textDescription.setText(getString(R.string.no_translation,
                    LanguageTools.getLanguageStringForCode(getContext(),
                            showCursor.getString(ShowQuery.SHOW_LANGUAGE)),
                    getString(R.string.tvdb)));
        } else {
            textDescription.setText(overview);
        }
    }

    @Override
    public void loadEpisodeActions() {
        if (currentEpisodeTvdbId == 0) {
            // do not load actions if there is no episode
            return;
        }
        Bundle args = new Bundle();
        args.putInt(ARG_EPISODE_TVDB_ID, currentEpisodeTvdbId);
        getLoaderManager().restartLoader(OverviewActivity.OVERVIEW_ACTIONS_LOADER_ID, args,
                episodeActionsLoaderCallbacks);
    }

    Runnable episodeActionsRunnable = new Runnable() {
        @Override
        public void run() {
            loadEpisodeActions();
        }
    };

    @Override
    public void loadEpisodeActionsDelayed() {
        handler.removeCallbacks(episodeActionsRunnable);
        handler.postDelayed(episodeActionsRunnable,
                ActionsFragmentContract.ACTION_LOADER_DELAY_MILLIS);
    }

    private void loadEpisodeImage(String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            imageEpisode.setImageDrawable(null);
            return;
        }

        // try loading image
        ServiceUtils.loadWithPicasso(getActivity(), TheTVDB.buildScreenshotUrl(imagePath))
                .error(R.drawable.ic_image_missing)
                .into(imageEpisode,
                        new Callback() {
                            @Override
                            public void onSuccess() {
                                imageEpisode.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            }

                            @Override
                            public void onError() {
                                imageEpisode.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            }
                        }
                );
    }

    private void loadTraktRatings() {
        if (!isEpisodeDataAvailable
                || (traktRatingsTask != null
                && traktRatingsTask.getStatus() != AsyncTask.Status.FINISHED)) {
            return;
        }
        int episodeTvdbId = currentEpisodeCursor.getInt(EpisodeQuery._ID);
        int seasonNumber = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        int episodeNumber = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        traktRatingsTask = new TraktRatingsTask(getActivity(), showTvdbId, episodeTvdbId,
                seasonNumber, episodeNumber);
        AsyncTaskCompat.executeParallel(traktRatingsTask);
    }

    private void populateShowViews(@NonNull Cursor show) {
        // set show title in action bar
        showTitle = show.getString(ShowQuery.SHOW_TITLE);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(showTitle);
            getActivity().setTitle(getString(R.string.description_overview) + showTitle);
        }

        if (getView() == null) {
            return;
        }

        // status
        final TextView statusText = (TextView) getView().findViewById(R.id.showStatus);
        ShowTools.setStatusAndColor(statusText, show.getInt(ShowQuery.SHOW_STATUS));

        // favorite
        final ImageButton favorited = ButterKnife.findById(getView(), R.id.imageButtonFavorite);
        boolean isFavorited = show.getInt(ShowQuery.SHOW_FAVORITE) == 1;
        if (isFavorited) {
            favorited.setImageResource(Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                    R.attr.drawableStar));
        } else {
            favorited.setImageResource(Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                    R.attr.drawableStar0));
        }
        favorited.setContentDescription(getString(isFavorited ? R.string.context_unfavorite
                : R.string.context_favorite));
        CheatSheet.setup(favorited, isFavorited ? R.string.context_unfavorite
                : R.string.context_favorite);
        favorited.setTag(isFavorited);

        // poster background
        Utils.loadPosterBackground(getActivity(), imageBackground,
                show.getString(ShowQuery.SHOW_POSTER));

        // next release day and time
        StringBuilder timeAndNetwork = new StringBuilder();
        int releaseTime = show.getInt(ShowQuery.SHOW_RELEASE_TIME);
        String network = show.getString(ShowQuery.SHOW_NETWORK);
        if (releaseTime != -1) {
            int weekDay = show.getInt(ShowQuery.SHOW_RELEASE_WEEKDAY);
            Date release = TimeTools.getShowReleaseDateTime(getActivity(),
                    TimeTools.getShowReleaseTime(releaseTime),
                    weekDay,
                    show.getString(ShowQuery.SHOW_RELEASE_TIMEZONE),
                    show.getString(ShowQuery.SHOW_RELEASE_COUNTRY),
                    network);
            String dayString = TimeTools.formatToLocalDayOrDaily(getActivity(), release, weekDay);
            String timeString = TimeTools.formatToLocalTime(getActivity(), release);
            // "Mon 08:30"
            timeAndNetwork.append(dayString).append(" ").append(timeString);
        }
        // network
        if (!TextUtils.isEmpty(network)) {
            if (timeAndNetwork.length() != 0) {
                timeAndNetwork.append(" ");
            }
            timeAndNetwork.append(getString(R.string.show_on_network, network));
        }
        ((TextView) getView().findViewById(R.id.showmeta)).setText(timeAndNetwork.toString());

        // episode description might need show language, so update it here as well
        populateEpisodeDescription();
    }

    private LoaderManager.LoaderCallbacks<List<Action>> episodeActionsLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<Action>>() {
                @Override
                public Loader<List<Action>> onCreateLoader(int id, Bundle args) {
                    int episodeTvdbId = args.getInt(ARG_EPISODE_TVDB_ID);
                    return new EpisodeActionsLoader(getActivity(), episodeTvdbId);
                }

                @Override
                public void onLoadFinished(Loader<List<Action>> loader, List<Action> data) {
                    if (!isAdded()) {
                        return;
                    }
                    if (data == null) {
                        Timber.e("onLoadFinished: did not receive valid actions");
                    } else {
                        Timber.d("onLoadFinished: received %s actions", data.size());
                    }
                    EpisodeActionsHelper.populateEpisodeActions(getActivity().getLayoutInflater(),
                            containerActions, data, TAG);
                }

                @Override
                public void onLoaderReset(Loader<List<Action>> loader) {
                    EpisodeActionsHelper.populateEpisodeActions(getActivity().getLayoutInflater(),
                            containerActions, null, TAG);
                }
            };
}
