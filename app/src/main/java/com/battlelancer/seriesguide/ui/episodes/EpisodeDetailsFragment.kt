package com.battlelancer.seriesguide.ui.episodes

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.ButtonsEpisodeBinding
import com.battlelancer.seriesguide.databinding.ButtonsEpisodeMoreBinding
import com.battlelancer.seriesguide.databinding.ButtonsServicesBinding
import com.battlelancer.seriesguide.databinding.FragmentEpisodeBinding
import com.battlelancer.seriesguide.databinding.LayoutEpisodeBinding
import com.battlelancer.seriesguide.databinding.RatingsShowsBinding
import com.battlelancer.seriesguide.extensions.ActionsHelper
import com.battlelancer.seriesguide.extensions.EpisodeActionsContract
import com.battlelancer.seriesguide.extensions.EpisodeActionsLoader
import com.battlelancer.seriesguide.extensions.ExtensionManager.EpisodeActionReceivedEvent
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.DisplaySettings.isDisplayExactDate
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktRatingsFetcher
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceActiveEvent
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceCompletedEvent
import com.battlelancer.seriesguide.ui.FullscreenImageActivity.Companion.intent
import com.battlelancer.seriesguide.ui.comments.TraktCommentsActivity.Companion.intentEpisode
import com.battlelancer.seriesguide.util.ImageTools.tmdbOrTvdbStillUrl
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.TmdbTools
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.battlelancer.seriesguide.util.safeShow
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale

/**
 * Displays details about a single episode like summary, ratings and episode image if available.
 */
class EpisodeDetailsFragment : Fragment(), EpisodeActionsContract {

    private val handler = Handler(Looper.getMainLooper())
    private var ratingFetchJob: Job? = null

    private var episodeId: Long = 0
    private var episode: SgEpisode2? = null
    private var show: SgShow2? = null
    private var episodeTitle: String? = null
    private var episodeFlag = 0
    private var collected = false

    private var binding: LayoutEpisodeBinding? = null
    private var bindingButtons: ButtonsEpisodeBinding? = null
    private var bindingRatings: RatingsShowsBinding? = null
    private var bindingActions: ButtonsServicesBinding? = null
    private var bindingBottom: ButtonsEpisodeMoreBinding? = null

    private val model by viewModels<EpisodeDetailsViewModel> {
        EpisodeDetailsViewModelFactory(episodeId, requireActivity().application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        episodeId = arguments?.getLong(ARG_LONG_EPISODE_ID)
            ?: throw IllegalArgumentException("Missing arguments")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bindingRoot = FragmentEpisodeBinding.inflate(inflater, container, false)
        binding = bindingRoot.includeEpisode.also { binding ->
            bindingButtons = binding.includeButtons
            bindingRatings = binding.includeRatings
            bindingActions = binding.includeServices.also {
                bindingBottom = it.includeMore
            }
        }
        return bindingRoot.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding!!
        binding.root.visibility = View.GONE

        bindingRatings!!.textViewRatingsRange.text = getString(R.string.format_rating_range, 10)

        StreamingSearch.initButtons(
            bindingButtons!!.buttonEpisodeStreamingSearch,
            bindingButtons!!.buttonEpisodeStreamingSearchInfo,
            parentFragmentManager
        )

        // other bottom buttons
        bindingBottom!!.buttonEpisodeShare.setOnClickListener { shareEpisode() }
        bindingBottom!!.buttonEpisodeCalendar.setOnClickListener {
            val show = show
            val episode = episode
            if (show != null && episode != null) {
                ShareUtils.suggestCalendarEvent(
                    requireContext(),
                    show.title,
                    TextTools.getNextEpisodeString(
                        requireContext(),
                        episode.season,
                        episode.number,
                        episodeTitle
                    ),
                    episode.firstReleasedMs,
                    show.runtime
                )
            }
        }

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        binding.textviewTitle.copyTextToClipboardOnLongClick()
        binding.textviewReleaseTime.copyTextToClipboardOnLongClick()
        binding.textviewDescription.copyTextToClipboardOnLongClick()
        binding.textviewGuestStars.copyTextToClipboardOnLongClick()
        binding.textviewDirectors.copyTextToClipboardOnLongClick()
        binding.textviewWriters.copyTextToClipboardOnLongClick()
        binding.textviewDvd.copyTextToClipboardOnLongClick()
        binding.textviewReleaseDate.copyTextToClipboardOnLongClick()

        // Once episode is loaded, trigger show loading: so set show observer first.
        model.show.observe(viewLifecycleOwner, { show: SgShow2? ->
            if (show != null) {
                if (show.tmdbId != null) {
                    model.setShowTmdbId(show.tmdbId)
                }
                val episode = model.episode.value
                if (episode != null) {
                    populateEpisodeData(episode, show)
                    return@observe
                }
            }
            // no data to display
            binding.root.visibility = View.GONE
        })
        model.episode.observe(viewLifecycleOwner, { sgEpisode2: SgEpisode2? ->
            if (sgEpisode2 != null) {
                model.showId.postValue(sgEpisode2.showId)
            } else {
                // no data to display
                binding.root.visibility = View.GONE
            }
        })
        model.watchProvider.observe(viewLifecycleOwner, { watchInfo: TmdbTools2.WatchInfo? ->
            val b = this.binding
            if (watchInfo != null && b != null) {
                StreamingSearch.configureButton(
                    b.includeButtons.buttonEpisodeStreamingSearch,
                    watchInfo,
                    true
                )
            }
        })
    }

    override fun onResume() {
        super.onResume()

        val event = EventBus.getDefault().getStickyEvent(ServiceActiveEvent::class.java)
        setEpisodeButtonsEnabled(event == null)

        EventBus.getDefault().register(this)
        loadEpisodeActionsDelayed()
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(actionsRunnable)
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the fragment from
        // being garbage collected. It also prevents our callback from getting invoked even after the
        // fragment is destroyed.
        Picasso.get().cancelRequest(binding!!.imageviewScreenshot)
        binding = null
        bindingButtons = null
        bindingRatings = null
        bindingActions = null
        bindingBottom = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release reference to any job.
        ratingFetchJob = null
    }

    /**
     * If episode was watched, flags as unwatched. Otherwise, flags as watched.
     */
    private fun onToggleWatched() {
        val watched = EpisodeTools.isWatched(episodeFlag)
        if (watched) {
            val anchor: View = bindingButtons!!.buttonEpisodeWatched
            val popupMenu = PopupMenu(anchor.context, anchor)
            popupMenu.inflate(R.menu.watched_popup_menu)
            popupMenu.setOnMenuItemClickListener(watchedEpisodePopupMenuListener)
            popupMenu.show()
        } else {
            changeEpisodeFlag(EpisodeFlags.WATCHED)
        }
    }

    private val watchedEpisodePopupMenuListener =
        PopupMenu.OnMenuItemClickListener { item: MenuItem ->
            val itemId = item.itemId
            if (itemId == R.id.watched_popup_menu_watch_again) {
                // Multiple plays are for supporters only.
                if (!Utils.hasAccessToX(requireContext())) {
                    Utils.advertiseSubscription(requireContext())
                } else {
                    changeEpisodeFlag(EpisodeFlags.WATCHED)
                }
            } else if (itemId == R.id.watched_popup_menu_set_not_watched) {
                changeEpisodeFlag(EpisodeFlags.UNWATCHED)
            }
            true
        }

    /**
     * If episode was skipped, flags as unwatched. Otherwise, flags as skipped.
     */
    private fun onToggleSkipped() {
        val skipped = EpisodeTools.isSkipped(episodeFlag)
        changeEpisodeFlag(if (skipped) EpisodeFlags.UNWATCHED else EpisodeFlags.SKIPPED)
    }

    private fun changeEpisodeFlag(episodeFlag: Int) {
        this.episodeFlag = episodeFlag
        EpisodeTools.episodeWatched(requireContext(), episodeId, episodeFlag)
    }

    private fun onToggleCollected() {
        collected = !collected
        EpisodeTools.episodeCollected(requireContext(), episodeId, collected)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onEventMainThread(event: EpisodeActionReceivedEvent) {
        val episodeOrNull = episode
        if (episodeOrNull != null) {
            if (episodeOrNull.tmdbId == event.episodeTmdbId) {
                loadEpisodeActionsDelayed()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventEpisodeTask(@Suppress("UNUSED_PARAMETER") event: ServiceActiveEvent?) {
        setEpisodeButtonsEnabled(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventEpisodeTask(@Suppress("UNUSED_PARAMETER") event: ServiceCompletedEvent?) {
        setEpisodeButtonsEnabled(true)
    }

    private fun setEpisodeButtonsEnabled(enabled: Boolean) {
        bindingButtons?.apply {
            buttonEpisodeWatched.isEnabled = enabled
            buttonEpisodeCollected.isEnabled = enabled
            buttonEpisodeSkip.isEnabled = enabled
            buttonEpisodeCheckin.isEnabled = enabled
            buttonEpisodeWatchedUpTo.isEnabled = enabled
            buttonEpisodeStreamingSearch.isEnabled = enabled
        }
    }

    private fun populateEpisodeData(episode: SgEpisode2, show: SgShow2) {
        this.episode = episode
        this.show = show

        val binding = binding ?: return
        val bindingRatings = bindingRatings ?: return

        ViewTools.configureNotMigratedWarning(
            binding.textViewEpisodeNotMigrated,
            episode.tmdbId == null
        )

        val episodeNumber = episode.number

        // title and description
        episodeFlag = episode.watched
        episodeTitle = TextTools.getEpisodeTitle(
            requireContext(),
            episode.title, episode.number
        )
        val hideDetails = EpisodeTools.isUnwatched(episodeFlag)
                && DisplaySettings.preventSpoilers(requireContext())
        binding.textviewTitle.text = TextTools.getEpisodeTitle(
            requireContext(), if (hideDetails) null else episodeTitle, episodeNumber
        )
        var overview = episode.overview
        if (overview.isNullOrEmpty()) {
            // no description available, show no translation available message
            overview = TextTools.textNoTranslation(requireContext(), show.language)
        } else if (hideDetails) {
            overview = getString(R.string.no_spoilers)
        }
        binding.textviewDescription.text =
            TextTools.textWithTmdbSource(binding.textviewDescription.context, overview)

        // release date, also build release time and day
        val isReleased: Boolean
        val timeText: String
        val episodeReleaseTime = episode.firstReleasedMs
        if (episodeReleaseTime != -1L) {
            val actualRelease = TimeTools.applyUserOffset(requireContext(), episodeReleaseTime)
            isReleased = TimeTools.isReleased(actualRelease)
            binding.textviewReleaseDate.text =
                TimeTools.formatToLocalDateAndDay(requireContext(), actualRelease)

            val dateTime: String = if (isDisplayExactDate(requireContext())) {
                // "31. October 2010"
                TimeTools.formatToLocalDate(requireContext(), actualRelease)
            } else {
                // "in 15 mins"
                TimeTools.formatToLocalRelativeTime(requireContext(), actualRelease)
            }
            // append day: "in 15 mins (Fri)"
            timeText = getString(
                R.string.format_date_and_day, dateTime,
                TimeTools.formatToLocalDay(actualRelease)
            ).uppercase(Locale.getDefault())
        } else {
            binding.textviewReleaseDate.setText(R.string.unknown)
            timeText = getString(R.string.episode_firstaired_unknown)
            isReleased = false
        }
        // absolute number (e.g. relevant for Anime): "ABSOLUTE 142"
        val absoluteNumber = episode.absoluteNumber
        var absoluteNumberText: String? = null
        if (absoluteNumber != null && absoluteNumber > 0) {
            absoluteNumberText = NumberFormat.getIntegerInstance().format(absoluteNumber)
        }
        binding.textviewReleaseTime.text = TextTools.dotSeparate(timeText, absoluteNumberText)

        // dim text color for title if not released
        TextViewCompat.setTextAppearance(
            binding.textviewTitle,
            if (isReleased) {
                R.style.TextAppearance_SeriesGuide_Headline6
            } else {
                R.style.TextAppearance_SeriesGuide_Headline6_Dim
            }
        )
        if (!isReleased) {
            TextViewCompat.setTextAppearance(
                binding.textviewReleaseTime,
                R.style.TextAppearance_SeriesGuide_Caption_Dim
            )
        }

        // guest stars
        ViewTools.setLabelValueOrHide(
            binding.textviewGuestStarsLabel, binding.textviewGuestStars,
            TextTools.splitPipeSeparatedStrings(episode.guestStars)
        )
        // DVD episode number
        ViewTools.setLabelValueOrHide(
            binding.textviewDvdLabel, binding.textviewDvd,
            episode.dvdNumber
        )
        // directors
        ViewTools.setValueOrPlaceholder(
            binding.textviewDirectors,
            TextTools.splitPipeSeparatedStrings(episode.directors)
        )
        // writers
        ViewTools.setValueOrPlaceholder(
            binding.textviewWriters,
            TextTools.splitPipeSeparatedStrings(episode.writers)
        )

        // ratings
        bindingRatings.root.setOnClickListener { rateEpisode() }
        TooltipCompat.setTooltipText(
            bindingRatings.root,
            bindingRatings.root.context.getString(R.string.action_rate)
        )

        // trakt rating
        bindingRatings.textViewRatingsValue.text =
            TraktTools.buildRatingString(episode.ratingGlobal)
        bindingRatings.textViewRatingsVotes.text = TraktTools.buildRatingVotesString(
            requireContext(),
            episode.ratingVotes
        )

        // user rating
        bindingRatings.textViewRatingsUser.text = TraktTools.buildUserRatingString(
            requireContext(),
            episode.ratingUser
        )

        // episode image
        val imagePath = episode.image
        binding.containerImage.setOnClickListener { v: View? ->
            val intent = intent(
                requireContext(),
                tmdbOrTvdbStillUrl(imagePath, requireContext(), false),
                tmdbOrTvdbStillUrl(imagePath, requireContext(), true)
            )
            Utils.startActivityWithAnimation(requireActivity(), intent, v)
        }
        loadImage(imagePath, hideDetails)

        // Buttons.
        updatePrimaryButtons(episode, show)
        updateSecondaryButtons(episode, show)

        binding.root.visibility = View.VISIBLE

        loadTraktRatings()
    }

    private fun updatePrimaryButtons(episode: SgEpisode2, show: SgShow2) {
        val bindingButtons = bindingButtons ?: return

        // Check in button.
        bindingButtons.buttonEpisodeCheckin.setOnClickListener {
            CheckInDialogFragment.show(
                requireContext(),
                parentFragmentManager,
                episodeId
            )
        }
        TooltipCompat.setTooltipText(
            bindingButtons.buttonEpisodeCheckin,
            bindingButtons.buttonEpisodeCheckin.contentDescription
        )
        // hide check-in if not connected to trakt or hexagon is enabled
        val isConnectedToTrakt = TraktCredentials.get(requireContext()).hasCredentials()
        val displayCheckIn = isConnectedToTrakt && !HexagonSettings.isEnabled(requireContext())
        bindingButtons.buttonEpisodeCheckin.visibility =
            if (displayCheckIn) View.VISIBLE else View.GONE

        // Watched up to button.
        val isWatched = EpisodeTools.isWatched(episodeFlag)
        val displayWatchedUpTo = !isWatched
        bindingButtons.buttonEpisodeWatchedUpTo.visibility =
            if (displayWatchedUpTo) View.VISIBLE else View.GONE
        bindingButtons.buttonEpisodeWatchedUpTo.nextFocusUpId =
            if (displayCheckIn) R.id.buttonCheckIn else R.id.buttonEpisodeWatched
        bindingButtons.buttonEpisodeWatchedUpTo.setOnClickListener {
            EpisodeWatchedUpToDialog.newInstance(
                show.id, episode.firstReleasedMs,
                episode.number
            ).safeShow(parentFragmentManager, "EpisodeWatchedUpToDialog")
        }

        // Streaming search button.
        val streamingSearchNextFocusUpId: Int = when {
            displayWatchedUpTo -> R.id.buttonEpisodeWatchedUpTo
            displayCheckIn -> R.id.buttonCheckIn
            else -> R.id.buttonEpisodeWatched
        }
        bindingButtons.buttonEpisodeStreamingSearch.nextFocusUpId = streamingSearchNextFocusUpId

        // watched button
        if (isWatched) {
            ViewTools.setVectorDrawableTop(
                bindingButtons.buttonEpisodeWatched,
                R.drawable.ic_watched_24dp
            )
        } else {
            ViewTools.setVectorDrawableTop(
                bindingButtons.buttonEpisodeWatched,
                R.drawable.ic_watch_black_24dp
            )
        }
        bindingButtons.buttonEpisodeWatched.setOnClickListener { onToggleWatched() }
        val plays = episode.plays
        bindingButtons.buttonEpisodeWatched.text =
            TextTools.getWatchedButtonText(requireContext(), isWatched, plays)
        TooltipCompat.setTooltipText(
            bindingButtons.buttonEpisodeWatched,
            bindingButtons.buttonEpisodeWatched.context.getString(
                if (isWatched) R.string.action_unwatched else R.string.action_watched
            )
        )

        // collected button
        collected = episode.collected
        if (collected) {
            ViewTools.setVectorDrawableTop(
                bindingButtons.buttonEpisodeCollected,
                R.drawable.ic_collected_24dp
            )
        } else {
            ViewTools.setVectorDrawableTop(
                bindingButtons.buttonEpisodeCollected,
                R.drawable.ic_collect_black_24dp
            )
        }
        bindingButtons.buttonEpisodeCollected.setOnClickListener { onToggleCollected() }
        bindingButtons.buttonEpisodeCollected.setText(
            if (collected) R.string.state_in_collection else R.string.action_collection_add
        )
        TooltipCompat.setTooltipText(
            bindingButtons.buttonEpisodeCollected,
            bindingButtons.buttonEpisodeCollected.context.getString(
                if (collected) R.string.action_collection_remove else R.string.action_collection_add
            )
        )

        // skip button
        if (isWatched) {
            // if watched do not allow skipping
            bindingButtons.buttonEpisodeSkip.visibility = View.INVISIBLE
        } else {
            bindingButtons.buttonEpisodeSkip.visibility = View.VISIBLE

            val isSkipped = EpisodeTools.isSkipped(episodeFlag)
            if (isSkipped) {
                ViewTools.setVectorDrawableTop(
                    bindingButtons.buttonEpisodeSkip,
                    R.drawable.ic_skipped_24dp
                )
            } else {
                ViewTools.setVectorDrawableTop(
                    bindingButtons.buttonEpisodeSkip,
                    R.drawable.ic_skip_black_24dp
                )
            }
            bindingButtons.buttonEpisodeSkip.setOnClickListener { onToggleSkipped() }
            bindingButtons.buttonEpisodeSkip.setText(
                if (isSkipped) R.string.state_skipped else R.string.action_skip
            )
            TooltipCompat.setTooltipText(
                bindingButtons.buttonEpisodeSkip,
                bindingButtons.buttonEpisodeSkip.context.getString(
                    if (isSkipped) R.string.action_dont_skip else R.string.action_skip
                )
            )
        }
    }

    private fun updateSecondaryButtons(episode: SgEpisode2, show: SgShow2) {
        val bindingBottom = bindingBottom ?: return

        // Trakt
        if (episode.tmdbId != null) {
            val traktLink = TraktTools.buildEpisodeUrl(episode.tmdbId)
            ViewTools.openUriOnClick(bindingBottom.buttonEpisodeTrakt, traktLink)
            bindingBottom.buttonEpisodeTrakt.copyTextToClipboardOnLongClick(traktLink)
        }

        // IMDb
        ViewTools.configureImdbButton(
            bindingBottom.buttonEpisodeImdb,
            lifecycleScope, requireContext(),
            show, episode
        )

        // TMDb
        if (show.tmdbId != null) {
            val url = TmdbTools
                .buildEpisodeUrl(show.tmdbId, episode.season, episode.number)
            ViewTools.openUriOnClick(bindingBottom.buttonEpisodeTmdb, url)
            bindingBottom.buttonEpisodeTmdb.copyTextToClipboardOnLongClick(url)
        }

        // Trakt comments
        bindingBottom.buttonEpisodeComments.setOnClickListener { v: View? ->
            val intent = intentEpisode(requireContext(), episodeTitle, episodeId)
            Utils.startActivityWithAnimation(requireActivity(), intent, v)
        }
    }

    private fun loadTraktRatings() {
        val ratingFetchJob = ratingFetchJob
        if (ratingFetchJob == null || !ratingFetchJob.isActive) {
            this.ratingFetchJob = TraktRatingsFetcher.fetchEpisodeRatingsAsync(
                requireContext(), episodeId
            )
        }
    }

    private fun rateEpisode() {
        RateDialogFragment.newInstanceEpisode(episodeId)
            .safeShow(context, parentFragmentManager)
    }

    private fun shareEpisode() {
        if (episodeTitle == null) {
            return
        }
        val showOrNull = show
        val episodeOrNull = episode
        if (showOrNull?.tmdbId != null && episodeOrNull != null) {
            ShareUtils.shareEpisode(
                requireActivity(), showOrNull.tmdbId,
                episodeOrNull.season, episodeOrNull.number, showOrNull.title,
                episodeTitle
            )
        }
    }

    private fun loadImage(imagePath: String?, hideDetails: Boolean) {
        val binding = binding ?: return

        // immediately hide container if there is no image
        if (imagePath.isNullOrEmpty()) {
            binding.containerImage.visibility = View.GONE
            return
        }

        if (hideDetails) {
            // show image placeholder
            binding.imageviewScreenshot.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.imageviewScreenshot.setImageResource(R.drawable.ic_photo_gray_24dp)
        } else {
            // try loading image
            binding.containerImage.visibility = View.VISIBLE
            ServiceUtils.loadWithPicasso(
                requireContext(),
                tmdbOrTvdbStillUrl(imagePath, requireContext(), false)
            )
                .error(R.drawable.ic_photo_gray_24dp)
                .into(
                    binding.imageviewScreenshot,
                    object : Callback {
                        override fun onSuccess() {
                            binding.imageviewScreenshot.scaleType =
                                ImageView.ScaleType.CENTER_CROP
                        }

                        override fun onError(e: Exception) {
                            binding.imageviewScreenshot.scaleType =
                                ImageView.ScaleType.CENTER_INSIDE
                        }
                    }
                )
        }
    }

    private val actionsLoaderCallbacks: LoaderManager.LoaderCallbacks<MutableList<Action>> =
        object : LoaderManager.LoaderCallbacks<MutableList<Action>> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<MutableList<Action>> {
                val episodeId = args!!.getLong(KEY_EPISODE_ID)
                return EpisodeActionsLoader(requireContext(), episodeId)
            }

            override fun onLoadFinished(
                loader: Loader<MutableList<Action>>,
                data: MutableList<Action>?
            ) {
                if (!isAdded) {
                    return
                }
                if (data == null) {
                    Timber.e(
                        "onLoadFinished: did not receive valid actions for %s",
                        episodeId
                    )
                } else {
                    Timber.d(
                        "onLoadFinished: received %s actions for %s", data.size,
                        episodeId
                    )
                }
                bindingActions?.let {
                    ActionsHelper.populateActions(
                        requireActivity().layoutInflater,
                        requireActivity().theme, it.containerEpisodeActions,
                        data
                    )
                }
            }

            override fun onLoaderReset(loader: Loader<MutableList<Action>>) {
                // do nothing, we are not holding onto the actions list
            }
        }

    override fun loadEpisodeActions() {
        val args = Bundle()
        args.putLong(KEY_EPISODE_ID, episodeId)
        LoaderManager.getInstance(this).restartLoader(
            EpisodesActivity.ACTIONS_LOADER_ID, args,
            actionsLoaderCallbacks
        )
    }

    private var actionsRunnable = Runnable { loadEpisodeActions() }

    override fun loadEpisodeActionsDelayed() {
        handler.removeCallbacks(actionsRunnable)
        handler.postDelayed(
            actionsRunnable,
            EpisodeActionsContract.ACTION_LOADER_DELAY_MILLIS.toLong()
        )
    }

    companion object {

        private const val ARG_LONG_EPISODE_ID = "episode_id"
        private const val KEY_EPISODE_ID = "episode_id"

        fun newInstance(episodeId: Long): EpisodeDetailsFragment {
            val f = EpisodeDetailsFragment()
            val args = Bundle()
            args.putLong(ARG_LONG_EPISODE_ID, episodeId)
            f.arguments = args
            return f
        }
    }
}