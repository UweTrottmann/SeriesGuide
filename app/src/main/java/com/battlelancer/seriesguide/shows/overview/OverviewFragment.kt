// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2024 Uwe Trottmann
// Copyright 2013 Andrew Neal

package com.battlelancer.seriesguide.shows.overview

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.comments.TraktCommentsActivity
import com.battlelancer.seriesguide.databinding.FragmentOverviewBinding
import com.battlelancer.seriesguide.extensions.ActionsHelper
import com.battlelancer.seriesguide.extensions.EpisodeActionsContract
import com.battlelancer.seriesguide.extensions.EpisodeActionsLoader
import com.battlelancer.seriesguide.extensions.ExtensionManager.EpisodeActionReceivedEvent
import com.battlelancer.seriesguide.preferences.MoreOptionsActivity
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.settings.AppSettings.setAskedForFeedback
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.DisplaySettings.isDisplayExactDate
import com.battlelancer.seriesguide.shows.RemoveShowDialogFragment
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.shows.episodes.EpisodesActivity
import com.battlelancer.seriesguide.shows.overview.OverviewActivityImpl.OverviewLayoutType.SINGLE_PANE
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsActivity
import com.battlelancer.seriesguide.shows.tools.ShowStatus
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.streaming.StreamingSearch.initButtons
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktRatingsFetcher.fetchEpisodeRatingsAsync
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceActiveEvent
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceCompletedEvent
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ImageTools.tmdbOrTvdbStillUrl
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.RatingsTools.initialize
import com.battlelancer.seriesguide.util.RatingsTools.setLink
import com.battlelancer.seriesguide.util.RatingsTools.setValuesFor
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.WebTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.battlelancer.seriesguide.util.safeShow
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays general information about a show and, if there is one, the next episode to watch.
 */
class OverviewFragment() : Fragment(), EpisodeActionsContract {

    constructor(showRowId: Long) : this() {
        arguments = buildArgs(showRowId)
    }

    private var binding: FragmentOverviewBinding? = null

    /** Inflated on demand from ViewStub. */
    private var feedbackView: FeedbackView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var ratingFetchJob: Job? = null
    private val model: OverviewViewModel by viewModels {
        OverviewViewModelFactory(showId, requireActivity().application)
    }

    private var showId: Long = 0

    private var hasSetEpisodeWatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showId = requireArguments().getLong(ARG_LONG_SHOW_ROWID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentOverviewBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding!!
        with(binding) {
            // On wide pane layouts this fragment is wrapped in a card which itself is inset,
            // so only inset on single pane layout.
            val overviewLayoutType = OverviewActivityImpl.getLayoutType(requireContext())
            if (overviewLayoutType == SINGLE_PANE) {
                ThemeUtils.applyBottomPaddingForNavigationBar(scrollViewOverview)
            }

            containerOverviewEpisode.visibility = View.GONE
            containerOverviewEmpty.visibility = View.GONE

            buttonOverviewEditReleaseTime.apply {
                contentDescription = getString(R.string.custom_release_time_edit)
                TooltipCompat.setTooltipText(this, contentDescription)
                setOnClickListener {
                    CustomReleaseTimeDialogFragment(showId).safeShow(
                        parentFragmentManager,
                        "custom-release-time"
                    )
                }
            }
            buttonOverviewFavoriteShow.setOnClickListener { onButtonFavoriteClick() }

            containerOverviewEpisodeCard.setOnClickListener { v: View? ->
                runIfHasEpisode { episode ->
                    // display episode details
                    val intent = EpisodesActivity.intentEpisode(episode.id, requireContext())
                    Utils.startActivityWithAnimation(activity, intent, v)
                }
            }

            // Empty view buttons.
            buttonOverviewSimilarShows.setOnClickListener {
                val show = model.show.value
                if (show?.tmdbId != null) {
                    startActivity(
                        SimilarShowsActivity.intent(
                            requireContext(),
                            show.tmdbId,
                            show.title
                        )
                    )
                }
            }
            buttonOverviewRemoveShow.setOnClickListener {
                RemoveShowDialogFragment.show(showId, parentFragmentManager, requireContext())
            }

            // episode buttons
            with(includeButtons) {
                buttonEpisodeWatchedUpTo.visibility = View.GONE // Unused in this fragment.
                buttonEpisodeCheckin.setOnClickListener { onButtonCheckInClick() }
                buttonEpisodeWatched.setOnClickListener { onButtonWatchedClick() }
                buttonEpisodeCollected.setOnClickListener { onButtonCollectedClick() }
                buttonEpisodeSkip.setOnClickListener { onButtonSkipClicked() }
                buttonEpisodeShare.setOnClickListener { shareEpisode() }
                buttonEpisodeCalendar.setOnClickListener { createCalendarEvent() }
                buttonEpisodeComments.setOnClickListener {
                    onButtonCommentsClick(buttonEpisodeComments)
                }
                initButtons(
                    buttonEpisodeStreamingSearch, buttonEpisodeStreamingSearchInfo,
                    parentFragmentManager
                )

                TooltipCompat.setTooltipText(
                    buttonEpisodeCheckin,
                    buttonEpisodeCheckin.contentDescription
                )
                TooltipCompat.setTooltipText(
                    buttonEpisodeWatched,
                    buttonEpisodeWatched.contentDescription
                )
                TooltipCompat.setTooltipText(
                    buttonEpisodeSkip,
                    buttonEpisodeSkip.contentDescription
                )
            }

            // Ratings
            includeRatings.initialize { onButtonRateClick() }

            // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
            textViewEpisodeDescription.copyTextToClipboardOnLongClick()
            textGuestStars.copyTextToClipboardOnLongClick()
            textDvdNumber.copyTextToClipboardOnLongClick()

            // Hide show info if show fragment is visible due to multi-pane layout.
            val isDisplayShowInfo = overviewLayoutType == SINGLE_PANE
            containerOverviewShow.visibility = if (isDisplayShowInfo) View.VISIBLE else View.GONE
        }

        model.show.observe(viewLifecycleOwner) { sgShow2: SgShow2? ->
            if (sgShow2 == null) {
                Timber.e("Failed to load show %s", showId)
                requireActivity().finish()
                return@observe
            }
            this.binding?.also { populateShowViews(it, sgShow2) }
            val episodeId = if (sgShow2.nextEpisode != null && sgShow2.nextEpisode.isNotEmpty()) {
                sgShow2.nextEpisode.toLong()
            } else -1
            model.setEpisodeId(episodeId)
            model.setShowTmdbId(sgShow2.tmdbId)
        }
        model.episode.observe(viewLifecycleOwner) { sgEpisode2: SgEpisode2? ->
            this.binding?.also {
                maybeAddFeedbackView(it)
                // May be null if there is no next episode.
                updateEpisodeViews(it, sgEpisode2)
            }
        }
        model.watchProvider.observe(viewLifecycleOwner) { watchInfo: TmdbTools2.WatchInfo? ->
            if (watchInfo != null) {
                this.binding?.let {
                    StreamingSearch.configureButton(
                        it.includeButtons.buttonEpisodeStreamingSearch,
                        watchInfo, replaceButtonText = true
                    )
                }
            }
        }
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
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the fragment from
        // being garbage collected. It also prevents our callback from getting invoked even after the
        // fragment is destroyed.
        Picasso.get().cancelRequest(binding!!.imageViewOverviewEpisode)

        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(episodeActionsRunnable)
        // Release reference to any job.
        ratingFetchJob = null
    }

    private fun createCalendarEvent() {
        val currentShow = model.show.value
        val currentEpisode = model.episode.value
        if (currentShow == null || currentEpisode == null) {
            return
        }
        // add calendar event
        ShareUtils.suggestCalendarEvent(
            activity,
            currentShow.title,
            TextTools.getNextEpisodeString(
                requireContext(), currentEpisode.season,
                currentEpisode.number, currentEpisode.title
            ),
            currentEpisode.firstReleasedMs,
            currentShow.runtime
        )
    }

    private fun onButtonFavoriteClick() {
        val currentShow = model.show.value ?: return
        SgApp.getServicesComponent(requireContext()).showTools()
            .storeIsFavorite(showId, !currentShow.favorite)
    }

    private fun onButtonCheckInClick() {
        runIfHasEpisode { episode ->
            CheckInDialogFragment
                .show(requireContext(), parentFragmentManager, episode.id)
        }
    }

    private fun onButtonWatchedClick() {
        hasSetEpisodeWatched = true
        changeEpisodeFlag(EpisodeFlags.WATCHED)
    }

    private fun onButtonCollectedClick() {
        runIfHasEpisode { episode ->
            EpisodeTools.episodeCollected(requireContext(), episode.id, !episode.collected)
        }
    }

    private fun onButtonSkipClicked() {
        changeEpisodeFlag(EpisodeFlags.SKIPPED)
    }

    private fun changeEpisodeFlag(episodeFlag: Int) {
        runIfHasEpisode { episode ->
            EpisodeTools.episodeWatched(requireContext(), episode.id, episodeFlag)
        }
    }

    private fun onButtonRateClick() {
        runIfHasEpisode { episode ->
            RateDialogFragment.newInstanceEpisode(episode.id, episode.ratingUser)
                .safeShow(requireContext(), parentFragmentManager)
        }
    }

    private fun onButtonCommentsClick(v: View?) {
        runIfHasEpisode { episode ->
            val i = TraktCommentsActivity.intentEpisode(requireContext(), episode.title, episode.id)
            Utils.startActivityWithAnimation(activity, i, v)
        }
    }

    private fun shareEpisode() {
        val currentShow = model.show.value ?: return
        runIfHasEpisode { episode ->
            if (currentShow.tmdbId != null) {
                ShareUtils.shareEpisode(
                    activity, currentShow.tmdbId, episode.season,
                    episode.number, currentShow.title, episode.title
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onEventMainThread(event: EpisodeActionReceivedEvent) {
        runIfHasEpisode { episode ->
            if (episode.tmdbId == event.episodeTmdbId) {
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
        val binding = binding ?: return
        with(binding.includeButtons) {
            buttonEpisodeWatched.isEnabled = enabled
            buttonEpisodeCollected.isEnabled = enabled
            buttonEpisodeSkip.isEnabled = enabled
            buttonEpisodeCheckin.isEnabled = enabled
        }
    }

    private fun updateEpisodeViews(binding: FragmentOverviewBinding, episode: SgEpisode2?) {
        if (episode != null) {
            // hide check-in if not connected to trakt or hexagon is enabled
            val isConnectedToTrakt = TraktCredentials.get(requireContext()).hasCredentials()
            val displayCheckIn = (isConnectedToTrakt
                    && !HexagonSettings.isEnabled(requireContext()))
            binding.includeButtons.buttonEpisodeCheckin.visibility =
                if (displayCheckIn) View.VISIBLE else View.GONE
            binding.includeButtons.buttonEpisodeStreamingSearch.nextFocusUpId =
                if (displayCheckIn) R.id.buttonCheckIn else R.id.buttonEpisodeWatched

            // populate episode details
            populateEpisodeViews(binding, episode)
            populateEpisodeDescriptionAndLinkButtons(binding)

            // load full info and ratings, image, actions
            loadEpisodeDetails()
            loadEpisodeImage(
                binding.imageViewOverviewEpisode,
                binding.textViewOverviewEpisodeDetailsHidden,
                episode.image
            )
            loadEpisodeActionsDelayed()

            binding.containerOverviewEmpty.visibility = View.GONE
            binding.containerOverviewEpisodeCard.visibility = View.VISIBLE
            binding.containerOverviewEpisodeDetails.visibility = View.VISIBLE
        } else {
            // No next episode: display empty view with suggestion on what to do.
            binding.textViewOverviewNotMigrated.visibility = View.GONE
            binding.containerOverviewEmpty.visibility = View.VISIBLE
            binding.containerOverviewEpisodeCard.visibility = View.GONE
            binding.containerOverviewEpisodeDetails.visibility = View.GONE
        }

        // animate view into visibility
        if (binding.containerOverviewEpisode.visibility == View.GONE) {
            binding.containerOverviewProgress.startAnimation(
                AnimationUtils.loadAnimation(
                    binding.containerOverviewProgress.context,
                    android.R.anim.fade_out
                )
            )
            binding.containerOverviewProgress.visibility = View.GONE
            binding.containerOverviewEpisode.startAnimation(
                AnimationUtils.loadAnimation(
                    binding.containerOverviewEpisode.context,
                    android.R.anim.fade_in
                )
            )
            binding.containerOverviewEpisode.visibility = View.VISIBLE
        }
    }

    private fun populateEpisodeViews(binding: FragmentOverviewBinding, episode: SgEpisode2) {
        ViewTools.configureNotMigratedWarning(
            binding.textViewOverviewNotMigrated,
            episode.tmdbId == null
        )

        // title
        val season = episode.season
        val number = episode.number
        val title = TextTools.getEpisodeTitle(
            requireContext(),
            if (DisplaySettings.preventSpoilers(requireContext())) null else episode.title, number
        )
        binding.episodeTitle.text = title

        // number
        val infoText = StringBuilder()
        if (season == 0) {
            infoText.append(getString(R.string.specialseason))
        } else {
            infoText.append(getString(R.string.season_number, season))
        }
        infoText.append(" ")
        infoText.append(getString(R.string.episode_number, number))
        val episodeAbsoluteNumber = episode.absoluteNumber
        if (episodeAbsoluteNumber != null
            && episodeAbsoluteNumber > 0 && episodeAbsoluteNumber != number) {
            infoText.append(" (").append(episodeAbsoluteNumber).append(")")
        }

        // release date
        val releaseTime = episode.firstReleasedMs
        val timeText = if (releaseTime != -1L) {
            val actualRelease = TimeTools.applyUserOffset(requireContext(), releaseTime)
            // "Oct 31 (Fri)" or "in 14 mins (Fri)"
            val dateTime: String = if (isDisplayExactDate(requireContext())) {
                TimeTools.formatToLocalDateShort(requireContext(), actualRelease)
            } else {
                TimeTools.formatToLocalRelativeTime(requireContext(), actualRelease)
            }
            getString(
                R.string.format_date_and_day, dateTime,
                TimeTools.formatToLocalDay(actualRelease)
            )
        } else {
            null
        }

        binding.textOverviewEpisodeInfo.text = TextTools.buildTitleAndSecondary(
            requireContext(),
            infoText.toString(),
            R.style.TextAppearance_SeriesGuide_Caption,
            timeText,
            R.style.TextAppearance_SeriesGuide_Caption_Dim
        )

        // watched button
        binding.includeButtons.buttonEpisodeWatched.also {
            val isWatched = EpisodeTools.isWatched(episode.watched)
            if (isWatched) {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_watched_24dp)
            } else {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_watch_black_24dp)
            }
            val plays = episode.plays
            it.text = TextTools.getWatchedButtonText(requireContext(), isWatched, plays)
        }

        // collected button
        binding.includeButtons.buttonEpisodeCollected.also {
            val isCollected = episode.collected
            if (isCollected) {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_collected_24dp)
            } else {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_collect_black_24dp)
            }
            it.setText(
                if (isCollected) R.string.state_in_collection else R.string.action_collection_add
            )
            TooltipCompat.setTooltipText(
                it,
                it.context.getString(
                    if (isCollected) R.string.action_collection_remove else R.string.action_collection_add
                )
            )
        }

        // skip button: hide if watched (may happen when rewatching) to avoid removing plays
        // use invisible to avoid buttons from moving positions
        binding.includeButtons.buttonEpisodeSkip.isInvisible =
            EpisodeTools.isWatched(episode.watched)

        // dvd number
        ViewTools.setLabelValueOrHide(
            binding.labelDvdNumber, binding.textDvdNumber, episode.dvdNumber
        )
        // guest stars
        ViewTools.setLabelValueOrHide(
            binding.labelGuestStars,
            binding.textGuestStars,
            TextTools.splitPipeSeparatedStrings(episode.guestStars)
        )

        // Ratings
        binding.includeRatings.setValuesFor(episode)
    }

    /**
     * Updates the episode description and link buttons. Need both show and episode data loaded.
     */
    private fun populateEpisodeDescriptionAndLinkButtons(binding: FragmentOverviewBinding) {
        val show = model.show.value
        val episode = model.episode.value
        if (show == null || episode == null) {
            // no show or episode data available
            return
        }
        var overview = episode.overview
        val languageCode = show.language?.let { LanguageTools.mapLegacyShowCode(it) }
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            overview = TextTools.textNoTranslation(requireContext(), languageCode)
        } else if (DisplaySettings.preventSpoilers(requireContext())) {
            overview = getString(R.string.no_spoilers)
        }
        binding.textViewEpisodeDescription.text = TextTools.textWithTmdbSource(
            binding.textViewEpisodeDescription.context, overview
        )

        // TMDb buttons
        val showTmdbId = show.tmdbId
        if (showTmdbId != null) {
            val tmdbUrl = TmdbTools.buildEpisodeUrl(showTmdbId, episode.season, episode.number)
            binding.includeRatings.ratingViewTmdb.setLink(requireContext(), tmdbUrl)
            ViewTools.openUrlOnClickAndCopyOnLongPress(
                binding.includeServices.includeMore.buttonEpisodeTmdb,
                tmdbUrl
            )
        }

        // Trakt buttons
        if (episode.tmdbId != null) {
            val traktUrl = TraktTools.buildEpisodeUrl(episode.tmdbId)
            binding.includeRatings.ratingViewTrakt.setLink(requireContext(), traktUrl)
            ViewTools.openUrlOnClickAndCopyOnLongPress(
                binding.includeServices.includeMore.buttonEpisodeTrakt,
                traktUrl
            )
        }

        // IMDb button
        ServiceUtils.configureImdbButton(
            binding.includeServices.includeMore.buttonEpisodeImdb,
            lifecycleScope, requireContext(),
            model.show.value, episode
        )
    }

    override fun loadEpisodeActions() {
        // do not load actions if there is no episode
        runIfHasEpisode { episode ->
            val args = Bundle()
            args.putLong(ARG_EPISODE_ID, episode.id)
            LoaderManager.getInstance(this)
                .restartLoader(
                    OverviewActivityImpl.OVERVIEW_ACTIONS_LOADER_ID, args,
                    episodeActionsLoaderCallbacks
                )
        }
    }

    private var episodeActionsRunnable = Runnable { loadEpisodeActions() }

    override fun loadEpisodeActionsDelayed() {
        handler.removeCallbacks(episodeActionsRunnable)
        handler.postDelayed(
            episodeActionsRunnable,
            EpisodeActionsContract.ACTION_LOADER_DELAY_MILLIS.toLong()
        )
    }

    private fun loadEpisodeImage(imageView: ImageView, detailsHiddenView: View, imagePath: String?) {
        if (imagePath.isNullOrEmpty()) {
            imageView.setImageDrawable(null)
            detailsHiddenView.isGone = true
            return
        }

        if (DisplaySettings.preventSpoilers(requireContext())) {
            // Display no spoilers info
            imageView.apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setImageDrawable(null)
            }
            detailsHiddenView.isVisible = true
        } else {
            detailsHiddenView.isGone = true
            // Try loading image
            ImageTools.loadWithPicasso(
                requireContext(),
                tmdbOrTvdbStillUrl(imagePath, requireContext(), false)
            )
                .error(R.drawable.ic_photo_gray_24dp)
                .into(imageView,
                    object : Callback {
                        override fun onSuccess() {
                            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        }

                        override fun onError(e: Exception) {
                            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        }
                    }
                )
        }
    }

    private fun loadEpisodeDetails() {
        runIfHasEpisode { episode ->
            val ratingFetchJob = ratingFetchJob
            if (ratingFetchJob == null || !ratingFetchJob.isActive) {
                this.ratingFetchJob = fetchEpisodeRatingsAsync(
                    requireContext(),
                    episode.id
                )
            }
        }
    }

    private fun populateShowViews(binding: FragmentOverviewBinding, show: SgShow2) {
        // set show title in action bar
        val showTitle = show.title
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.title = showTitle
            requireActivity().title = getString(R.string.description_overview) + showTitle
        }

        // status
        ShowStatus.setStatusAndColor(binding.overviewShowStatus, show.statusOrUnknown)

        // favorite
        val isFavorite = show.favorite
        binding.buttonOverviewFavoriteShow.also {
            it.setImageResource(
                if (isFavorite) R.drawable.ic_star_black_24dp else R.drawable.ic_star_border_black_24dp
            )
            it.contentDescription = getString(
                if (isFavorite) R.string.context_unfavorite else R.string.context_favorite
            )
            TooltipCompat.setTooltipText(it, it.contentDescription)
        }

        // Regular network, release time and length.
        val network = show.network
        val timeOrNull = TimeTools.getLocalReleaseDayAndTime(requireContext(), show)
        val runtime = show.runtime?.let { TimeTools.formatToHoursAndMinutes(resources, it) }
        val combinedString =
            TextTools.dotSeparate(TextTools.dotSeparate(network, timeOrNull), runtime)
        binding.overviewShowNetworkAndTime.text = combinedString
        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        binding.overviewShowNetworkAndTime.copyTextToClipboardOnLongClick()

        // Remaining episodes
        binding.textOverviewEpisodeHeader.text = TextTools.buildTitleAndSecondary(
            requireContext(),
            getString(R.string.next_to_watch),
            R.style.TextAppearance_SeriesGuide_Body2_Bold,
            TextTools.getRemainingEpisodes(requireContext().resources, show.unwatchedCount),
            R.style.TextAppearance_SeriesGuide_Body2_Dim
        )

        // episode description might need show language, so update it here as well
        populateEpisodeDescriptionAndLinkButtons(binding)
    }

    private fun runIfHasEpisode(block: (episode: SgEpisode2) -> Unit) {
        val currentEpisode = model.episode.value
        if (currentEpisode != null) {
            block.invoke(currentEpisode)
        }
    }

    private fun maybeAddFeedbackView(binding: FragmentOverviewBinding) {
        if (feedbackView != null
            || !hasSetEpisodeWatched || !AppSettings.shouldAskForFeedback(requireContext())) {
            return  // can or should not add feedback view
        }
        (binding.viewStubOverviewFeedback.inflate() as FeedbackView).also {
            feedbackView = it
            it.setCallback(object : FeedbackView.Callback {
                override fun onRate() {
                    if (WebTools.openInApp(
                            requireContext(),
                            getString(R.string.url_store_page)
                        )) {
                        hideFeedbackView()
                    }
                }

                override fun onFeedback() {
                    if (Utils.tryStartActivity(
                            requireContext(),
                            MoreOptionsActivity.getFeedbackEmailIntent(requireContext()),
                            true
                        )) {
                        hideFeedbackView()
                    }
                }

                override fun onDismiss() {
                    hideFeedbackView()
                }
            })
        }
    }

    private fun hideFeedbackView() {
        feedbackView?.visibility = View.GONE
        setAskedForFeedback(requireContext())
    }

    private val episodeActionsLoaderCallbacks: LoaderManager.LoaderCallbacks<MutableList<Action>?> =
        object : LoaderManager.LoaderCallbacks<MutableList<Action>?> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<MutableList<Action>?> {
                val episodeId = args!!.getLong(ARG_EPISODE_ID)
                return EpisodeActionsLoader(requireContext(), episodeId)
            }

            override fun onLoadFinished(
                loader: Loader<MutableList<Action>?>,
                data: MutableList<Action>?
            ) {
                val binding = binding ?: return
                if (data == null) {
                    Timber.e("onLoadFinished: did not receive valid actions")
                } else {
                    Timber.d("onLoadFinished: received %s actions", data.size)
                }
                ActionsHelper.populateActions(
                    requireActivity().layoutInflater,
                    requireActivity().theme, binding.includeServices.containerEpisodeActions, data
                )
            }

            override fun onLoaderReset(loader: Loader<MutableList<Action>?>) {
                val binding = binding ?: return
                ActionsHelper.populateActions(
                    requireActivity().layoutInflater,
                    requireActivity().theme, binding.includeServices.containerEpisodeActions, null
                )
            }
        }

    companion object {
        val liftOnScrollTargetViewId = R.id.scrollViewOverview

        private const val ARG_LONG_SHOW_ROWID = "show_id"
        private const val ARG_EPISODE_ID = "episode_id"

        fun buildArgs(showRowId: Long): Bundle = bundleOf(
            ARG_LONG_SHOW_ROWID to showRowId
        )

    }
}