// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2023 Uwe Trottmann
// Copyright 2013 Andrew Neal

package com.battlelancer.seriesguide.shows.overview

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.TooltipCompat
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.comments.TraktCommentsActivity
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.people.PeopleListHelper
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.overview.OverviewActivityImpl.OverviewLayoutType.MULTI_PANE_VERTICAL
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsActivity
import com.battlelancer.seriesguide.shows.tools.ShowStatus
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.FullscreenImageActivity
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.Metacritic
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.ShortcutCreator
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.Credits
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
        val textViewBaseInfo: TextView
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
        val buttonEditReleaseTime: Button
        val buttonShortcut: Button
        val buttonLanguage: Button
        val buttonRate: View
        val buttonTrailer: Button
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
            textViewBaseInfo = view.findViewById(R.id.textViewShowBaseInfo)
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
            buttonEditReleaseTime = view.findViewById(R.id.buttonEditReleaseTime)
            buttonShortcut = view.findViewById(R.id.buttonShowShortcut)
            buttonLanguage = view.findViewById(R.id.buttonShowLanguage)
            buttonRate = view.findViewById(R.id.containerRatings)
            buttonTrailer = view.findViewById(R.id.buttonShowTrailer)
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
            ThemeUtils.applyBottomPaddingForNavigationBar(binding.scrollViewShow)
        }

        // Edit release time button
        binding.buttonEditReleaseTime.apply {
            contentDescription = getString(R.string.custom_release_time_edit)
            TooltipCompat.setTooltipText(this, contentDescription)
            setOnClickListener {
                CustomReleaseTimeDialogFragment(showId).safeShow(
                    parentFragmentManager,
                    "custom-release-time"
                )
            }
        }

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
        // Only populate show once both show data and user status is loaded.
        model.showForUi.observe(viewLifecycleOwner) {
            if (it != null) {
                show = it.show
                populateShow(it, model.hasAccessToX.value)
            }
        }
        model.hasAccessToX.observe(viewLifecycleOwner) {
            populateShow(model.showForUi.value, it)
        }
        model.credits.observe(viewLifecycleOwner) { credits ->
            populateCredits(credits)
        }
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
        model.updateUserStatus()
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun showNotificationsNotAllowedMessage() {
        (activity as BaseMessageActivity?)?.snackbarParentView
            ?.let {
                Snackbar
                    .make(it, R.string.notifications_allow_reason, Snackbar.LENGTH_LONG)
                    .show()
            }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Re-bind show views to update notification button state.
                populateShow(model.showForUi.value, model.hasAccessToX.value)
            } else {
                showNotificationsNotAllowedMessage()
            }
        }

    private val requestPreciseNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // RESULT_OK = permission enabled
            if (result.resultCode == Activity.RESULT_OK) {
                // Re-schedule with exact alarm (if this shows episode is next)
                NotificationService.trigger(requireContext())
            }
        }

    private fun populateShow(showForUi: ShowViewModel.ShowForUi?, hasAccessToX: Boolean?) {
        if (showForUi == null || hasAccessToX == null) return
        val binding = binding ?: return
        val show = showForUi.show

        // Release time, base info and status
        binding.buttonEditReleaseTime.text = showForUi.releaseTime
        binding.textViewBaseInfo.text = showForUi.baseInfo
        binding.textViewStatus.text = ShowStatus.buildYearAndStatus(requireContext(), show)

        // favorite button
        val isFavorite = show.favorite
        binding.buttonFavorite.apply {
            text = getString(
                if (isFavorite) R.string.state_favorite else R.string.context_favorite
            )
            contentDescription = getString(
                if (isFavorite) R.string.context_unfavorite else R.string.context_favorite
            )
            TooltipCompat.setTooltipText(this, contentDescription)
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

        // Notifications button, always show as disabled if user is not a sub.
        val areNotificationsAllowed = NotificationSettings.areNotificationsAllowed(requireContext())
        val notify = show.notify && hasAccessToX && areNotificationsAllowed
        binding.buttonNotify.apply {
            contentDescription = getString(
                if (notify) {
                    R.string.action_episode_notifications_off
                } else {
                    R.string.action_episode_notifications_on
                }
            )
            TooltipCompat.setTooltipText(this, contentDescription)
            setIconResource(
                if (notify) {
                    R.drawable.ic_notifications_active_black_24dp
                } else {
                    R.drawable.ic_notifications_off_black_24dp
                }
            )
            isEnabled = true
            setOnClickListener { v ->
                if (!hasAccessToX) {
                    Utils.advertiseSubscription(activity)
                } else if (!areNotificationsAllowed) {
                    if (AndroidUtils.isAtLeastTiramisu) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        showNotificationsNotAllowedMessage()
                    }
                } else {
                    // On Android 12+, ask for exact alarm permission, but still enable
                    // notifications right away. If granted, will just re-run notifications service.
                    if (!notify && !NotificationSettings.canScheduleExactAlarms(requireContext())) {
                        requestPreciseNotificationPermissionLauncher.launch(
                            @Suppress("NewApi") // Can never be here if not Android 12+
                            NotificationSettings.buildRequestExactAlarmSettingsIntent(requireContext())
                        )
                    }
                    // disable until action is complete
                    v.isEnabled = false
                    SgApp.getServicesComponent(requireContext()).showTools()
                        .storeNotify(showId, !notify)
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
            TooltipCompat.setTooltipText(this, contentDescription)
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
        // Source text requires styling, so needs UI context
        binding.textViewOverview.text =
            TextTools.textWithTmdbSource(requireContext(), showForUi.overview)

        // language preferred for content
        val languageData = showForUi.languageData
        if (languageData != null) {
            this.languageCode = languageData.languageCode
            binding.buttonLanguage.text = languageData.languageString
        }

        // More infos
        binding.textViewReleaseCountry.text = showForUi.country
        ViewTools.setValueOrPlaceholder(binding.textViewFirstRelease, showForUi.releaseYear)
        binding.textShowLastUpdated.text = showForUi.lastUpdated
        ViewTools.setValueOrPlaceholder(binding.textViewContentRating, show.contentRating)
        ViewTools.setValueOrPlaceholder(binding.textViewGenres, showForUi.genres)

        // Trakt rating
        binding.textViewRating.text = showForUi.traktRating
        binding.textViewRatingVotes.text = showForUi.traktVotes
        binding.textViewRatingUser.text = showForUi.traktUserRating

        // Trailer button
        binding.buttonTrailer.apply {
            if (showForUi.trailerVideoId != null) {
                setOnClickListener {
                    ServiceUtils.openYoutube(showForUi.trailerVideoId, requireContext())
                }
                isEnabled = true
            } else {
                setOnClickListener(null)
                isEnabled = false
            }
        }

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
            ViewTools.openUrlOnClickAndCopyOnLongPress(
                binding.buttonTmdb,
                TmdbTools.buildShowUrl(it)
            )

            // Trakt button
            ViewTools.openUrlOnClickAndCopyOnLongPress(
                binding.buttonTrakt,
                TraktTools.buildShowUrl(it)
            )
        }

        binding.buttonShowMetacritic.setOnClickListener {
            if (show.title.isNotEmpty()) Metacritic.searchForTvShow(requireContext(), show.title)
        }

        // web search button
        ServiceUtils.setUpWebSearchButton(show.title, binding.buttonWebSearch)

        // shout button
        binding.buttonComments.setOnClickListener { v ->
            val showId = showId
            if (showId > 0) {
                val i = TraktCommentsActivity.intentShow(requireContext(), show.title, showId)
                Utils.startActivityWithAnimation(activity, i, v)
            }
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

        val peopleListHelper = PeopleListHelper()
        if (credits.cast?.size != 0
            && peopleListHelper.populateShowCast(activity, binding.castContainer, credits)) {
            setCastVisibility(binding, true)
        } else {
            setCastVisibility(binding, false)
        }

        if (credits.crew?.size != 0
            && peopleListHelper.populateShowCrew(activity, binding.crewContainer, credits)) {
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
                ShortcutCreator(
                    requireContext(),
                    show.title,
                    show.posterSmall,
                    show.tmdbId
                ).prepareAndPinShortcut(viewLifecycleOwner)
            }
        }
    }

    private fun shareShow() {
        show?.also {
            ShareUtils.shareShow(activity, it.tmdbId ?: 0, it.title)
        }
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.scrollViewShow

        private const val ARG_SHOW_ROWID = "show_id"

        @JvmStatic
        fun buildArgs(showRowId: Long): Bundle {
            return bundleOf(ARG_SHOW_ROWID to showRowId)
        }
    }
}
