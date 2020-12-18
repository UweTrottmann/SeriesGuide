package com.battlelancer.seriesguide.ui.overview;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.extensions.ActionsHelper;
import com.battlelancer.seriesguide.extensions.EpisodeActionsContract;
import com.battlelancer.seriesguide.extensions.EpisodeActionsLoader;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.streaming.StreamingSearch;
import com.battlelancer.seriesguide.streaming.StreamingSearchConfigureDialog;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.thetvdbapi.TvdbLinks;
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment;
import com.battlelancer.seriesguide.traktapi.RateDialogFragment;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktRatingsFetcher;
import com.battlelancer.seriesguide.traktapi.TraktTools;
import com.battlelancer.seriesguide.ui.BaseMessageActivity;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.comments.TraktCommentsActivity;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity;
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.preferences.MoreOptionsActivity;
import com.battlelancer.seriesguide.ui.search.SimilarShowsActivity;
import com.battlelancer.seriesguide.ui.shows.RemoveShowDialogFragment;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.ClipboardTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.LanguageTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.battlelancer.seriesguide.widgets.FeedbackView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.uwetrottmann.androidutils.CheatSheet;
import java.util.Date;
import java.util.List;
import kotlinx.coroutines.Job;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Displays general information about a show and its next episode.
 */
public class OverviewFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, EpisodeActionsContract {

    public static final String ARG_INT_SHOW_TVDBID = "show_tvdbid";
    private static final String ARG_EPISODE_TVDB_ID = "episodeTvdbId";

    @BindView(R.id.background) ImageView imageBackground;

    @Nullable
    @BindView(R.id.viewStubOverviewFeedback)
    ViewStub feedbackViewStub;
    @Nullable
    @BindView(R.id.feedbackViewOverview)
    FeedbackView feedbackView;

    @BindView(R.id.containerOverviewShow) View containerShow;
    @BindView(R.id.imageButtonFavorite) ImageButton buttonFavorite;

    @BindView(R.id.progress_container) View containerProgress;
    @BindView(R.id.containerOverviewEpisode) View containerEpisode;

    @BindView(R.id.episode_empty_container) View containerEpisodeEmpty;
    @BindView(R.id.buttonOverviewSimilarShows) Button buttonSimilarShows;
    @BindView(R.id.buttonOverviewRemoveShow) Button buttonRemoveShow;

    @BindView(R.id.episode_primary_container) View containerEpisodePrimary;
    @BindView(R.id.dividerHorizontalOverviewEpisodeMeta) View dividerEpisodeMeta;
    @BindView(R.id.imageViewOverviewEpisode) ImageView imageEpisode;
    @BindView(R.id.episodeTitle) TextView textEpisodeTitle;
    @BindView(R.id.episodeTime) TextView textEpisodeTime;
    @BindView(R.id.episodeInfo) TextView textEpisodeNumbers;

    @BindView(R.id.episode_meta_container) View containerEpisodeMeta;
    @BindView(R.id.containerRatings) View containerRatings;
    @BindView(R.id.dividerEpisodeButtons) View dividerEpisodeButtons;
    @BindView(R.id.buttonEpisodeCheckin) Button buttonCheckin;
    @BindView(R.id.buttonEpisodeWatchedUpTo) Button buttonWatchedUpTo;
    @BindView(R.id.buttonEpisodeStreamingSearch) Button buttonStreamingSearch;
    @BindView(R.id.buttonEpisodeWatched) Button buttonWatch;
    @BindView(R.id.buttonEpisodeCollected) Button buttonCollect;
    @BindView(R.id.buttonEpisodeSkip) Button buttonSkip;

    @BindView(R.id.TextViewEpisodeDescription) TextView textDescription;
    @BindView(R.id.labelDvd) View labelDvdNumber;
    @BindView(R.id.textViewEpisodeDVDnumber) TextView textDvdNumber;
    @BindView(R.id.labelGuestStars) View labelGuestStars;
    @BindView(R.id.TextViewEpisodeGuestStars) TextView textGuestStars;
    @BindView(R.id.textViewRatingsValue) TextView textRating;
    @BindView(R.id.textViewRatingsRange) TextView textRatingRange;
    @BindView(R.id.textViewRatingsVotes) TextView textRatingVotes;
    @BindView(R.id.textViewRatingsUser) TextView textUserRating;

    @BindView(R.id.buttonEpisodeImdb) Button buttonImdb;
    @BindView(R.id.buttonEpisodeTvdb) Button buttonTvdb;
    @BindView(R.id.buttonEpisodeTrakt) Button buttonTrakt;
    @BindView(R.id.buttonEpisodeShare) Button buttonShare;
    @BindView(R.id.buttonEpisodeCalendar) Button buttonAddToCalendar;
    @BindView(R.id.buttonEpisodeLists) Button buttonManageLists;
    @BindView(R.id.buttonEpisodeComments) Button buttonComments;

    @BindView(R.id.containerEpisodeActions) LinearLayout containerActions;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Job ratingFetchJob;
    private Unbinder unbinder;

    private boolean isEpisodeDataAvailable;
    private Cursor currentEpisodeCursor;
    private int currentEpisodeTvdbId;

    private boolean isShowDataAvailable;
    private Cursor showCursor;
    private int showTvdbId;
    private String showTitle;

    private boolean hasSetEpisodeWatched;

    public static OverviewFragment newInstance(int showTvdbId) {
        OverviewFragment f = new OverviewFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(ARG_INT_SHOW_TVDBID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showTvdbId = requireArguments().getInt(ARG_INT_SHOW_TVDBID);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        unbinder = ButterKnife.bind(this, view);

        containerEpisode.setVisibility(View.GONE);
        containerEpisodeEmpty.setVisibility(View.GONE);

        containerEpisodePrimary.setOnClickListener(viewContainer -> {
            if (isEpisodeDataAvailable) {
                // display episode details
                Intent intent = new Intent(getActivity(), EpisodesActivity.class);
                intent.putExtra(EpisodesActivity.EXTRA_EPISODE_TVDBID, currentEpisodeTvdbId);
                Utils.startActivityWithAnimation(getActivity(), intent, viewContainer);
            }
        });

        // Empty view buttons.
        buttonSimilarShows.setOnClickListener(v ->
                startActivity(SimilarShowsActivity.intent(requireContext(), showTvdbId, showTitle))
        );
        buttonRemoveShow.setOnClickListener(v ->
                RemoveShowDialogFragment
                        .show(requireContext(), getParentFragmentManager(), showTvdbId)
        );

        // episode buttons
        buttonWatchedUpTo.setVisibility(View.GONE); // Unused.
        CheatSheet.setup(buttonCheckin);
        CheatSheet.setup(buttonWatch);
        CheatSheet.setup(buttonSkip);

        // ratings
        CheatSheet.setup(containerRatings, R.string.action_rate);
        textRatingRange.setText(getString(R.string.format_rating_range, 10));

        buttonShare.setOnClickListener(v -> shareEpisode());
        buttonAddToCalendar.setOnClickListener(v -> createCalendarEvent());
        buttonManageLists.setOnClickListener(v -> {
                    if (isEpisodeDataAvailable) {
                        ManageListsDialogFragment.show(
                                getParentFragmentManager(),
                                currentEpisodeCursor.getInt(EpisodeQuery._ID),
                                ListItemTypes.EPISODE
                        );
                    }
                }
        );

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        ClipboardTools.copyTextToClipboardOnLongClick(textDescription);
        ClipboardTools.copyTextToClipboardOnLongClick(textGuestStars);
        ClipboardTools.copyTextToClipboardOnLongClick(textDvdNumber);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Hide show info if show fragment is visible due to multi-pane layout.
        boolean isDisplayShowInfo = getResources().getBoolean(R.bool.isOverviewSinglePane);
        containerShow.setVisibility(isDisplayShowInfo ? View.VISIBLE : View.GONE);

        LoaderManager loaderManager = LoaderManager.getInstance(this);
        loaderManager.initLoader(OverviewActivity.OVERVIEW_SHOW_LOADER_ID, null, this);
        loaderManager.initLoader(OverviewActivity.OVERVIEW_EPISODE_LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        BaseMessageActivity.ServiceActiveEvent event = EventBus.getDefault()
                .getStickyEvent(BaseMessageActivity.ServiceActiveEvent.class);
        setEpisodeButtonsEnabled(event == null);

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
        Picasso.get().cancelRequest(imageEpisode);

        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(episodeActionsRunnable);
        }
        // Release reference to any job.
        ratingFetchJob = null;
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
    }

    @OnClick(R.id.imageButtonFavorite)
    void onButtonFavoriteClick(View view) {
        if (view.getTag() == null) {
            return;
        }

        // store new value
        boolean isFavorite = (Boolean) view.getTag();
        SgApp.getServicesComponent(requireContext()).showTools()
                .storeIsFavorite(showTvdbId, !isFavorite);
    }

    @OnClick(R.id.buttonEpisodeCheckin)
    void onButtonCheckInClick() {
        if (!isEpisodeDataAvailable) {
            return;
        }
        int episodeTvdbId = currentEpisodeCursor.getInt(EpisodeQuery._ID);
        // check in
        CheckInDialogFragment.show(requireContext(), getParentFragmentManager(), episodeTvdbId);
    }

    @OnClick(R.id.buttonEpisodeStreamingSearch)
    void onButtonStreamingSearchClick() {
        if (StreamingSearch.isNotConfigured(requireContext())) {
            showStreamingSearchConfigDialog();
        } else {
            StreamingSearch.searchForShow(requireContext(), showTitle);
        }
    }

    @OnLongClick(R.id.buttonEpisodeStreamingSearch)
    boolean onButtonStreamingSearchLongClick() {
        showStreamingSearchConfigDialog();
        return true;
    }

    private void showStreamingSearchConfigDialog() {
        StreamingSearchConfigureDialog.show(getParentFragmentManager());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStreamingSearchConfigured(
            StreamingSearchConfigureDialog.StreamingSearchConfiguredEvent event) {
        if (event.getTurnedOff()) {
            buttonStreamingSearch.setVisibility(View.GONE);
        } else {
            onButtonStreamingSearchClick();
        }
    }

    @OnClick(R.id.buttonEpisodeWatched)
    void onButtonWatchedClick() {
        hasSetEpisodeWatched = true;
        changeEpisodeFlag(EpisodeFlags.WATCHED);
    }

    @OnClick(R.id.buttonEpisodeCollected)
    void onButtonCollectedClick() {
        if (!isEpisodeDataAvailable) {
            return;
        }
        final int season = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        final int episode = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        final boolean isCollected = currentEpisodeCursor.getInt(EpisodeQuery.COLLECTED) == 1;
        EpisodeTools.episodeCollected(getContext(), showTvdbId,
                currentEpisodeCursor.getInt(EpisodeQuery._ID), season, episode, !isCollected);
    }

    @OnClick(R.id.buttonEpisodeSkip)
    void onButtonSkipClicked() {
        changeEpisodeFlag(EpisodeFlags.SKIPPED);
    }

    private void changeEpisodeFlag(int episodeFlag) {
        if (!isEpisodeDataAvailable) {
            return;
        }
        final int season = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        final int episode = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        EpisodeTools.episodeWatched(getContext(), showTvdbId,
                currentEpisodeCursor.getInt(EpisodeQuery._ID), season, episode, episodeFlag);
    }

    @OnClick(R.id.containerRatings)
    void onButtonRateClick() {
        if (currentEpisodeTvdbId == 0) {
            return;
        }
        RateDialogFragment.newInstanceEpisode(currentEpisodeTvdbId)
                .safeShow(getContext(), getParentFragmentManager());
    }

    @OnClick(R.id.buttonEpisodeComments)
    void onButtonCommentsClick(View v) {
        if (isEpisodeDataAvailable) {
            Intent i = new Intent(getActivity(), TraktCommentsActivity.class);
            i.putExtras(TraktCommentsActivity.createInitBundleEpisode(
                    currentEpisodeCursor.getString(EpisodeQuery.TITLE),
                    currentEpisodeTvdbId
            ));
            Utils.startActivityWithAnimation(getActivity(), i, v);
        }
    }

    private void shareEpisode() {
        if (!isShowDataAvailable || !isEpisodeDataAvailable) {
            return;
        }
        int seasonTvdbId = currentEpisodeCursor.getInt(EpisodeQuery.SEASON_ID);
        int seasonNumber = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        int episodeNumber = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        String episodeTitle = currentEpisodeCursor.getString(EpisodeQuery.TITLE);
        String showTvdbSlug = showCursor.getString(ShowQuery.SHOW_SLUG);

        ShareUtils.shareEpisode(getActivity(), showTvdbSlug, showTvdbId, seasonTvdbId,
                currentEpisodeTvdbId, seasonNumber, episodeNumber, showTitle, episodeTitle);
    }

    public static class EpisodeLoader extends CursorLoader {

        private final long showId;

        public EpisodeLoader(Context context, long showId) {
            super(context);
            this.showId = showId;
            setProjection(EpisodeQuery.PROJECTION);
        }

        @Override
        public Cursor loadInBackground() {
            // get episode id, set query params
            long episodeId = DBUtils.updateLatestEpisode(getContext(), showId);
            setUri(Episodes.buildIdUri(episodeId));

            return super.loadInBackground();
        }
    }

    interface EpisodeQuery {

        String[] PROJECTION = new String[]{
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
                Episodes.IMAGE,
                Episodes.LAST_EDITED,
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
        int LAST_EDITED = 17;
    }

    interface ShowQuery {

        String[] PROJECTION = new String[]{
                Shows._ID,
                Shows.TITLE,
                Shows.STATUS,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.NETWORK,
                Shows.POSTER_SMALL,
                Shows.IMDBID,
                Shows.RUNTIME,
                Shows.FAVORITE,
                Shows.LANGUAGE,
                Shows.SLUG
        };

        int SHOW_TITLE = 1;
        int SHOW_STATUS = 2;
        int SHOW_RELEASE_TIME = 3;
        int SHOW_RELEASE_WEEKDAY = 4;
        int SHOW_RELEASE_TIMEZONE = 5;
        int SHOW_RELEASE_COUNTRY = 6;
        int SHOW_NETWORK = 7;
        int SHOW_POSTER_SMALL = 8;
        int SHOW_IMDBID = 9;
        int SHOW_RUNTIME = 10;
        int SHOW_FAVORITE = 11;
        int SHOW_LANGUAGE = 12;
        int SHOW_SLUG = 13;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case OverviewActivity.OVERVIEW_EPISODE_LOADER_ID:
            default:
                // FIXME
//                return new EpisodeLoader(getActivity(), showId);
            case OverviewActivity.OVERVIEW_SHOW_LOADER_ID:
                return new CursorLoader(requireContext(), Shows.buildShowUri(String
                        .valueOf(showTvdbId)), ShowQuery.PROJECTION, null, null, null);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            return;
        }
        switch (loader.getId()) {
            case OverviewActivity.OVERVIEW_EPISODE_LOADER_ID:
                isEpisodeDataAvailable = data != null && data.moveToFirst();
                currentEpisodeCursor = data;
                maybeAddFeedbackView();
                updateEpisodeViews(data);
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event) {
        if (currentEpisodeTvdbId == event.episodeTvdbId) {
            loadEpisodeActionsDelayed();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEpisodeTask(BaseMessageActivity.ServiceActiveEvent event) {
        setEpisodeButtonsEnabled(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEpisodeTask(BaseMessageActivity.ServiceCompletedEvent event) {
        setEpisodeButtonsEnabled(true);
    }

    private void setEpisodeButtonsEnabled(boolean enabled) {
        if (getView() == null) {
            return;
        }

        buttonWatch.setEnabled(enabled);
        buttonCollect.setEnabled(enabled);
        buttonSkip.setEnabled(enabled);
        buttonCheckin.setEnabled(enabled);
        buttonStreamingSearch.setEnabled(enabled);
    }

    private void updateEpisodeViews(Cursor episode) {
        if (isEpisodeDataAvailable) {
            // some episode properties
            currentEpisodeTvdbId = episode.getInt(EpisodeQuery._ID);

            // hide check-in if not connected to trakt or hexagon is enabled
            boolean isConnectedToTrakt = TraktCredentials.get(getActivity()).hasCredentials();
            boolean displayCheckIn = isConnectedToTrakt
                    && !HexagonSettings.isEnabled(getActivity());
            buttonCheckin.setVisibility(displayCheckIn ? View.VISIBLE : View.GONE);
            buttonStreamingSearch.setNextFocusUpId(
                    displayCheckIn ? R.id.buttonCheckIn : R.id.buttonEpisodeWatched);
            // hide streaming search if turned off
            boolean displayStreamingSearch = !StreamingSearch.isTurnedOff(requireContext());
            buttonStreamingSearch.setVisibility(displayStreamingSearch ? View.VISIBLE : View.GONE);
            dividerEpisodeButtons.setVisibility(displayCheckIn || displayStreamingSearch
                    ? View.VISIBLE : View.GONE);

            // populate episode details
            populateEpisodeViews(episode);
            populateEpisodeDescriptionAndTvdbButton();

            // load full info and ratings, image, actions
            loadEpisodeDetails();
            loadEpisodeImage(episode.getString(EpisodeQuery.IMAGE));
            loadEpisodeActionsDelayed();

            containerEpisodeEmpty.setVisibility(View.GONE);
            containerEpisodePrimary.setVisibility(View.VISIBLE);
            containerEpisodeMeta.setVisibility(View.VISIBLE);
        } else {
            // No next episode: display empty view with suggestion on what to do.
            currentEpisodeTvdbId = 0;

            containerEpisodeEmpty.setVisibility(View.VISIBLE);
            containerEpisodePrimary.setVisibility(View.GONE);
            containerEpisodeMeta.setVisibility(View.GONE);
        }

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

    private void populateEpisodeViews(Cursor episode) {
        // title
        int season = episode.getInt(EpisodeQuery.SEASON);
        int number = episode.getInt(EpisodeQuery.NUMBER);
        final String title = TextTools.getEpisodeTitle(getContext(),
                DisplaySettings.preventSpoilers(getContext())
                        ? null : episode.getString(EpisodeQuery.TITLE), number);
        textEpisodeTitle.setText(title);

        // number
        StringBuilder infoText = new StringBuilder();
        infoText.append(getString(R.string.season_number, season));
        infoText.append(" ");
        infoText.append(getString(R.string.episode_number, number));
        int episodeAbsoluteNumber = episode.getInt(EpisodeQuery.ABSOLUTE_NUMBER);
        if (episodeAbsoluteNumber > 0 && episodeAbsoluteNumber != number) {
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
            textEpisodeTime.setText(getString(R.string.format_date_and_day, dateTime,
                    TimeTools.formatToLocalDay(actualRelease)));
        } else {
            textEpisodeTime.setText(null);
        }

        // collected button
        boolean isCollected = episode.getInt(EpisodeQuery.COLLECTED) == 1;
        if (isCollected) {
            ViewTools.setVectorDrawableTop(buttonCollect, R.drawable.ic_collected_24dp);
        } else {
            ViewTools.setVectorDrawableTop(buttonCollect, R.drawable.ic_collect_black_24dp);
        }
        buttonCollect.setText(isCollected ? R.string.state_in_collection
                : R.string.action_collection_add);
        CheatSheet.setup(buttonCollect, isCollected ? R.string.action_collection_remove
                : R.string.action_collection_add);

        // dvd number
        boolean isShowingMeta = ViewTools.setLabelValueOrHide(labelDvdNumber, textDvdNumber,
                episode.getDouble(EpisodeQuery.DVD_NUMBER));
        // guest stars
        isShowingMeta |= ViewTools.setLabelValueOrHide(labelGuestStars, textGuestStars,
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
        ServiceUtils.setUpImdbButton(imdbId, buttonImdb);

        // trakt button
        String traktLink = TraktTools.buildEpisodeUrl(currentEpisodeTvdbId);
        ViewTools.openUriOnClick(buttonTrakt, traktLink);
        ClipboardTools.copyTextToClipboardOnLongClick(buttonTrakt, traktLink);
    }

    /**
     * Updates the episode description and TVDB button. Need both show and episode data loaded.
     */
    private void populateEpisodeDescriptionAndTvdbButton() {
        if (!isShowDataAvailable || !isEpisodeDataAvailable) {
            // no show or episode data available
            return;
        }
        String overview = currentEpisodeCursor.getString(EpisodeQuery.OVERVIEW);
        String languageCode = showCursor.getString(ShowQuery.SHOW_LANGUAGE);
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            overview = getString(R.string.no_translation,
                    LanguageTools.getShowLanguageStringFor(getContext(), languageCode),
                    getString(R.string.tvdb));
        } else if (DisplaySettings.preventSpoilers(getContext())) {
            overview = getString(R.string.no_spoilers);
        }
        long lastEditSeconds = currentEpisodeCursor.getLong(EpisodeQuery.LAST_EDITED);
        textDescription.setText(TextTools.textWithTvdbSource(textDescription.getContext(),
                overview, lastEditSeconds));

        // TVDb button
        final int episodeTvdbId = currentEpisodeCursor.getInt(EpisodeQuery._ID);
        final int seasonTvdbId = currentEpisodeCursor.getInt(EpisodeQuery.SEASON_ID);
        String showTvdbSlug = showCursor.getString(ShowQuery.SHOW_SLUG);
        String tvdbLink = TvdbLinks.episode(showTvdbSlug, showTvdbId, seasonTvdbId, episodeTvdbId);
        ViewTools.openUriOnClick(buttonTvdb, tvdbLink);
        ClipboardTools.copyTextToClipboardOnLongClick(buttonTvdb, tvdbLink);
    }

    @Override
    public void loadEpisodeActions() {
        if (currentEpisodeTvdbId == 0) {
            // do not load actions if there is no episode
            return;
        }
        Bundle args = new Bundle();
        args.putInt(ARG_EPISODE_TVDB_ID, currentEpisodeTvdbId);
        LoaderManager.getInstance(this)
                .restartLoader(OverviewActivity.OVERVIEW_ACTIONS_LOADER_ID, args,
                        episodeActionsLoaderCallbacks);
    }

    Runnable episodeActionsRunnable = this::loadEpisodeActions;

    @Override
    public void loadEpisodeActionsDelayed() {
        handler.removeCallbacks(episodeActionsRunnable);
        handler.postDelayed(episodeActionsRunnable,
                EpisodeActionsContract.ACTION_LOADER_DELAY_MILLIS);
    }

    private void loadEpisodeImage(String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            imageEpisode.setImageDrawable(null);
            return;
        }

        if (DisplaySettings.preventSpoilers(getContext())) {
            // show image placeholder
            imageEpisode.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageEpisode.setImageResource(R.drawable.ic_photo_gray_24dp);
        } else {
            // try loading image
            ServiceUtils.loadWithPicasso(requireContext(), TvdbImageTools.artworkUrl(imagePath))
                    .error(R.drawable.ic_photo_gray_24dp)
                    .into(imageEpisode,
                            new Callback() {
                                @Override
                                public void onSuccess() {
                                    imageEpisode.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                }

                                @Override
                                public void onError(Exception e) {
                                    imageEpisode.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                }
                            }
                    );
        }
    }

    private void loadEpisodeDetails() {
        if (!isEpisodeDataAvailable) {
            return;
        }
        if (ratingFetchJob == null || !ratingFetchJob.isActive()) {
            int seasonNumber = currentEpisodeCursor.getInt(EpisodeQuery.SEASON);
            int episodeNumber = currentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            ratingFetchJob = TraktRatingsFetcher.fetchEpisodeRatingsAsync(
                    requireContext(),
                    showTvdbId,
                    currentEpisodeTvdbId,
                    seasonNumber,
                    episodeNumber
            );
        }
    }

    private void populateShowViews(@NonNull Cursor show) {
        // set show title in action bar
        showTitle = show.getString(ShowQuery.SHOW_TITLE);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(showTitle);
            requireActivity().setTitle(getString(R.string.description_overview) + showTitle);
        }

        if (getView() == null) {
            return;
        }

        // status
        final TextView statusText = getView().findViewById(R.id.showStatus);
        ShowTools.setStatusAndColor(statusText, show.getInt(ShowQuery.SHOW_STATUS));

        // favorite
        boolean isFavorite = show.getInt(ShowQuery.SHOW_FAVORITE) == 1;
        buttonFavorite.setImageResource(isFavorite
                ? R.drawable.ic_star_black_24dp
                : R.drawable.ic_star_border_black_24dp);
        buttonFavorite.setContentDescription(getString(isFavorite ? R.string.context_unfavorite
                : R.string.context_favorite));
        CheatSheet.setup(buttonFavorite);
        buttonFavorite.setTag(isFavorite);

        // poster background
        TvdbImageTools.loadShowPosterAlpha(requireContext(), imageBackground,
                show.getString(ShowQuery.SHOW_POSTER_SMALL));

        // Regular network, release time and length.
        String network = show.getString(ShowQuery.SHOW_NETWORK);
        String time = null;
        int releaseTime = show.getInt(ShowQuery.SHOW_RELEASE_TIME);
        if (releaseTime != -1) {
            int weekDay = show.getInt(ShowQuery.SHOW_RELEASE_WEEKDAY);
            Date release = TimeTools.getShowReleaseDateTime(requireContext(),
                    releaseTime,
                    weekDay,
                    show.getString(ShowQuery.SHOW_RELEASE_TIMEZONE),
                    show.getString(ShowQuery.SHOW_RELEASE_COUNTRY),
                    network);
            String dayString = TimeTools.formatToLocalDayOrDaily(requireContext(), release, weekDay);
            String timeString = TimeTools.formatToLocalTime(requireContext(), release);
            // "Mon 08:30"
            time = dayString + " " + timeString;
        }
        String runtime = getString(
                R.string.runtime_minutes,
                String.valueOf(showCursor.getInt(ShowQuery.SHOW_RUNTIME))
        );
        String combinedString = TextTools.dotSeparate(TextTools.dotSeparate(network, time), runtime);
        TextView textViewNetworkAndTime = getView().findViewById(R.id.showmeta);
        textViewNetworkAndTime.setText(combinedString);
        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        ClipboardTools.copyTextToClipboardOnLongClick(textViewNetworkAndTime);

        // episode description might need show language, so update it here as well
        populateEpisodeDescriptionAndTvdbButton();
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
                    if (Utils.tryStartActivity(requireContext(),
                            MoreOptionsActivity.getFeedbackEmailIntent(requireContext()), true)) {
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

    private LoaderManager.LoaderCallbacks<List<Action>> episodeActionsLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<Action>>() {
                @Override
                public Loader<List<Action>> onCreateLoader(int id, Bundle args) {
                    int episodeTvdbId = args.getInt(ARG_EPISODE_TVDB_ID);
                    return new EpisodeActionsLoader(getActivity(), episodeTvdbId);
                }

                @Override
                public void onLoadFinished(@NonNull Loader<List<Action>> loader, List<Action> data) {
                    if (!isAdded()) {
                        return;
                    }
                    if (data == null) {
                        Timber.e("onLoadFinished: did not receive valid actions");
                    } else {
                        Timber.d("onLoadFinished: received %s actions", data.size());
                    }
                    ActionsHelper.populateActions(requireActivity().getLayoutInflater(),
                            requireActivity().getTheme(), containerActions, data);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<List<Action>> loader) {
                    ActionsHelper.populateActions(requireActivity().getLayoutInflater(),
                            requireActivity().getTheme(), containerActions, null);
                }
            };
}
