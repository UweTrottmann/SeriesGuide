package com.battlelancer.seriesguide.shows.overview

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.comments.TraktCommentsActivity
import com.battlelancer.seriesguide.people.PeopleListHelper
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.overview.OverviewActivityImpl.OverviewLayoutType.MULTI_PANE_VERTICAL
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsActivity
import com.battlelancer.seriesguide.shows.tools.ShowStatus
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.FullscreenImageActivity
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.Metacritic
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.ShortcutCreator
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.google.android.material.button.MaterialButton
import com.uwetrottmann.tmdb2.entities.Credits
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays extended information (poster, release info, description, ...) and actions (favoriting,
 * shortcut) for a particular show.
 */
class ShowFragment() : Fragment() {

    constructor(showRowId: Long) : this() {
        arguments = buildArgs(showRowId)
    }

    private var binding: Binding? = null
    private var showId: Long = 0
    private var show: SgShow2? = null
    private var languageCode: String? = null

    val model: ShowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            showId = it.getLong(ARG_SHOW_ROWID)
        } ?: throw IllegalArgumentException("Missing arguments")
    }

    class Binding(view: View) {
        val scrollViewShow: NestedScrollView
        val containerPoster: View
        val imageViewPoster: ImageView
        val textViewStatus: TextView
        val textViewReleaseTime: TextView
        val textViewOverview: TextView
        val textViewReleaseCountry: TextView
        val textViewFirstRelease: TextView
        val textShowLastUpdated: TextView
        val textViewContentRating: TextView
        val textViewGenres: TextView
        val textViewRating: TextView
        val textViewRatingRange: TextView
        val textViewRatingVotes: TextView
        val textViewRatingUser: TextView
        val buttonFavorite: MaterialButton
        val buttonNotify: MaterialButton
        val buttonHidden: MaterialButton
        val buttonShortcut: Button
        val buttonLanguage: Button
        val buttonRate: View
        val buttonSimilar: Button
        val buttonImdb: Button
        val buttonShowMetacritic: Button
        val buttonTmdb: Button
        val buttonTrakt: Button
        val buttonWebSearch: Button
        val buttonComments: Button
        val buttonShare: Button
        val castLabel: TextView
        val castContainer: LinearLayout
        val crewLabel: TextView
        val crewContainer: LinearLayout

        init {
            // Show fragment and included layouts vary depending on screen size,
            // currently not seeing an easy way to use view binding, so just using findViewById.
            scrollViewShow = view.findViewById(R.id.scrollViewShow)
            containerPoster = view.findViewById(R.id.containerShowPoster)
            imageViewPoster = view.findViewById(R.id.imageViewShowPoster)
            textViewStatus = view.findViewById(R.id.textViewShowStatus)
            textViewReleaseTime = view.findViewById(R.id.textViewShowReleaseTime)
            textViewOverview = view.findViewById(R.id.textViewShowOverview)
            textViewReleaseCountry = view.findViewById(R.id.textViewShowReleaseCountry)
            textViewFirstRelease = view.findViewById(R.id.textViewShowFirstAirdate)
            textShowLastUpdated = view.findViewById(R.id.textShowLastUpdated)
            textViewContentRating = view.findViewById(R.id.textViewShowContentRating)
            textViewGenres = view.findViewById(R.id.textViewShowGenres)
            textViewRating = view.findViewById(R.id.textViewRatingsValue)
            textViewRatingRange = view.findViewById(R.id.textViewRatingsRange)
            textViewRatingVotes = view.findViewById(R.id.textViewRatingsVotes)
            textViewRatingUser = view.findViewById(R.id.textViewRatingsUser)
            buttonFavorite = view.findViewById(R.id.buttonShowFavorite)
            buttonNotify = view.findViewById(R.id.buttonShowNotify)
            buttonHidden = view.findViewById(R.id.buttonShowHidden)
            buttonShortcut = view.findViewById(R.id.buttonShowShortcut)
            buttonLanguage = view.findViewById(R.id.buttonShowLanguage)
            buttonRate = view.findViewById(R.id.containerRatings)
            buttonSimilar = view.findViewById(R.id.buttonShowSimilar)
            buttonImdb = view.findViewById(R.id.buttonShowImdb)
            buttonShowMetacritic = view.findViewById(R.id.buttonShowMetacritic)
            buttonTmdb = view.findViewById(R.id.buttonShowTmdb)
            buttonTrakt = view.findViewById(R.id.buttonShowTrakt)
            buttonWebSearch = view.findViewById(R.id.buttonShowWebSearch)
            buttonComments = view.findViewById(R.id.buttonShowComments)
            buttonShare = view.findViewById(R.id.buttonShowShare)
            castLabel = view.findViewById(R.id.labelCast)
            castContainer = view.findViewById(R.id.containerCast)
            crewLabel = view.findViewById(R.id.labelCrew)
            crewContainer = view.findViewById(R.id.containerCrew)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_show, container, false)
        binding = Binding(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return
        // In the vertical multi-pane layout the show fragment sits on top of the seasons fragment,
        // so do not inset to avoid navigation bar.
        if (OverviewActivityImpl.getLayoutType(requireContext()) != MULTI_PANE_VERTICAL) {
            ThemeUtils.applySystemBarInset(binding.scrollViewShow)
        }

        // favorite + notifications + visibility button
        TooltipCompat.setTooltipText(
            binding.buttonFavorite,
            binding.buttonFavorite.contentDescription
        )
        TooltipCompat.setTooltipText(binding.buttonNotify, binding.buttonNotify.contentDescription)
        TooltipCompat.setTooltipText(binding.buttonHidden, binding.buttonHidden.contentDescription)

        // language button
        val buttonLanguage = binding.buttonLanguage
        buttonLanguage.setOnClickListener { displayLanguageSettings() }
        TooltipCompat.setTooltipText(
            buttonLanguage,
            buttonLanguage.context.getString(R.string.pref_language)
        )

        // rate button
        val buttonRate = binding.buttonRate
        buttonRate.setOnClickListener { rateShow() }
        TooltipCompat.setTooltipText(buttonRate, buttonRate.context.getString(R.string.action_rate))
        binding.textViewRatingRange.text = getString(R.string.format_rating_range, 10)

        // share button
        binding.buttonShare.setOnClickListener { shareShow() }

        // shortcut button
        binding.buttonShortcut.setOnClickListener { createShortcut() }

        setCastVisibility(binding, false)
        setCrewVisibility(binding, false)

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        binding.textViewOverview.copyTextToClipboardOnLongClick()
        binding.textViewGenres.copyTextToClipboardOnLongClick()
        binding.textViewContentRating.copyTextToClipboardOnLongClick()
        binding.textViewReleaseCountry.copyTextToClipboardOnLongClick()
        binding.textViewFirstRelease.copyTextToClipboardOnLongClick()

        model.setShowId(showId)
        model.show.observe(viewLifecycleOwner) { sgShow2 ->
            if (sgShow2 != null) {
                show = sgShow2
                populateShow(sgShow2)
            }
        }
        model.credits.observe(viewLifecycleOwner) { credits ->
            populateCredits(credits)
        }
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun populateShow(show: SgShow2) {
        val binding = binding ?: return

        // status
        ShowStatus.setStatusAndColor(binding.textViewStatus, show.statusOrUnknown)

        // Network, next release day and time, runtime
        val releaseCountry = show.releaseCountry
        val releaseTime = show.releaseTime
        val network = show.network
        val time = if (releaseTime != null && releaseTime != -1) {
            val weekDay = show.releaseWeekDayOrDefault
            val release = TimeTools.getShowReleaseDateTime(
                requireContext(),
                releaseTime,
                weekDay,
                show.releaseTimeZone,
                releaseCountry, network
            )
            val dayString = TimeTools.formatToLocalDayOrDaily(requireContext(), release, weekDay)
            val timeString = TimeTools.formatToLocalTime(requireContext(), release)
            String.format("%s %s", dayString, timeString)
        } else {
            null
        }
        val runtime = getString(
            R.string.runtime_minutes,
            show.runtime.toString()
        )
        val combinedString =
            TextTools.dotSeparate(TextTools.dotSeparate(network, time), runtime)
        binding.textViewReleaseTime.text = combinedString

        // favorite button
        val isFavorite = show.favorite
        binding.buttonFavorite.apply {
            text = getString(
                if (isFavorite) R.string.state_favorite else R.string.context_favorite
            )
            contentDescription = getString(
                if (isFavorite) R.string.context_unfavorite else R.string.context_favorite
            )
            setIconResource(
                if (isFavorite) {
                    R.drawable.ic_star_black_24dp
                } else {
                    R.drawable.ic_star_border_black_24dp
                }
            )
            isEnabled = true
            setOnClickListener { v ->
                // disable until action is complete
                v.isEnabled = false
                SgApp.getServicesComponent(requireContext()).showTools()
                    .storeIsFavorite(showId, !isFavorite)
            }
        }

        // notifications button
        val notify = show.notify
        binding.buttonNotify.apply {
            contentDescription = getString(
                if (notify) {
                    R.string.action_episode_notifications_off
                } else {
                    R.string.action_episode_notifications_on
                }
            )
            setIconResource(
                if (notify) {
                    R.drawable.ic_notifications_active_black_24dp
                } else {
                    R.drawable.ic_notifications_off_black_24dp
                }
            )
            isEnabled = true
            setOnClickListener { v ->
                if (Utils.hasAccessToX(activity)) {
                    // disable until action is complete
                    v.isEnabled = false
                    SgApp.getServicesComponent(requireContext()).showTools()
                        .storeNotify(showId, !notify)
                } else {
                    Utils.advertiseSubscription(activity)
                }
            }
        }

        // hidden button
        val isHidden = show.hidden
        binding.buttonHidden.apply {
            text = getString(
                if (isHidden) R.string.action_shows_filter_hidden else R.string.context_hide
            )
            contentDescription = getString(
                if (isHidden) R.string.context_unhide else R.string.context_hide
            )
            setIconResource(
                if (isHidden) {
                    R.drawable.ic_visibility_off_black_24dp
                } else {
                    R.drawable.ic_visibility_white_24dp
                }
            )
            isEnabled = true
            setOnClickListener { v ->
                // disable until action is complete
                v.isEnabled = false
                SgApp.getServicesComponent(requireContext()).showTools()
                    .storeIsHidden(showId, !isHidden)
            }
        }

        // overview
        var overview = show.overview
        val languageCode = show.language?.let { LanguageTools.mapLegacyShowCode(it) }
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            overview = TextTools.textNoTranslation(requireContext(), languageCode)
        }
        binding.textViewOverview.text =
            TextTools.textWithTmdbSource(binding.textViewOverview.context, overview)

        // language preferred for content
        val languageData = LanguageTools.getShowLanguageDataFor(
            requireContext(), languageCode
        )
        if (languageData != null) {
            this.languageCode = languageData.languageCode
            binding.buttonLanguage.text = languageData.languageString
        }

        // country for release time calculation
        // show "unknown" if country is not supported
        binding.textViewReleaseCountry.text = TimeTools.getCountry(activity, releaseCountry)

        // original release
        ViewTools.setValueOrPlaceholder(
            binding.textViewFirstRelease,
            TimeTools.getShowReleaseYear(show.firstRelease)
        )

        // When the show was last updated by this app
        binding.textShowLastUpdated.text =
            TextTools.timeInMillisToDateAndTime(requireContext(), show.lastUpdatedMs)

        // content rating
        ViewTools.setValueOrPlaceholder(binding.textViewContentRating, show.contentRating)
        // genres
        ViewTools.setValueOrPlaceholder(
            binding.textViewGenres,
            TextTools.splitPipeSeparatedStrings(show.genres)
        )

        // trakt rating
        binding.textViewRating.text = TraktTools.buildRatingString(show.ratingGlobal)
        binding.textViewRatingVotes.text =
            TraktTools.buildRatingVotesString(activity, show.ratingVotes)

        // user rating
        binding.textViewRatingUser.text =
            TraktTools.buildUserRatingString(activity, show.ratingUser)

        // Similar shows button.
        binding.buttonSimilar.setOnClickListener {
            show.tmdbId?.also {
                startActivity(SimilarShowsActivity.intent(requireContext(), it, show.title))
            }
        }

        // IMDb button
        ServiceUtils.setUpImdbButton(show.imdbId, binding.buttonImdb)

        show.tmdbId?.also {
            // TMDb button
            val url = TmdbTools.buildShowUrl(it)
            ViewTools.openUriOnClick(binding.buttonTmdb, url)
            binding.buttonTmdb.copyTextToClipboardOnLongClick(url)

            // Trakt button
            val traktLink = TraktTools.buildShowUrl(it)
            ViewTools.openUriOnClick(binding.buttonTrakt, traktLink)
            binding.buttonTrakt.copyTextToClipboardOnLongClick(traktLink)
        }

        binding.buttonShowMetacritic.setOnClickListener {
            if (show.title.isNotEmpty()) Metacritic.searchForTvShow(requireContext(), show.title)
        }

        // web search button
        ServiceUtils.setUpWebSearchButton(show.title, binding.buttonWebSearch)

        // shout button
        binding.buttonComments.setOnClickListener { v ->
            val i = TraktCommentsActivity.intentShow(requireContext(), show.title, showId)
            Utils.startActivityWithAnimation(activity, i, v)
        }

        // poster, full screen poster button
        val posterSmall = show.posterSmall
        if (posterSmall.isNullOrEmpty()) {
            // have no poster
            binding.containerPoster.isClickable = false
            binding.containerPoster.isFocusable = false
        } else {
            // poster and fullscreen button
            ImageTools.loadShowPoster(requireActivity(), binding.imageViewPoster, posterSmall)
            binding.containerPoster.isFocusable = true
            binding.containerPoster.setOnClickListener { v ->
                val intent = FullscreenImageActivity.intent(
                    requireContext(),
                    ImageTools.tmdbOrTvdbPosterUrl(posterSmall, requireContext()),
                    ImageTools.tmdbOrTvdbPosterUrl(
                        show.poster,
                        requireContext(),
                        originalSize = true
                    )
                )
                Utils.startActivityWithAnimation(activity, intent, v)
            }
        }
    }

    private fun populateCredits(credits: Credits?) {
        val binding = binding ?: return
        if (credits == null) {
            setCastVisibility(binding, false)
            setCrewVisibility(binding, false)
            return
        }

        if (credits.cast?.size != 0
            && PeopleListHelper.populateShowCast(activity, binding.castContainer, credits)) {
            setCastVisibility(binding, true)
        } else {
            setCastVisibility(binding, false)
        }

        if (credits.crew?.size != 0
            && PeopleListHelper.populateShowCrew(activity, binding.crewContainer, credits)) {
            setCrewVisibility(binding, true)
        } else {
            setCrewVisibility(binding, false)
        }
    }

    private fun setCastVisibility(binding: Binding, visible: Boolean) {
        binding.castLabel.visibility = if (visible) View.VISIBLE else View.GONE
        binding.castContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setCrewVisibility(binding: Binding, visible: Boolean) {
        binding.crewLabel.visibility = if (visible) View.VISIBLE else View.GONE
        binding.crewContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun rateShow() {
        RateDialogFragment.newInstanceShow(showId).safeShow(context, parentFragmentManager)
    }

    private fun displayLanguageSettings() {
        L10nDialogFragment.show(
            parentFragmentManager,
            languageCode, "showLanguageDialog"
        )
    }

    private fun changeShowLanguage(languageCode: String) {
        this.languageCode = languageCode

        Timber.d("Changing show language to %s", languageCode)
        SgApp.getServicesComponent(requireContext()).showTools()
            .storeLanguage(showId, languageCode)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: L10nDialogFragment.LanguageChangedEvent) {
        changeShowLanguage(event.selectedLanguageCode)
    }

    private fun createShortcut() {
        if (!Utils.hasAccessToX(activity)) {
            Utils.advertiseSubscription(activity)
            return
        }

        // create the shortcut
        show?.also { show ->
            if (show.tmdbId != null && show.posterSmall != null) {
                val shortcutLiveData = ShortcutCreator(
                    requireContext(),
                    show.title,
                    show.posterSmall,
                    show.tmdbId
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    whenStarted {
                        shortcutLiveData.prepareAndPinShortcut()
                    }
                }
            }
        }
    }

    private fun shareShow() {
        show?.also {
            ShareUtils.shareShow(activity, it.tmdbId ?: 0, it.title)
        }
    }

    companion object {
        const val liftOnScrollTargetViewId = R.id.scrollViewShow

        private const val ARG_SHOW_ROWID = "show_id"

        @JvmStatic
        fun buildArgs(showRowId: Long): Bundle {
            return bundleOf(ARG_SHOW_ROWID to showRowId)
        }
    }
}
