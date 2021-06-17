package com.battlelancer.seriesguide.ui.overview;

import android.content.Intent;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.extensions.ActionsHelper;
import com.battlelancer.seriesguide.extensions.EpisodeActionsContract;
import com.battlelancer.seriesguide.extensions.EpisodeActionsLoader;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.model.SgEpisode2;
import com.battlelancer.seriesguide.model.SgShow2;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.streaming.StreamingSearch;
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
import com.battlelancer.seriesguide.ui.preferences.MoreOptionsActivity;
import com.battlelancer.seriesguide.ui.search.SimilarShowsActivity;
import com.battlelancer.seriesguide.ui.shows.RemoveShowDialogFragment;
import com.battlelancer.seriesguide.util.ClipboardTools;
import com.battlelancer.seriesguide.util.ImageTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TextToolsK;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TmdbTools;
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
public class OverviewFragment extends Fragment implements EpisodeActionsContract {

    private static final String ARG_LONG_SHOW_ROWID = "show_id";
    private static final String ARG_EPISODE_ID = "episode_id";

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

    @BindView(R.id.textViewOverviewNotMigrated) View textViewOverviewNotMigrated;
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
    @BindView(R.id.containerEpisodeStreamingSearch) ViewGroup containerEpisodeStreamingSearch;
    @BindView(R.id.buttonEpisodeStreamingSearch) Button buttonStreamingSearch;
    @BindView(R.id.buttonEpisodeStreamingSearchInfo) ImageButton buttonEpisodeStreamingSearchInfo;
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
    @BindView(R.id.buttonEpisodeTmdb) Button buttonTmdb;
    @BindView(R.id.buttonEpisodeTrakt) Button buttonTrakt;
    @BindView(R.id.buttonEpisodeShare) Button buttonShare;
    @BindView(R.id.buttonEpisodeCalendar) Button buttonAddToCalendar;
    @BindView(R.id.buttonEpisodeComments) Button buttonComments;

    @BindView(R.id.containerEpisodeActions) LinearLayout containerActions;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Job ratingFetchJob;
    private Unbinder unbinder;
    private OverviewViewModel model;

    private long showId;
    private int showTvdbId;
    @Nullable private SgShow2 show;
    @Nullable private SgEpisode2 episode;

    private boolean hasSetEpisodeWatched;

    // No arg constructor for Android.
    public OverviewFragment() {
    }

    public OverviewFragment(long showRowId) {
        setArguments(buildArgs(showRowId));
    }

    public static Bundle buildArgs(long showRowId) {
        Bundle args = new Bundle();
        args.putLong(ARG_LONG_SHOW_ROWID, showRowId);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showId = requireArguments().getLong(ARG_LONG_SHOW_ROWID);
        showTvdbId = SgRoomDatabase.getInstance(requireContext()).sgShow2Helper()
                .getShowTvdbId(showId);
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

        containerEpisodePrimary.setOnClickListener(
                v -> runIfHasEpisode((episode) -> {
                    // display episode details
                    Intent intent = EpisodesActivity.intentEpisode(episode.getId(), requireContext());
                    Utils.startActivityWithAnimation(getActivity(), intent, v);
                }));

        // Empty view buttons.
        buttonSimilarShows.setOnClickListener(v -> {
            if (show != null && show.getTmdbId() != null) {
                startActivity(
                        SimilarShowsActivity
                                .intent(requireContext(), show.getTmdbId(), show.getTitle()));
            }
        });
        buttonRemoveShow.setOnClickListener(v ->
                RemoveShowDialogFragment.show(showId, getParentFragmentManager(), requireContext())
        );

        // episode buttons
        buttonWatchedUpTo.setVisibility(View.GONE); // Unused.
        CheatSheet.setup(buttonCheckin);
        CheatSheet.setup(buttonWatch);
        CheatSheet.setup(buttonSkip);
        StreamingSearch.initButtons(buttonStreamingSearch, buttonEpisodeStreamingSearchInfo,
                getParentFragmentManager());

        // ratings
        CheatSheet.setup(containerRatings, R.string.action_rate);
        textRatingRange.setText(getString(R.string.format_rating_range, 10));

        buttonShare.setOnClickListener(v -> shareEpisode());
        buttonAddToCalendar.setOnClickListener(v -> createCalendarEvent());

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

        model = new ViewModelProvider(this,
                new OverviewViewModelFactory(showId, requireActivity().getApplication()))
                .get(OverviewViewModel.class);
        model.getShow().observe(getViewLifecycleOwner(), sgShow2 -> {
            if (sgShow2 == null) {
                Timber.e("Failed to load show %s", showId);
                requireActivity().finish();
                return;
            }
            show = sgShow2;
            populateShowViews(sgShow2);
            long episodeId = sgShow2.getNextEpisode() != null && !sgShow2.getNextEpisode().isEmpty()
                    ? Long.parseLong(sgShow2.getNextEpisode()) : -1;
            model.setEpisodeId(episodeId);
            model.setShowTmdbId(sgShow2.getTmdbId());
        });
        model.getEpisode().observe(getViewLifecycleOwner(), sgEpisode2 -> {
            // May be null if there is no next episode.
            episode = sgEpisode2;
            maybeAddFeedbackView();
            updateEpisodeViews(sgEpisode2);
        });
        model.getWatchProvider().observe(getViewLifecycleOwner(),
                watchInfo -> StreamingSearch
                        .configureButton(buttonStreamingSearch, watchInfo, true));
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
        SgShow2 currentShow = this.show;
        SgEpisode2 currentEpisode = this.episode;
        if (currentShow == null || currentEpisode == null) {
            return;
        }
        // add calendar event
        ShareUtils.suggestCalendarEvent(
                getActivity(),
                currentShow.getTitle(),
                TextTools.getNextEpisodeString(getActivity(), currentEpisode.getSeason(),
                        currentEpisode.getNumber(), currentEpisode.getTitle()),
                currentEpisode.getFirstReleasedMs(),
                currentShow.getRuntime()
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
                .storeIsFavorite(showId, !isFavorite);
    }

    @OnClick(R.id.buttonEpisodeCheckin)
    void onButtonCheckInClick() {
        runIfHasEpisode((e) -> CheckInDialogFragment
                .show(requireContext(), getParentFragmentManager(), e.getId()));
    }

    @OnClick(R.id.buttonEpisodeWatched)
    void onButtonWatchedClick() {
        hasSetEpisodeWatched = true;
        changeEpisodeFlag(EpisodeFlags.WATCHED);
    }

    @OnClick(R.id.buttonEpisodeCollected)
    void onButtonCollectedClick() {
        runIfHasEpisode((episode) -> {
            final boolean isCollected = episode.getCollected();
            EpisodeTools.episodeCollected(getContext(), episode.getId(), !isCollected);
        });
    }

    @OnClick(R.id.buttonEpisodeSkip)
    void onButtonSkipClicked() {
        changeEpisodeFlag(EpisodeFlags.SKIPPED);
    }

    private void changeEpisodeFlag(int episodeFlag) {
        runIfHasEpisode((episode) -> EpisodeTools
                .episodeWatched(getContext(), episode.getId(), episodeFlag));
    }

    @OnClick(R.id.containerRatings)
    void onButtonRateClick() {
        runIfHasEpisode(episode -> RateDialogFragment.newInstanceEpisode(episode.getId())
                .safeShow(getContext(), getParentFragmentManager()));
    }

    @OnClick(R.id.buttonEpisodeComments)
    void onButtonCommentsClick(View v) {
        runIfHasEpisode(episode -> {
            Intent i = TraktCommentsActivity
                    .intentEpisode(requireContext(), episode.getTitle(), episode.getId());
            Utils.startActivityWithAnimation(getActivity(), i, v);
        });
    }

    private void shareEpisode() {
        SgShow2 currentShow = this.show;
        if (currentShow == null) {
            return;
        }
        runIfHasEpisode(episode -> {
            if (currentShow.getTmdbId() != null) {
                ShareUtils.shareEpisode(getActivity(), currentShow.getTmdbId(), episode.getSeason(),
                        episode.getNumber(), currentShow.getTitle(), episode.getTitle());
            }
        });
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event) {
        runIfHasEpisode(episode -> {
            if (episode.getTmdbId() == event.episodeTmdbId) {
                loadEpisodeActionsDelayed();
            }
        });
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
    }

    private void updateEpisodeViews(@Nullable SgEpisode2 episode) {
        if (episode != null) {
            // hide check-in if not connected to trakt or hexagon is enabled
            boolean isConnectedToTrakt = TraktCredentials.get(getActivity()).hasCredentials();
            boolean displayCheckIn = isConnectedToTrakt
                    && !HexagonSettings.isEnabled(getActivity());
            buttonCheckin.setVisibility(displayCheckIn ? View.VISIBLE : View.GONE);
            buttonStreamingSearch.setNextFocusUpId(
                    displayCheckIn ? R.id.buttonCheckIn : R.id.buttonEpisodeWatched);

            // populate episode details
            populateEpisodeViews(episode);
            populateEpisodeDescriptionAndTvdbButton();

            // load full info and ratings, image, actions
            loadEpisodeDetails();
            loadEpisodeImage(episode.getImage());
            loadEpisodeActionsDelayed();

            containerEpisodeEmpty.setVisibility(View.GONE);
            containerEpisodePrimary.setVisibility(View.VISIBLE);
            containerEpisodeMeta.setVisibility(View.VISIBLE);
        } else {
            // No next episode: display empty view with suggestion on what to do.
            textViewOverviewNotMigrated.setVisibility(View.GONE);
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

    private void populateEpisodeViews(@NonNull SgEpisode2 episode) {
        ViewTools.configureNotMigratedWarning(textViewOverviewNotMigrated,
                episode.getTmdbId() == null);

        // title
        int season = episode.getSeason();
        int number = episode.getNumber();
        final String title = TextTools.getEpisodeTitle(getContext(),
                DisplaySettings.preventSpoilers(getContext()) ? null : episode.getTitle(), number);
        textEpisodeTitle.setText(title);

        // number
        StringBuilder infoText = new StringBuilder();
        infoText.append(getString(R.string.season_number, season));
        infoText.append(" ");
        infoText.append(getString(R.string.episode_number, number));
        Integer episodeAbsoluteNumber = episode.getAbsoluteNumber();
        if (episodeAbsoluteNumber != null
                && episodeAbsoluteNumber > 0 && episodeAbsoluteNumber != number) {
            infoText.append(" (").append(episodeAbsoluteNumber).append(")");
        }
        textEpisodeNumbers.setText(infoText);

        // air date
        long releaseTime = episode.getFirstReleasedMs();
        if (releaseTime != -1) {
            Date actualRelease = TimeTools.applyUserOffset(getContext(), releaseTime);
            // "Oct 31 (Fri)" or "in 14 mins (Fri)"
            String dateTime;
            if (DisplaySettings.isDisplayExactDate(getContext())) {
                dateTime = TimeTools.formatToLocalDateShort(requireContext(), actualRelease);
            } else {
                dateTime = TimeTools.formatToLocalRelativeTime(getContext(), actualRelease);
            }
            textEpisodeTime.setText(getString(R.string.format_date_and_day, dateTime,
                    TimeTools.formatToLocalDay(actualRelease)));
        } else {
            textEpisodeTime.setText(null);
        }

        // collected button
        boolean isCollected = episode.getCollected();
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
                episode.getDvdNumber());
        // guest stars
        isShowingMeta |= ViewTools.setLabelValueOrHide(labelGuestStars, textGuestStars,
                TextTools.splitAndKitTVDBStrings(episode.getGuestStars()));
        // hide divider if no meta is visible
        dividerEpisodeMeta.setVisibility(isShowingMeta ? View.VISIBLE : View.GONE);

        // trakt rating
        textRating.setText(TraktTools.buildRatingString(episode.getRatingGlobal()));
        textRatingVotes.setText(TraktTools.buildRatingVotesString(getActivity(),
                episode.getRatingVotes()));

        // user rating
        textUserRating.setText(TraktTools.buildUserRatingString(getActivity(),
                episode.getRatingUser()));

        // IMDb button
        String imdbId = episode.getImdbId();
        if (TextUtils.isEmpty(imdbId) && show != null) {
            // fall back to show IMDb id
            imdbId = show.getImdbId();
        }
        ServiceUtils.setUpImdbButton(imdbId, buttonImdb);

        // trakt button
        if (episode.getTmdbId() != null) {
            String traktLink = TraktTools.buildEpisodeUrl(episode.getTmdbId());
            ViewTools.openUriOnClick(buttonTrakt, traktLink);
            ClipboardTools.copyTextToClipboardOnLongClick(buttonTrakt, traktLink);
        }
    }

    /**
     * Updates the episode description and TVDB button. Need both show and episode data loaded.
     */
    private void populateEpisodeDescriptionAndTvdbButton() {
        if (show == null || episode == null) {
            // no show or episode data available
            return;
        }
        String overview = episode.getOverview();
        String languageCode = show.getLanguage();
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            overview = TextToolsK.textNoTranslation(requireContext(), languageCode);
        } else if (DisplaySettings.preventSpoilers(getContext())) {
            overview = getString(R.string.no_spoilers);
        }
        textDescription.setText(TextTools.textWithTmdbSource(textDescription.getContext(),
                overview));

        // TMDb button
        final Integer showTmdbId = show.getTmdbId();
        if (showTmdbId != null) {
            String url = TmdbTools
                    .buildEpisodeUrl(showTmdbId, episode.getSeason(), episode.getNumber());
            ViewTools.openUriOnClick(buttonTmdb, url);
            ClipboardTools.copyTextToClipboardOnLongClick(buttonTmdb, url);
        }
    }

    @Override
    public void loadEpisodeActions() {
        // do not load actions if there is no episode
        runIfHasEpisode(episode -> {
            Bundle args = new Bundle();
            args.putLong(ARG_EPISODE_ID, episode.getId());
            LoaderManager.getInstance(this)
                    .restartLoader(OverviewActivity.OVERVIEW_ACTIONS_LOADER_ID, args,
                            episodeActionsLoaderCallbacks);
        });
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
            ServiceUtils.loadWithPicasso(requireContext(),
                    ImageTools.tmdbOrTvdbStillUrl(imagePath, requireContext(), false))
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
        runIfHasEpisode(episode -> {
            if (ratingFetchJob == null || !ratingFetchJob.isActive()) {
                ratingFetchJob = TraktRatingsFetcher.fetchEpisodeRatingsAsync(
                        requireContext(),
                        episode.getId()
                );
            }
        });
    }

    private void populateShowViews(@NonNull SgShow2 show) {
        // set show title in action bar
        String showTitle = show.getTitle();
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
        SgApp.getServicesComponent(requireContext()).showTools()
                .setStatusAndColor(statusText, show.getStatusOrUnknown());

        // favorite
        boolean isFavorite = show.getFavorite();
        buttonFavorite.setImageResource(isFavorite
                ? R.drawable.ic_star_black_24dp
                : R.drawable.ic_star_border_black_24dp);
        buttonFavorite.setContentDescription(getString(isFavorite ? R.string.context_unfavorite
                : R.string.context_favorite));
        CheatSheet.setup(buttonFavorite);
        buttonFavorite.setTag(isFavorite);

        // Regular network, release time and length.
        String network = show.getNetwork();
        String time = null;
        Integer releaseTime = show.getReleaseTime();
        if (releaseTime != null && releaseTime != -1) {
            int weekDay = show.getReleaseWeekDayOrDefault();
            Date release = TimeTools.getShowReleaseDateTime(requireContext(),
                    releaseTime,
                    weekDay,
                    show.getReleaseTimeZone(),
                    show.getReleaseCountry(),
                    network);
            String dayString = TimeTools.formatToLocalDayOrDaily(requireContext(), release, weekDay);
            String timeString = TimeTools.formatToLocalTime(requireContext(), release);
            // "Mon 08:30"
            time = dayString + " " + timeString;
        }
        String runtime = getString(
                R.string.runtime_minutes,
                String.valueOf(show.getRuntime())
        );
        String combinedString = TextTools.dotSeparate(TextTools.dotSeparate(network, time), runtime);
        TextView textViewNetworkAndTime = getView().findViewById(R.id.showmeta);
        textViewNetworkAndTime.setText(combinedString);
        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        ClipboardTools.copyTextToClipboardOnLongClick(textViewNetworkAndTime);

        // episode description might need show language, so update it here as well
        populateEpisodeDescriptionAndTvdbButton();
    }

    private interface EpisodeBlock {
        void run(@NonNull SgEpisode2 episode);
    }

    private void runIfHasEpisode(EpisodeBlock block) {
        SgEpisode2 currentEpisode = this.episode;
        if (currentEpisode != null) {
            block.run(currentEpisode);
        }
    }

    private interface EpisodeTvdbIdBlock {
        void run(@NonNull SgEpisode2 episode, int episodeTvdbId);
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

    private final LoaderManager.LoaderCallbacks<List<Action>> episodeActionsLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<Action>>() {
                @Override
                public Loader<List<Action>> onCreateLoader(int id, Bundle args) {
                    long episodeId = args.getLong(ARG_EPISODE_ID);
                    return new EpisodeActionsLoader(getActivity(), episodeId);
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
