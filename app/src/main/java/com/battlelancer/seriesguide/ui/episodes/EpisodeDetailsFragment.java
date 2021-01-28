package com.battlelancer.seriesguide.ui.episodes;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.databinding.ButtonsEpisodeBinding;
import com.battlelancer.seriesguide.databinding.ButtonsEpisodeMoreBinding;
import com.battlelancer.seriesguide.databinding.ButtonsServicesBinding;
import com.battlelancer.seriesguide.databinding.FragmentEpisodeBinding;
import com.battlelancer.seriesguide.databinding.LayoutEpisodeBinding;
import com.battlelancer.seriesguide.databinding.RatingsShowsBinding;
import com.battlelancer.seriesguide.extensions.ActionsHelper;
import com.battlelancer.seriesguide.extensions.EpisodeActionsContract;
import com.battlelancer.seriesguide.extensions.EpisodeActionsLoader;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.model.SgEpisode2;
import com.battlelancer.seriesguide.model.SgShow2;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
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
import com.battlelancer.seriesguide.ui.FullscreenImageActivity;
import com.battlelancer.seriesguide.ui.comments.TraktCommentsActivity;
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.ClipboardTools;
import com.battlelancer.seriesguide.util.DialogTools;
import com.battlelancer.seriesguide.util.LanguageTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TextToolsK;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.uwetrottmann.androidutils.CheatSheet;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import kotlinx.coroutines.Job;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Displays details about a single episode like summary, ratings and episode image if available.
 */
public class EpisodeDetailsFragment extends Fragment implements EpisodeActionsContract {

    private static final String ARG_LONG_EPISODE_ID = "episode_id";
    private static final String KEY_EPISODE_TVDB_ID = "episodeTvdbId";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Job ratingFetchJob;

    private long episodeId;
    @Nullable private SgEpisode2 episode;
    @Nullable private SgShow2 show;
    private String episodeTitle;
    private int episodeFlag;
    private boolean collected;

    private LayoutEpisodeBinding binding;
    private ButtonsEpisodeBinding bindingButtons;
    private RatingsShowsBinding bindingRatings;
    private ButtonsServicesBinding bindingActions;
    private ButtonsEpisodeMoreBinding bindingBottom;

    public static EpisodeDetailsFragment newInstance(long episodeId) {
        EpisodeDetailsFragment f = new EpisodeDetailsFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_LONG_EPISODE_ID, episodeId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            episodeId = args.getLong(ARG_LONG_EPISODE_ID);
        } else {
            throw new IllegalArgumentException("Missing arguments");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentEpisodeBinding bindingRoot = FragmentEpisodeBinding.inflate(inflater, container, false);
        binding = bindingRoot.includeEpisode;
        bindingButtons = binding.includeButtons;
        bindingRatings = binding.includeRatings;
        bindingActions = binding.includeServices;
        bindingBottom = bindingActions.includeMore;
        return bindingRoot.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.getRoot().setVisibility(View.GONE);

        bindingRatings.textViewRatingsRange.setText(getString(R.string.format_rating_range, 10));

        bindingButtons.buttonEpisodeStreamingSearch.setOnClickListener(
                v -> onButtonStreamingSearchClick());
        bindingButtons.buttonEpisodeStreamingSearch.setOnLongClickListener(
                v -> onButtonStreamingSearchLongClick());

        // other bottom buttons
        bindingBottom.buttonEpisodeShare.setOnClickListener(v -> shareEpisode());
        bindingBottom.buttonEpisodeCalendar.setOnClickListener(v -> {
            if (show != null && episode != null) {
                ShareUtils.suggestCalendarEvent(
                        getActivity(),
                        show.getTitle(),
                        TextTools.getNextEpisodeString(
                                getActivity(),
                                episode.getSeason(),
                                episode.getNumber(),
                                episodeTitle
                        ),
                        episode.getFirstReleasedMs(),
                        show.getRuntime()
                );
            }
        });
        bindingBottom.buttonEpisodeLists.setOnClickListener(v -> {
            if (episode != null && episode.getTvdbId() != null) {
                ManageListsDialogFragment.show(
                        getParentFragmentManager(),
                        episode.getTvdbId(),
                        ListItemTypes.EPISODE
                );
            }
        });

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        ClipboardTools.copyTextToClipboardOnLongClick(binding.textviewTitle);
        ClipboardTools.copyTextToClipboardOnLongClick(binding.textviewReleaseTime);
        ClipboardTools.copyTextToClipboardOnLongClick(binding.textviewDescription);
        ClipboardTools.copyTextToClipboardOnLongClick(binding.textviewGuestStars);
        ClipboardTools.copyTextToClipboardOnLongClick(binding.textviewDirectors);
        ClipboardTools.copyTextToClipboardOnLongClick(binding.textviewWriters);
        ClipboardTools.copyTextToClipboardOnLongClick(binding.textviewDvd);
        ClipboardTools.copyTextToClipboardOnLongClick(binding.textviewReleaseDate);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        EpisodeDetailsViewModel model = new ViewModelProvider(this,
                new EpisodeDetailsViewModelFactory(episodeId, requireActivity().getApplication()))
                .get(EpisodeDetailsViewModel.class);
        // Once episode is loaded, trigger show loading: so set show observer first.
        model.getShow().observe(getViewLifecycleOwner(), show -> {
            if (show != null) {
                SgEpisode2 episode = model.getEpisode().getValue();
                if (episode != null) {
                    populateEpisodeData(episode, show);
                    return;
                }
            }
            // no data to display
            if (binding != null) {
                binding.getRoot().setVisibility(View.GONE);
            }
        });
        model.getEpisode().observe(getViewLifecycleOwner(), sgEpisode2 -> {
            if (sgEpisode2 != null) {
                model.getShowId().postValue(sgEpisode2.getShowId());
            } else {
                // no data to display
                if (binding != null) {
                    binding.getRoot().setVisibility(View.GONE);
                }
            }
        });
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
        super.onPause();

        if (handler != null) {
            handler.removeCallbacks(actionsRunnable);
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the fragment from
        // being garbage collected. It also prevents our callback from getting invoked even after the
        // fragment is destroyed.
        Picasso.get().cancelRequest(binding.imageviewScreenshot);
        binding = null;
        bindingButtons = null;
        bindingRatings = null;
        bindingActions = null;
        bindingBottom = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Release reference to any job.
        ratingFetchJob = null;
    }

    /**
     * If episode was watched, flags as unwatched. Otherwise, flags as watched.
     */
    private void onToggleWatched() {
        boolean watched = EpisodeTools.isWatched(episodeFlag);
        if (watched) {
            View anchor = bindingButtons.buttonEpisodeWatched;
            PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
            popupMenu.inflate(R.menu.watched_popup_menu);
            popupMenu.setOnMenuItemClickListener(watchedEpisodePopupMenuListener);
            popupMenu.show();
        } else {
            changeEpisodeFlag(EpisodeFlags.WATCHED);
        }
    }

    private final PopupMenu.OnMenuItemClickListener watchedEpisodePopupMenuListener = item -> {
        int itemId = item.getItemId();
        if (itemId == R.id.watched_popup_menu_watch_again) {
            // Multiple plays are for supporters only.
            if (!Utils.hasAccessToX(requireContext())) {
                Utils.advertiseSubscription(requireContext());
            } else {
                changeEpisodeFlag(EpisodeFlags.WATCHED);
            }
        } else if (itemId == R.id.watched_popup_menu_set_not_watched) {
            changeEpisodeFlag(EpisodeFlags.UNWATCHED);
        }
        return true;
    };

    /**
     * If episode was skipped, flags as unwatched. Otherwise, flags as skipped.
     */
    private void onToggleSkipped() {
        boolean skipped = EpisodeTools.isSkipped(episodeFlag);
        changeEpisodeFlag(skipped ? EpisodeFlags.UNWATCHED : EpisodeFlags.SKIPPED);
    }

    private void changeEpisodeFlag(int episodeFlag) {
        this.episodeFlag = episodeFlag;
        EpisodeTools.episodeWatched(requireContext(), episodeId, episodeFlag);
    }

    private void onToggleCollected() {
        collected = !collected;
        EpisodeTools.episodeCollected(requireContext(), episodeId, collected);
    }

    private void onButtonStreamingSearchClick() {
        SgShow2 showOrNull = this.show;
        if (showOrNull == null) {
            return;
        }
        if (StreamingSearch.isNotConfigured(requireContext())) {
            showStreamingSearchConfigDialog();
        } else {
            StreamingSearch.searchForShow(requireContext(), showOrNull.getTitle());
        }
    }

    private boolean onButtonStreamingSearchLongClick() {
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
            bindingButtons.buttonEpisodeStreamingSearch.setVisibility(View.GONE);
        } else {
            onButtonStreamingSearchClick();
        }
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event) {
        SgEpisode2 episodeOrNull = this.episode;
        if (episodeOrNull != null && episodeOrNull.getTvdbId() != null) {
            if (episodeOrNull.getTvdbId() == event.episodeTvdbId) {
                loadEpisodeActionsDelayed();
            }
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
        bindingButtons.buttonEpisodeWatched.setEnabled(enabled);
        bindingButtons.buttonEpisodeCollected.setEnabled(enabled);
        bindingButtons.buttonEpisodeSkip.setEnabled(enabled);
        bindingButtons.buttonEpisodeCheckin.setEnabled(enabled);
        bindingButtons.buttonEpisodeWatchedUpTo.setEnabled(enabled);
        bindingButtons.buttonEpisodeStreamingSearch.setEnabled(enabled);
    }

    private void populateEpisodeData(SgEpisode2 episode, SgShow2 show) {
        this.episode = episode;
        this.show = show;

        int episodeNumber = episode.getNumber();

        // title and description
        episodeFlag = episode.getWatched();
        episodeTitle = TextTools.getEpisodeTitle(requireContext(),
                episode.getTitle(), episode.getNumber());
        boolean hideDetails = EpisodeTools.isUnwatched(episodeFlag)
                && DisplaySettings.preventSpoilers(requireContext());
        binding.textviewTitle.setText(
                TextTools.getEpisodeTitle(requireContext(), hideDetails ? null : episodeTitle,
                        episodeNumber));
        String overview = episode.getOverview();
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            String languageCode = show.getLanguage();
            overview = getString(R.string.no_translation,
                    LanguageTools.getShowLanguageStringFor(getContext(), languageCode),
                    getString(R.string.tvdb));
        } else if (hideDetails) {
            overview = getString(R.string.no_spoilers);
        }
        long lastEditSeconds = episode.getLastEditedSec();
        binding.textviewDescription.setText(
                TextTools.textWithTvdbSource(binding.textviewDescription.getContext(), overview,
                        lastEditSeconds));

        // release date, also build release time and day
        boolean isReleased;
        String timeText;
        long episodeReleaseTime = episode.getFirstReleasedMs();
        if (episodeReleaseTime != -1) {
            Date actualRelease = TimeTools.applyUserOffset(requireContext(), episodeReleaseTime);
            isReleased = TimeTools.isReleased(actualRelease);
            binding.textviewReleaseDate.setText(
                    TimeTools.formatToLocalDateAndDay(requireContext(), actualRelease));

            String dateTime;
            if (DisplaySettings.isDisplayExactDate(requireContext())) {
                // "31. October 2010"
                dateTime = TimeTools.formatToLocalDate(requireContext(), actualRelease);
            } else {
                // "in 15 mins"
                dateTime = TimeTools.formatToLocalRelativeTime(requireContext(), actualRelease);
            }
            // append day: "in 15 mins (Fri)"
            timeText = getString(R.string.format_date_and_day, dateTime,
                    TimeTools.formatToLocalDay(actualRelease)).toUpperCase(Locale.getDefault());
        } else {
            binding.textviewReleaseDate.setText(R.string.unknown);
            timeText = getString(R.string.episode_firstaired_unknown);
            isReleased = false;
        }
        // absolute number (e.g. relevant for Anime): "ABSOLUTE 142"
        Integer absoluteNumber = episode.getAbsoluteNumber();
        String absoluteNumberText = null;
        if (absoluteNumber != null && absoluteNumber > 0) {
            absoluteNumberText = NumberFormat.getIntegerInstance().format(absoluteNumber);
        }
        binding.textviewReleaseTime.setText(TextTools.dotSeparate(timeText, absoluteNumberText));

        // dim text color for title if not released
        TextViewCompat.setTextAppearance(binding.textviewTitle, isReleased
                ? R.style.TextAppearance_SeriesGuide_Headline6
                : R.style.TextAppearance_SeriesGuide_Headline6_Dim);
        if (!isReleased) {
            TextViewCompat.setTextAppearance(binding.textviewReleaseTime,
                    R.style.TextAppearance_SeriesGuide_Caption_Dim);
        }

        // guest stars
        ViewTools.setLabelValueOrHide(binding.textviewGuestStarsLabel, binding.textviewGuestStars,
                TextTools.splitAndKitTVDBStrings(episode.getGuestStars())
        );
        // DVD episode number
        ViewTools.setLabelValueOrHide(binding.textviewDvdLabel, binding.textviewDvd,
                episode.getDvdNumber());
        // directors
        ViewTools.setValueOrPlaceholder(binding.textviewDirectors,
                TextTools.splitAndKitTVDBStrings(episode.getDirectors()));
        // writers
        ViewTools.setValueOrPlaceholder(binding.textviewWriters,
                TextTools.splitAndKitTVDBStrings(episode.getWriters()));

        // ratings
        bindingRatings.getRoot().setOnClickListener(v -> rateEpisode());
        CheatSheet.setup(bindingRatings.getRoot(), R.string.action_rate);

        // trakt rating
        bindingRatings.textViewRatingsValue.setText(
                TraktTools.buildRatingString(episode.getRatingGlobal()));
        bindingRatings.textViewRatingsVotes
                .setText(TraktTools.buildRatingVotesString(requireContext(),
                        episode.getRatingVotes()));

        // user rating
        bindingRatings.textViewRatingsUser
                .setText(TraktTools.buildUserRatingString(requireContext(),
                        episode.getRatingUser()));

        // episode image
        final String imagePath = episode.getImage();
        binding.containerImage.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), FullscreenImageActivity.class);
            intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE,
                    TvdbImageTools.artworkUrl(imagePath));
            Utils.startActivityWithAnimation(requireActivity(), intent, v);
        });
        loadImage(imagePath, hideDetails);

        // Buttons.
        updatePrimaryButtons(episode, show);
        updateSecondaryButtons(episode, show);

        binding.getRoot().setVisibility(View.VISIBLE);

        loadDetails();
    }

    private void updatePrimaryButtons(SgEpisode2 episode, SgShow2 show) {
        // Check in button.
        bindingButtons.buttonEpisodeCheckin.setOnClickListener(v -> {
            if (episode.getTvdbId() != null) {
                CheckInDialogFragment
                        .show(requireContext(), getParentFragmentManager(), episode.getTvdbId());
            }
        });
        CheatSheet.setup(bindingButtons.buttonEpisodeCheckin);
        // hide check-in if not connected to trakt or hexagon is enabled
        boolean isConnectedToTrakt = TraktCredentials.get(requireContext()).hasCredentials();
        boolean displayCheckIn = isConnectedToTrakt && !HexagonSettings.isEnabled(requireContext());
        bindingButtons.buttonEpisodeCheckin
                .setVisibility(displayCheckIn ? View.VISIBLE : View.GONE);

        // Watched up to button.
        boolean isWatched = EpisodeTools.isWatched(episodeFlag);
        boolean displayWatchedUpTo = !isWatched;
        bindingButtons.buttonEpisodeWatchedUpTo
                .setVisibility(displayWatchedUpTo ? View.VISIBLE : View.GONE);
        bindingButtons.buttonEpisodeWatchedUpTo
                .setNextFocusUpId(displayCheckIn ? R.id.buttonCheckIn : R.id.buttonEpisodeWatched);
        bindingButtons.buttonEpisodeWatchedUpTo.setOnClickListener(v -> DialogTools.safeShow(
                EpisodeWatchedUpToDialog.newInstance(show.getId(), episode.getFirstReleasedMs(),
                        episode.getNumber()),
                getParentFragmentManager(), "EpisodeWatchedUpToDialog"
        ));

        // Streaming search button.
        int streamingSearchNextFocusUpId;
        if (displayWatchedUpTo) {
            streamingSearchNextFocusUpId = R.id.buttonEpisodeWatchedUpTo;
        } else if (displayCheckIn) {
            streamingSearchNextFocusUpId = R.id.buttonCheckIn;
        } else {
            streamingSearchNextFocusUpId = R.id.buttonEpisodeWatched;
        }
        bindingButtons.buttonEpisodeStreamingSearch.setNextFocusUpId(streamingSearchNextFocusUpId);
        // hide streaming search if turned off
        boolean displayStreamingSearch = !StreamingSearch.isTurnedOff(requireContext());
        bindingButtons.buttonEpisodeStreamingSearch
                .setVisibility(displayStreamingSearch ? View.VISIBLE : View.GONE);

        bindingButtons.dividerEpisodeButtons.setVisibility(displayCheckIn || displayStreamingSearch
                ? View.VISIBLE : View.GONE);

        // watched button
        if (isWatched) {
            ViewTools.setVectorDrawableTop(bindingButtons.buttonEpisodeWatched,
                    R.drawable.ic_watched_24dp);
        } else {
            ViewTools.setVectorDrawableTop(bindingButtons.buttonEpisodeWatched,
                    R.drawable.ic_watch_black_24dp);
        }
        bindingButtons.buttonEpisodeWatched.setOnClickListener(v -> onToggleWatched());
        Integer plays = episode.getPlays();
        bindingButtons.buttonEpisodeWatched
                .setText(TextToolsK.getWatchedButtonText(requireContext(), isWatched, plays));
        CheatSheet.setup(bindingButtons.buttonEpisodeWatched, isWatched ? R.string.action_unwatched
                : R.string.action_watched);

        // collected button
        collected = episode.getCollected();
        if (collected) {
            ViewTools.setVectorDrawableTop(bindingButtons.buttonEpisodeCollected,
                    R.drawable.ic_collected_24dp);
        } else {
            ViewTools.setVectorDrawableTop(bindingButtons.buttonEpisodeCollected,
                    R.drawable.ic_collect_black_24dp);
        }
        bindingButtons.buttonEpisodeCollected.setOnClickListener(v -> onToggleCollected());
        bindingButtons.buttonEpisodeCollected.setText(collected
                ? R.string.state_in_collection : R.string.action_collection_add);
        CheatSheet.setup(bindingButtons.buttonEpisodeCollected, collected
                ? R.string.action_collection_remove : R.string.action_collection_add);

        // skip button
        if (isWatched) {
            // if watched do not allow skipping
            bindingButtons.buttonEpisodeSkip.setVisibility(View.INVISIBLE);
        } else {
            bindingButtons.buttonEpisodeSkip.setVisibility(View.VISIBLE);

            boolean isSkipped = EpisodeTools.isSkipped(episodeFlag);
            if (isSkipped) {
                ViewTools.setVectorDrawableTop(bindingButtons.buttonEpisodeSkip,
                        R.drawable.ic_skipped_24dp);
            } else {
                ViewTools.setVectorDrawableTop(bindingButtons.buttonEpisodeSkip,
                        R.drawable.ic_skip_black_24dp);
            }
            bindingButtons.buttonEpisodeSkip.setOnClickListener(v -> onToggleSkipped());
            bindingButtons.buttonEpisodeSkip
                    .setText(isSkipped ? R.string.state_skipped : R.string.action_skip);
            CheatSheet.setup(bindingButtons.buttonEpisodeSkip,
                    isSkipped ? R.string.action_dont_skip : R.string.action_skip);
        }
    }

    private void updateSecondaryButtons(SgEpisode2 episode, SgShow2 show) {
        // trakt
        if (episode.getTvdbId() != null) {
            String traktLink = TraktTools.buildEpisodeUrl(episode.getTvdbId());
            ViewTools.openUriOnClick(bindingBottom.buttonEpisodeTrakt, traktLink);
            ClipboardTools
                    .copyTextToClipboardOnLongClick(bindingBottom.buttonEpisodeTrakt, traktLink);
        }

        // IMDb
        String imdbId = episode.getImdbId();
        if (TextUtils.isEmpty(imdbId)) {
            // fall back to show IMDb id
            imdbId = show.getImdbId();
        }
        ServiceUtils.setUpImdbButton(imdbId, bindingBottom.buttonEpisodeImdb);

        // TVDb
        if (episode.getTvdbId() != null) {
            String tvdbLink = TvdbLinks
                    .episode(show.getSlug(), null, null, episode.getTvdbId());
            ViewTools.openUriOnClick(bindingBottom.buttonEpisodeTvdb, tvdbLink);
            ClipboardTools
                    .copyTextToClipboardOnLongClick(bindingBottom.buttonEpisodeTvdb, tvdbLink);

            // trakt comments
            bindingBottom.buttonEpisodeComments.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), TraktCommentsActivity.class);
                intent.putExtras(TraktCommentsActivity.createInitBundleEpisode(episodeTitle,
                        episode.getTvdbId()));
                Utils.startActivityWithAnimation(requireActivity(), intent, v);
            });
        }
    }

    private void loadDetails() {
        SgShow2 showOrNull = this.show;
        SgEpisode2 episodeOrNull = this.episode;
        if (showOrNull != null && showOrNull.getTvdbId() != null
                && episodeOrNull != null && episodeOrNull.getTvdbId() != null) {
            // update trakt ratings
            if (ratingFetchJob == null || !ratingFetchJob.isActive()) {
                ratingFetchJob = TraktRatingsFetcher.fetchEpisodeRatingsAsync(
                        requireContext(),
                        showOrNull.getTvdbId(),
                        episodeOrNull.getTvdbId(),
                        episodeOrNull.getSeason(),
                        episodeOrNull.getNumber()
                );
            }
        }
    }

    private void rateEpisode() {
        SgEpisode2 episodeOrNull = this.episode;
        if (episodeOrNull != null && episodeOrNull.getTvdbId() != null) {
            RateDialogFragment.newInstanceEpisode(episodeOrNull.getTvdbId())
                    .safeShow(getContext(), getParentFragmentManager());
        }
    }

    private void shareEpisode() {
        if (episodeTitle == null) {
            return;
        }
        SgShow2 showOrNull = this.show;
        SgEpisode2 episodeOrNull = this.episode;
        if (showOrNull != null && showOrNull.getTvdbId() != null
                && episodeOrNull != null && episodeOrNull.getTvdbId() != null) {
            ShareUtils.shareEpisode(requireActivity(), show.getSlug(), showOrNull.getTvdbId(), null,
                    episodeOrNull.getTvdbId(), episodeOrNull.getSeason(), episodeOrNull.getNumber(),
                    showOrNull.getTitle(), episodeTitle);
        }
    }

    private void loadImage(String imagePath, boolean hideDetails) {
        // immediately hide container if there is no image
        if (TextUtils.isEmpty(imagePath)) {
            binding.containerImage.setVisibility(View.GONE);
            return;
        }

        if (hideDetails) {
            // show image placeholder
            binding.imageviewScreenshot.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            binding.imageviewScreenshot.setImageResource(R.drawable.ic_photo_gray_24dp);
        } else {
            // try loading image
            binding.containerImage.setVisibility(View.VISIBLE);
            ServiceUtils.loadWithPicasso(requireContext(), TvdbImageTools.artworkUrl(imagePath))
                    .error(R.drawable.ic_photo_gray_24dp)
                    .into(binding.imageviewScreenshot,
                            new Callback() {
                                @Override
                                public void onSuccess() {
                                    binding.imageviewScreenshot
                                            .setScaleType(ImageView.ScaleType.CENTER_CROP);
                                }

                                @Override
                                public void onError(Exception e) {
                                    binding.imageviewScreenshot
                                            .setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                }
                            }
                    );
        }
    }

    private LoaderManager.LoaderCallbacks<List<Action>> actionsLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<Action>>() {
                @Override
                public Loader<List<Action>> onCreateLoader(int id, Bundle args) {
                    int episodeTvdbId = args.getInt(KEY_EPISODE_TVDB_ID);
                    return new EpisodeActionsLoader(requireContext(), episodeTvdbId);
                }

                @Override
                public void onLoadFinished(@NonNull Loader<List<Action>> loader,
                        List<Action> data) {
                    if (!isAdded()) {
                        return;
                    }
                    if (data == null) {
                        Timber.e("onLoadFinished: did not receive valid actions for %s",
                                episodeId);
                    } else {
                        Timber.d("onLoadFinished: received %s actions for %s", data.size(),
                                episodeId);
                    }
                    ActionsHelper.populateActions(requireActivity().getLayoutInflater(),
                            requireActivity().getTheme(), bindingActions.containerEpisodeActions,
                            data);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<List<Action>> loader) {
                    // do nothing, we are not holding onto the actions list
                }
            };

    public void loadEpisodeActions() {
        SgEpisode2 episodeOrNull = this.episode;
        if (episodeOrNull != null && episodeOrNull.getTvdbId() != null) {
            Bundle args = new Bundle();
            args.putInt(KEY_EPISODE_TVDB_ID, episodeOrNull.getTvdbId());
            LoaderManager.getInstance(this)
                    .restartLoader(EpisodesActivity.ACTIONS_LOADER_ID, args,
                            actionsLoaderCallbacks);
        }
    }

    Runnable actionsRunnable = this::loadEpisodeActions;

    public void loadEpisodeActionsDelayed() {
        handler.removeCallbacks(actionsRunnable);
        handler.postDelayed(actionsRunnable,
                EpisodeActionsContract.ACTION_LOADER_DELAY_MILLIS);
    }
}
