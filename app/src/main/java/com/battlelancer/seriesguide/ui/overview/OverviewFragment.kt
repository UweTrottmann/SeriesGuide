package com.battlelancer.seriesguide.ui.overview

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.extensions.ActionsHelper
import com.battlelancer.seriesguide.extensions.EpisodeActionsContract
import com.battlelancer.seriesguide.extensions.EpisodeActionsLoader
import com.battlelancer.seriesguide.extensions.ExtensionManager.EpisodeActionReceivedEvent
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.settings.AppSettings.setAskedForFeedback
import com.battlelancer.seriesguide.settings.DisplaySettings.isDisplayExactDate
import com.battlelancer.seriesguide.settings.DisplaySettings.preventSpoilers
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.streaming.StreamingSearch.initButtons
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktRatingsFetcher.fetchEpisodeRatingsAsync
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceActiveEvent
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceCompletedEvent
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.comments.TraktCommentsActivity.Companion.intentEpisode
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity.Companion.intentEpisode
import com.battlelancer.seriesguide.ui.preferences.MoreOptionsActivity.Companion.getFeedbackEmailIntent
import com.battlelancer.seriesguide.ui.search.SimilarShowsActivity.Companion.intent
import com.battlelancer.seriesguide.ui.shows.RemoveShowDialogFragment.Companion.show
import com.battlelancer.seriesguide.util.ImageTools.tmdbOrTvdbStillUrl
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.TmdbTools
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.battlelancer.seriesguide.widgets.FeedbackView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays general information about a show and its next episode.
 */
@SuppressLint("NonConstantResourceId")
class OverviewFragment : Fragment(), EpisodeActionsContract {

    @BindView(R.id.viewStubOverviewFeedback)
    @JvmField
    var feedbackViewStub: ViewStub? = null

    @BindView(R.id.feedbackViewOverview)
    @JvmField
    var feedbackView: FeedbackView? = null

    @BindView(R.id.containerOverviewShow)
    lateinit var containerShow: View

    @BindView(R.id.imageButtonFavorite)
    lateinit var buttonFavorite: ImageButton

    @BindView(R.id.progress_container)
    lateinit var containerProgress: View

    @BindView(R.id.containerOverviewEpisode)
    lateinit var containerEpisode: View

    @BindView(R.id.episode_empty_container)
    lateinit var containerEpisodeEmpty: View

    @BindView(R.id.buttonOverviewSimilarShows)
    lateinit var buttonSimilarShows: Button

    @BindView(R.id.buttonOverviewRemoveShow)
    lateinit var buttonRemoveShow: Button

    @BindView(R.id.textViewOverviewNotMigrated)
    lateinit var textViewOverviewNotMigrated: View

    @BindView(R.id.episode_primary_container)
    lateinit var containerEpisodePrimary: View

    @BindView(R.id.dividerHorizontalOverviewEpisodeMeta)
    lateinit var dividerEpisodeMeta: View

    @BindView(R.id.imageViewOverviewEpisode)
    lateinit var imageEpisode: ImageView

    @BindView(R.id.episodeTitle)
    lateinit var textEpisodeTitle: TextView

    @BindView(R.id.episodeTime)
    lateinit var textEpisodeTime: TextView

    @BindView(R.id.episodeInfo)
    lateinit var textEpisodeNumbers: TextView

    @BindView(R.id.episode_meta_container)
    lateinit var containerEpisodeMeta: View

    @BindView(R.id.containerRatings)
    lateinit var containerRatings: View

    @BindView(R.id.dividerEpisodeButtons)
    lateinit var dividerEpisodeButtons: View

    @BindView(R.id.buttonEpisodeCheckin)
    lateinit var buttonCheckin: Button

    @BindView(R.id.buttonEpisodeWatchedUpTo)
    lateinit var buttonWatchedUpTo: Button

    @BindView(R.id.containerEpisodeStreamingSearch)
    lateinit var containerEpisodeStreamingSearch: ViewGroup

    @BindView(R.id.buttonEpisodeStreamingSearch)
    lateinit var buttonStreamingSearch: Button

    @BindView(R.id.buttonEpisodeStreamingSearchInfo)
    lateinit var buttonEpisodeStreamingSearchInfo: ImageButton

    @BindView(R.id.buttonEpisodeWatched)
    lateinit var buttonWatch: Button

    @BindView(R.id.buttonEpisodeCollected)
    lateinit var buttonCollect: Button

    @BindView(R.id.buttonEpisodeSkip)
    lateinit var buttonSkip: Button

    @BindView(R.id.TextViewEpisodeDescription)
    lateinit var textDescription: TextView

    @BindView(R.id.labelDvd)
    lateinit var labelDvdNumber: View

    @BindView(R.id.textViewEpisodeDVDnumber)
    lateinit var textDvdNumber: TextView

    @BindView(R.id.labelGuestStars)
    lateinit var labelGuestStars: View

    @BindView(R.id.TextViewEpisodeGuestStars)
    lateinit var textGuestStars: TextView

    @BindView(R.id.textViewRatingsValue)
    lateinit var textRating: TextView

    @BindView(R.id.textViewRatingsRange)
    lateinit var textRatingRange: TextView

    @BindView(R.id.textViewRatingsVotes)
    lateinit var textRatingVotes: TextView

    @BindView(R.id.textViewRatingsUser)
    lateinit var textUserRating: TextView

    @BindView(R.id.buttonEpisodeImdb)
    lateinit var buttonImdb: Button

    @BindView(R.id.buttonEpisodeTmdb)
    lateinit var buttonTmdb: Button

    @BindView(R.id.buttonEpisodeTrakt)
    lateinit var buttonTrakt: Button

    @BindView(R.id.buttonEpisodeShare)
    lateinit var buttonShare: Button

    @BindView(R.id.buttonEpisodeCalendar)
    lateinit var buttonAddToCalendar: Button

    @BindView(R.id.buttonEpisodeComments)
    lateinit var buttonComments: Button

    @BindView(R.id.containerEpisodeActions)
    @JvmField
    var containerActions: LinearLayout? = null

    private val handler = Handler(Looper.getMainLooper())
    private var ratingFetchJob: Job? = null
    private lateinit var unbinder: Unbinder
    private val model: OverviewViewModel by viewModels {
        OverviewViewModelFactory(showId, requireActivity().application)
    }

    private var showId: Long = 0
    private var show: SgShow2? = null
    private var episode: SgEpisode2? = null

    private var hasSetEpisodeWatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showId = requireArguments().getLong(ARG_LONG_SHOW_ROWID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        unbinder = ButterKnife.bind(this, view)

        containerEpisode.visibility = View.GONE
        containerEpisodeEmpty.visibility = View.GONE

        containerEpisodePrimary.setOnClickListener { v: View? ->
            runIfHasEpisode { episode ->
                // display episode details
                val intent = intentEpisode(episode.id, requireContext())
                Utils.startActivityWithAnimation(activity, intent, v)
            }
        }

        // Empty view buttons.
        buttonSimilarShows.setOnClickListener {
            val show = show
            if (show?.tmdbId != null) {
                startActivity(intent(requireContext(), show.tmdbId, show.title))
            }
        }
        buttonRemoveShow.setOnClickListener {
            show(showId, parentFragmentManager, requireContext())
        }

        // episode buttons
        buttonWatchedUpTo.visibility = View.GONE // Unused.
        TooltipCompat.setTooltipText(buttonCheckin, buttonCheckin.contentDescription)
        TooltipCompat.setTooltipText(buttonWatch, buttonWatch.contentDescription)
        TooltipCompat.setTooltipText(buttonSkip, buttonSkip.contentDescription)
        initButtons(
            buttonStreamingSearch, buttonEpisodeStreamingSearchInfo,
            parentFragmentManager
        )

        // ratings
        TooltipCompat.setTooltipText(
            containerRatings,
            containerRatings.context.getString(R.string.action_rate)
        )
        textRatingRange.text = getString(R.string.format_rating_range, 10)
        buttonShare.setOnClickListener { shareEpisode() }
        buttonAddToCalendar.setOnClickListener { createCalendarEvent() }

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        textDescription.copyTextToClipboardOnLongClick()
        textGuestStars.copyTextToClipboardOnLongClick()
        textDvdNumber.copyTextToClipboardOnLongClick()

        // Hide show info if show fragment is visible due to multi-pane layout.
        val isDisplayShowInfo = resources.getBoolean(R.bool.isOverviewSinglePane)
        containerShow.visibility = if (isDisplayShowInfo) View.VISIBLE else View.GONE

        model.show.observe(viewLifecycleOwner, { sgShow2: SgShow2? ->
            if (sgShow2 == null) {
                Timber.e("Failed to load show %s", showId)
                requireActivity().finish()
                return@observe
            }
            show = sgShow2
            populateShowViews(sgShow2)
            val episodeId = if (sgShow2.nextEpisode != null && sgShow2.nextEpisode.isNotEmpty()) {
                sgShow2.nextEpisode.toLong()
            } else -1
            model.setEpisodeId(episodeId)
            model.setShowTmdbId(sgShow2.tmdbId)
        })
        model.episode.observe(viewLifecycleOwner, { sgEpisode2: SgEpisode2? ->
            // May be null if there is no next episode.
            episode = sgEpisode2
            maybeAddFeedbackView()
            updateEpisodeViews(sgEpisode2)
        })
        model.watchProvider.observe(viewLifecycleOwner, { watchInfo: TmdbTools2.WatchInfo? ->
            if (watchInfo != null) {
                StreamingSearch.configureButton(buttonStreamingSearch, watchInfo, true)
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
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the fragment from
        // being garbage collected. It also prevents our callback from getting invoked even after the
        // fragment is destroyed.
        Picasso.get().cancelRequest(imageEpisode)

        unbinder.unbind()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(episodeActionsRunnable)
        // Release reference to any job.
        ratingFetchJob = null
    }

    private fun createCalendarEvent() {
        val currentShow = show
        val currentEpisode = episode
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

    @OnClick(R.id.imageButtonFavorite)
    fun onButtonFavoriteClick(view: View) {
        if (view.tag == null) {
            return
        }

        // store new value
        val isFavorite = view.tag as Boolean
        getServicesComponent(requireContext()).showTools()
            .storeIsFavorite(showId, !isFavorite)
    }

    @OnClick(R.id.buttonEpisodeCheckin)
    fun onButtonCheckInClick() {
        runIfHasEpisode { episode ->
            CheckInDialogFragment
                .show(requireContext(), parentFragmentManager, episode.id)
        }
    }

    @OnClick(R.id.buttonEpisodeWatched)
    fun onButtonWatchedClick() {
        hasSetEpisodeWatched = true
        changeEpisodeFlag(EpisodeFlags.WATCHED)
    }

    @OnClick(R.id.buttonEpisodeCollected)
    fun onButtonCollectedClick() {
        runIfHasEpisode { episode ->
            EpisodeTools.episodeCollected(context, episode.id, !episode.collected)
        }
    }

    @OnClick(R.id.buttonEpisodeSkip)
    fun onButtonSkipClicked() {
        changeEpisodeFlag(EpisodeFlags.SKIPPED)
    }

    private fun changeEpisodeFlag(episodeFlag: Int) {
        runIfHasEpisode { episode ->
            EpisodeTools
                .episodeWatched(context, episode.id, episodeFlag)
        }
    }

    @OnClick(R.id.containerRatings)
    fun onButtonRateClick() {
        runIfHasEpisode { episode ->
            RateDialogFragment.newInstanceEpisode(episode.id)
                .safeShow(context, parentFragmentManager)
        }
    }

    @OnClick(R.id.buttonEpisodeComments)
    fun onButtonCommentsClick(v: View?) {
        runIfHasEpisode { episode ->
            val i = intentEpisode(requireContext(), episode.title, episode.id)
            Utils.startActivityWithAnimation(activity, i, v)
        }
    }

    private fun shareEpisode() {
        val currentShow = show ?: return
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
        if (view == null) {
            return
        }
        buttonWatch.isEnabled = enabled
        buttonCollect.isEnabled = enabled
        buttonSkip.isEnabled = enabled
        buttonCheckin.isEnabled = enabled
    }

    private fun updateEpisodeViews(episode: SgEpisode2?) {
        if (episode != null) {
            // hide check-in if not connected to trakt or hexagon is enabled
            val isConnectedToTrakt = TraktCredentials.get(requireContext()).hasCredentials()
            val displayCheckIn = (isConnectedToTrakt
                    && !HexagonSettings.isEnabled(requireContext()))
            buttonCheckin.visibility = if (displayCheckIn) View.VISIBLE else View.GONE
            buttonStreamingSearch.nextFocusUpId =
                if (displayCheckIn) R.id.buttonCheckIn else R.id.buttonEpisodeWatched

            // populate episode details
            populateEpisodeViews(episode)
            populateEpisodeDescriptionAndTvdbButton()

            // load full info and ratings, image, actions
            loadEpisodeDetails()
            loadEpisodeImage(episode.image)
            loadEpisodeActionsDelayed()

            containerEpisodeEmpty.visibility = View.GONE
            containerEpisodePrimary.visibility = View.VISIBLE
            containerEpisodeMeta.visibility = View.VISIBLE
        } else {
            // No next episode: display empty view with suggestion on what to do.
            textViewOverviewNotMigrated.visibility = View.GONE
            containerEpisodeEmpty.visibility = View.VISIBLE
            containerEpisodePrimary.visibility = View.GONE
            containerEpisodeMeta.visibility = View.GONE
        }

        // animate view into visibility
        if (containerEpisode.visibility == View.GONE) {
            containerProgress.startAnimation(
                AnimationUtils.loadAnimation(containerProgress.context, android.R.anim.fade_out)
            )
            containerProgress.visibility = View.GONE
            containerEpisode.startAnimation(
                AnimationUtils.loadAnimation(containerEpisode.context, android.R.anim.fade_in)
            )
            containerEpisode.visibility = View.VISIBLE
        }
    }

    private fun populateEpisodeViews(episode: SgEpisode2) {
        ViewTools.configureNotMigratedWarning(
            textViewOverviewNotMigrated,
            episode.tmdbId == null
        )

        // title
        val season = episode.season
        val number = episode.number
        val title = TextTools.getEpisodeTitle(
            requireContext(),
            if (preventSpoilers(requireContext())) null else episode.title, number
        )
        textEpisodeTitle.text = title

        // number
        val infoText = StringBuilder()
        infoText.append(getString(R.string.season_number, season))
        infoText.append(" ")
        infoText.append(getString(R.string.episode_number, number))
        val episodeAbsoluteNumber = episode.absoluteNumber
        if (episodeAbsoluteNumber != null
            && episodeAbsoluteNumber > 0 && episodeAbsoluteNumber != number) {
            infoText.append(" (").append(episodeAbsoluteNumber).append(")")
        }
        textEpisodeNumbers.text = infoText

        // air date
        val releaseTime = episode.firstReleasedMs
        if (releaseTime != -1L) {
            val actualRelease = TimeTools.applyUserOffset(requireContext(), releaseTime)
            // "Oct 31 (Fri)" or "in 14 mins (Fri)"
            val dateTime: String = if (isDisplayExactDate(requireContext())) {
                TimeTools.formatToLocalDateShort(requireContext(), actualRelease)
            } else {
                TimeTools.formatToLocalRelativeTime(context, actualRelease)
            }
            textEpisodeTime.text = getString(
                R.string.format_date_and_day, dateTime,
                TimeTools.formatToLocalDay(actualRelease)
            )
        } else {
            textEpisodeTime.text = null
        }

        // watched button
        val isWatched = EpisodeTools.isWatched(episode.watched)
        if (isWatched) {
            ViewTools.setVectorDrawableTop(
                buttonWatch,
                R.drawable.ic_watched_24dp
            )
        } else {
            ViewTools.setVectorDrawableTop(
                buttonWatch,
                R.drawable.ic_watch_black_24dp
            )
        }
        val plays = episode.plays
        buttonWatch.text =
            TextTools.getWatchedButtonText(requireContext(), isWatched, plays)

        // collected button
        val isCollected = episode.collected
        if (isCollected) {
            ViewTools.setVectorDrawableTop(buttonCollect, R.drawable.ic_collected_24dp)
        } else {
            ViewTools.setVectorDrawableTop(buttonCollect, R.drawable.ic_collect_black_24dp)
        }
        buttonCollect.setText(
            if (isCollected) R.string.state_in_collection else R.string.action_collection_add
        )
        TooltipCompat.setTooltipText(
            buttonCollect,
            buttonCollect.context.getString(
                if (isCollected) R.string.action_collection_remove else R.string.action_collection_add
            )
        )

        // dvd number
        var isShowingMeta = ViewTools.setLabelValueOrHide(
            labelDvdNumber, textDvdNumber, episode.dvdNumber
        )
        // guest stars
        isShowingMeta = isShowingMeta or ViewTools.setLabelValueOrHide(
            labelGuestStars, textGuestStars, TextTools.splitPipeSeparatedStrings(episode.guestStars)
        )
        // hide divider if no meta is visible
        dividerEpisodeMeta.visibility = if (isShowingMeta) View.VISIBLE else View.GONE

        // trakt rating
        textRating.text = TraktTools.buildRatingString(episode.ratingGlobal)
        textRatingVotes.text = TraktTools.buildRatingVotesString(
            activity, episode.ratingVotes
        )

        // user rating
        textUserRating.text = TraktTools.buildUserRatingString(
            activity, episode.ratingUser
        )

        // IMDb button
        ViewTools.configureImdbButton(
            buttonImdb,
            lifecycleScope, requireContext(),
            show, episode
        )

        // trakt button
        if (episode.tmdbId != null) {
            val traktLink = TraktTools.buildEpisodeUrl(episode.tmdbId)
            ViewTools.openUriOnClick(buttonTrakt, traktLink)
            buttonTrakt.copyTextToClipboardOnLongClick(traktLink)
        }
    }

    /**
     * Updates the episode description and TVDB button. Need both show and episode data loaded.
     */
    private fun populateEpisodeDescriptionAndTvdbButton() {
        val show = show
        val episode = episode
        if (show == null || episode == null) {
            // no show or episode data available
            return
        }
        var overview = episode.overview
        val languageCode = show.language
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            overview = TextTools.textNoTranslation(requireContext(), languageCode)
        } else if (preventSpoilers(requireContext())) {
            overview = getString(R.string.no_spoilers)
        }
        textDescription.text = TextTools.textWithTmdbSource(
            textDescription.context, overview
        )

        // TMDb button
        val showTmdbId = show.tmdbId
        if (showTmdbId != null) {
            val url = TmdbTools.buildEpisodeUrl(showTmdbId, episode.season, episode.number)
            ViewTools.openUriOnClick(buttonTmdb, url)
            buttonTmdb.copyTextToClipboardOnLongClick(url)
        }
    }

    override fun loadEpisodeActions() {
        // do not load actions if there is no episode
        runIfHasEpisode { episode ->
            val args = Bundle()
            args.putLong(ARG_EPISODE_ID, episode.id)
            LoaderManager.getInstance(this)
                .restartLoader(
                    OverviewActivity.OVERVIEW_ACTIONS_LOADER_ID, args,
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

    private fun loadEpisodeImage(imagePath: String?) {
        if (TextUtils.isEmpty(imagePath)) {
            imageEpisode.setImageDrawable(null)
            return
        }

        if (preventSpoilers(requireContext())) {
            // show image placeholder
            imageEpisode.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageEpisode.setImageResource(R.drawable.ic_photo_gray_24dp)
        } else {
            // try loading image
            ServiceUtils.loadWithPicasso(
                requireContext(),
                tmdbOrTvdbStillUrl(imagePath, requireContext(), false)
            )
                .error(R.drawable.ic_photo_gray_24dp)
                .into(imageEpisode,
                    object : Callback {
                        override fun onSuccess() {
                            imageEpisode.scaleType = ImageView.ScaleType.CENTER_CROP
                        }

                        override fun onError(e: Exception) {
                            imageEpisode.scaleType = ImageView.ScaleType.CENTER_INSIDE
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

    private fun populateShowViews(show: SgShow2) {
        // set show title in action bar
        val showTitle = show.title
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.title = showTitle
            requireActivity().title = getString(R.string.description_overview) + showTitle
        }

        val view = view ?: return

        // status
        val statusText = view.findViewById<TextView>(R.id.showStatus)
        getServicesComponent(requireContext()).showTools()
            .setStatusAndColor(statusText, show.statusOrUnknown)

        // favorite
        val isFavorite = show.favorite
        buttonFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_star_black_24dp else R.drawable.ic_star_border_black_24dp
        )
        buttonFavorite.contentDescription = getString(
            if (isFavorite) R.string.context_unfavorite else R.string.context_favorite
        )
        TooltipCompat.setTooltipText(buttonFavorite, buttonFavorite.contentDescription)
        buttonFavorite.tag = isFavorite

        // Regular network, release time and length.
        val network = show.network
        var time: String? = null
        val releaseTime = show.releaseTime
        if (releaseTime != null && releaseTime != -1) {
            val weekDay = show.releaseWeekDayOrDefault
            val release = TimeTools.getShowReleaseDateTime(
                requireContext(),
                releaseTime,
                weekDay,
                show.releaseTimeZone,
                show.releaseCountry,
                network
            )
            val dayString = TimeTools.formatToLocalDayOrDaily(requireContext(), release, weekDay)
            val timeString = TimeTools.formatToLocalTime(requireContext(), release)
            // "Mon 08:30"
            time = "$dayString $timeString"
        }
        val runtime = getString(
            R.string.runtime_minutes, show.runtime.toString()
        )
        val combinedString = TextTools.dotSeparate(TextTools.dotSeparate(network, time), runtime)
        val textViewNetworkAndTime = view.findViewById<TextView>(R.id.showmeta)
        textViewNetworkAndTime.text = combinedString
        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        textViewNetworkAndTime.copyTextToClipboardOnLongClick()

        // episode description might need show language, so update it here as well
        populateEpisodeDescriptionAndTvdbButton()
    }

    private fun runIfHasEpisode(block: (episode: SgEpisode2) -> Unit) {
        val currentEpisode = episode
        if (currentEpisode != null) {
            block.invoke(currentEpisode)
        }
    }

    private fun maybeAddFeedbackView() {
        val feedbackViewStub = feedbackViewStub
        if (feedbackView != null || feedbackViewStub == null
            || !hasSetEpisodeWatched || !AppSettings.shouldAskForFeedback(requireContext())) {
            return  // can or should not add feedback view
        }
        feedbackView = feedbackViewStub.inflate() as FeedbackView
        this.feedbackViewStub = null
        feedbackView?.setCallback(object : FeedbackView.Callback {
            override fun onRate() {
                if (Utils.launchWebsite(context, getString(R.string.url_store_page))) {
                    removeFeedbackView()
                }
            }

            override fun onFeedback() {
                if (Utils.tryStartActivity(
                        requireContext(), getFeedbackEmailIntent(requireContext()), true
                    )) {
                    removeFeedbackView()
                }
            }

            override fun onDismiss() {
                removeFeedbackView()
            }
        })
    }

    private fun removeFeedbackView() {
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
                if (!isAdded) {
                    return
                }
                if (data == null) {
                    Timber.e("onLoadFinished: did not receive valid actions")
                } else {
                    Timber.d("onLoadFinished: received %s actions", data.size)
                }
                ActionsHelper.populateActions(
                    requireActivity().layoutInflater,
                    requireActivity().theme, containerActions, data
                )
            }

            override fun onLoaderReset(loader: Loader<MutableList<Action>?>) {
                ActionsHelper.populateActions(
                    requireActivity().layoutInflater,
                    requireActivity().theme, containerActions, null
                )
            }
        }

    companion object {

        private const val ARG_LONG_SHOW_ROWID = "show_id"
        private const val ARG_EPISODE_ID = "episode_id"

        fun buildArgs(showRowId: Long): Bundle {
            val args = Bundle()
            args.putLong(ARG_LONG_SHOW_ROWID, showRowId)
            return args
        }

        fun newInstance(showRowId: Long): OverviewFragment {
            return OverviewFragment().apply {
                arguments = buildArgs(showRowId)
            }
        }
    }
}