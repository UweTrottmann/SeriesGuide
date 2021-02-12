package com.battlelancer.seriesguide.ui.overview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.thetvdbapi.TvdbLinks
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktRatingsFetcher
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.FullscreenImageActivity
import com.battlelancer.seriesguide.ui.comments.TraktCommentsActivity
import com.battlelancer.seriesguide.ui.dialogs.ShowL10nDialogFragment
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment
import com.battlelancer.seriesguide.ui.people.PeopleListHelper
import com.battlelancer.seriesguide.ui.search.SimilarShowsActivity
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.Metacritic
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.ShortcutCreator
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TextToolsK
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.google.android.material.button.MaterialButton
import com.uwetrottmann.androidutils.CheatSheet
import com.uwetrottmann.tmdb2.entities.Credits
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays extended information (poster, release info, description, ...) and actions (favoriting,
 * shortcut) for a particular show.
 */
@SuppressLint("NonConstantResourceId")
class ShowFragment() : Fragment() {

    constructor(showRowId: Long) : this() {
        arguments = buildArgs(showRowId)
    }

    @BindView(R.id.imageViewShowPosterBackground)
    internal lateinit var imageViewBackground: ImageView

    @BindView(R.id.containerShowPoster)
    internal lateinit var containerPoster: View
    @BindView(R.id.imageViewShowPoster)
    internal lateinit var imageViewPoster: ImageView
    @BindView(R.id.textViewShowStatus)
    @JvmField
    internal var textViewStatus: TextView? = null
    @BindView(R.id.textViewShowReleaseTime)
    @JvmField
    internal var textViewReleaseTime: TextView? = null
    @BindView(R.id.textViewShowOverview)
    internal lateinit var textViewOverview: TextView
    @BindView(R.id.textViewShowReleaseCountry)
    internal lateinit var textViewReleaseCountry: TextView
    @BindView(R.id.textViewShowFirstAirdate)
    internal lateinit var textViewFirstRelease: TextView
    @BindView(R.id.textViewShowContentRating)
    internal lateinit var textViewContentRating: TextView
    @BindView(R.id.textViewShowGenres)
    internal lateinit var textViewGenres: TextView
    @BindView(R.id.textViewRatingsValue)
    internal lateinit var textViewRating: TextView
    @BindView(R.id.textViewRatingsRange)
    internal lateinit var textViewRatingRange: TextView
    @BindView(R.id.textViewRatingsVotes)
    internal lateinit var textViewRatingVotes: TextView
    @BindView(R.id.textViewRatingsUser)
    internal lateinit var textViewRatingUser: TextView

    @BindView(R.id.buttonShowFavorite)
    internal lateinit var buttonFavorite: MaterialButton
    @BindView(R.id.buttonShowNotify)
    internal lateinit var buttonNotify: MaterialButton
    @BindView(R.id.buttonShowHidden)
    internal lateinit var buttonHidden: MaterialButton
    @BindView(R.id.buttonShowShortcut)
    internal lateinit var buttonShortcut: Button
    @BindView(R.id.buttonShowLanguage)
    internal lateinit var buttonLanguage: Button
    @BindView(R.id.containerRatings)
    internal lateinit var buttonRate: View
    @BindView(R.id.buttonShowSimilar)
    internal lateinit var buttonSimilar: Button
    @BindView(R.id.buttonShowImdb)
    internal lateinit var buttonImdb: Button
    @BindView(R.id.buttonShowMetacritic)
    internal lateinit var buttonShowMetacritic: Button
    @BindView(R.id.buttonShowTvdb)
    internal lateinit var buttonTvdb: Button
    @BindView(R.id.buttonShowTrakt)
    internal lateinit var buttonTrakt: Button
    @BindView(R.id.buttonShowWebSearch)
    internal lateinit var buttonWebSearch: Button
    @BindView(R.id.buttonShowComments)
    internal lateinit var buttonComments: Button
    @BindView(R.id.buttonShowShare)
    internal lateinit var buttonShare: Button

    @BindView(R.id.labelCast)
    internal lateinit var castLabel: TextView
    @BindView(R.id.containerCast)
    internal lateinit var castContainer: LinearLayout
    @BindView(R.id.labelCrew)
    internal lateinit var crewLabel: TextView
    @BindView(R.id.containerCrew)
    internal lateinit var crewContainer: LinearLayout

    private lateinit var unbinder: Unbinder

    private var showId: Long = 0
    private var show: SgShow2? = null
//    private var showTvdbId: Int = 0
//    private var showCursor: Cursor? = null
    private lateinit var showTools: ShowTools
    private var ratingFetchJob: Job? = null
//    private var showSlug: String? = null
//    private var showTitle: String? = null
//    private var posterPath: String? = null
//    private var posterPathSmall: String? = null
    private var languageCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showTools = SgApp.getServicesComponent(requireContext()).showTools()
        arguments?.let {
            showId = it.getLong(ARG_SHOW_ROWID)
        } ?: throw IllegalArgumentException("Missing arguments")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_show, container, false)
        unbinder = ButterKnife.bind(this, view)

        // favorite + notifications + visibility button
        CheatSheet.setup(buttonFavorite)
        CheatSheet.setup(buttonNotify)
        CheatSheet.setup(buttonHidden)

        // language button
        buttonLanguage.setOnClickListener { displayLanguageSettings() }
        CheatSheet.setup(buttonLanguage, R.string.pref_language)

        // rate button
        buttonRate.setOnClickListener { rateShow() }
        CheatSheet.setup(buttonRate, R.string.action_rate)
        textViewRatingRange.text = getString(R.string.format_rating_range, 10)

        // share button
        buttonShare.setOnClickListener { shareShow() }

        // shortcut button
        buttonShortcut.setOnClickListener { createShortcut() }

        setCastVisibility(false)
        setCrewVisibility(false)

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        textViewOverview.copyTextToClipboardOnLongClick()
        textViewGenres.copyTextToClipboardOnLongClick()
        textViewContentRating.copyTextToClipboardOnLongClick()
        textViewReleaseCountry.copyTextToClipboardOnLongClick()
        textViewFirstRelease.copyTextToClipboardOnLongClick()

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val model: ShowViewModel by viewModels()
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

        setHasOptionsMenu(true)
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

        unbinder.unbind()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release reference to any job.
        ratingFetchJob = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.show_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_show_manage_lists -> {
                show?.tvdbId?.also {
                    ManageListsDialogFragment.show(parentFragmentManager, it, ListItemTypes.SHOW)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populateShow(show: SgShow2) {
        // status
        textViewStatus?.let {
            showTools.setStatusAndColor(it, show.statusOrUnknown)
        }

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
        textViewReleaseTime?.text = combinedString

        // favorite button
        val isFavorite = show.favorite
        buttonFavorite.apply {
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
                showTools.storeIsFavorite(showId, !isFavorite)
            }
        }

        // notifications button
        val notify = show.notify
        buttonNotify.apply {
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
                    showTools.storeNotify(showId, !notify)
                } else {
                    Utils.advertiseSubscription(activity)
                }
            }
        }

        // hidden button
        val isHidden = show.hidden
        buttonHidden.apply {
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
                showTools.storeIsHidden(showId, !isHidden)
            }
        }

        // overview
        var overview = show.overview
        val languageCode = show.language
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            overview = TextToolsK.textNoTranslation(requireContext(), languageCode)
        }
        textViewOverview.text = TextTools.textWithTmdbSource(textViewOverview.context, overview)

        // language preferred for content
        val languageData = LanguageTools.getShowLanguageDataFor(
            context, languageCode
        )
        if (languageData != null) {
            this.languageCode = languageData.languageCode
            buttonLanguage.text = languageData.languageString
        }

        // country for release time calculation
        // show "unknown" if country is not supported
        textViewReleaseCountry.text = TimeTools.getCountry(activity, releaseCountry)

        // original release
        ViewTools.setValueOrPlaceholder(
            textViewFirstRelease,
            TimeTools.getShowReleaseYear(show.firstRelease)
        )

        // content rating
        ViewTools.setValueOrPlaceholder(textViewContentRating, show.contentRating)
        // genres
        ViewTools.setValueOrPlaceholder(
            textViewGenres,
            TextTools.splitAndKitTVDBStrings(show.genres)
        )

        // trakt rating
        textViewRating.text = TraktTools.buildRatingString(show.ratingGlobal)
        textViewRatingVotes.text = TraktTools.buildRatingVotesString(activity, show.ratingVotes)

        // user rating
        textViewRatingUser.text = TraktTools.buildUserRatingString(activity, show.ratingUser)

        // Similar shows button.
        buttonSimilar.setOnClickListener {
            show.tvdbId?.also {
                startActivity(SimilarShowsActivity.intent(requireContext(), it, show.title))
            }
        }

        // IMDb button
        ServiceUtils.setUpImdbButton(show.imdbId, buttonImdb)

        // TVDb button
        val tvdbLink = TvdbLinks.show(show.slug, 0)
        ViewTools.openUriOnClick(buttonTvdb, tvdbLink)
        buttonTvdb.copyTextToClipboardOnLongClick(tvdbLink)

        // trakt button
        show.tvdbId?.also {
            val traktLink = TraktTools.buildShowUrl(it)
            ViewTools.openUriOnClick(buttonTrakt, traktLink)
            buttonTrakt.copyTextToClipboardOnLongClick(traktLink)
        }

        buttonShowMetacritic.setOnClickListener {
            if (show.title.isNotEmpty()) Metacritic.searchForTvShow(requireContext(), show.title)
        }

        // web search button
        ServiceUtils.setUpWebSearchButton(show.title, buttonWebSearch)

        // shout button
        buttonComments.setOnClickListener { v ->
            show.tvdbId?.also {
                val i = Intent(activity, TraktCommentsActivity::class.java).putExtras(
                    TraktCommentsActivity.createInitBundleShow(show.title, it)
                )
                Utils.startActivityWithAnimation(activity, i, v)
            }
        }

        // poster, full screen poster button
        val posterSmall = show.posterSmall
        if (posterSmall.isNullOrEmpty()) {
            // have no poster
            containerPoster.isClickable = false
            containerPoster.isFocusable = false
        } else {
            // poster and fullscreen button
            TvdbImageTools.loadShowPoster(requireActivity(), imageViewPoster, posterSmall)
            containerPoster.isFocusable = true
            containerPoster.setOnClickListener { v ->
                val intent = Intent(activity, FullscreenImageActivity::class.java)
                intent.putExtra(
                    FullscreenImageActivity.EXTRA_PREVIEW_IMAGE,
                    TvdbImageTools.artworkUrl(posterSmall)
                )
                intent.putExtra(
                    FullscreenImageActivity.EXTRA_IMAGE,
                    TvdbImageTools.artworkUrl(show.poster)
                )
                Utils.startActivityWithAnimation(activity, intent, v)
            }

            // poster background
            TvdbImageTools.loadShowPosterAlpha(
                requireActivity(),
                imageViewBackground,
                posterSmall
            )
        }

        loadTraktRatings()
    }

    private fun populateCredits(credits: Credits?) {
        if (credits == null) {
            setCastVisibility(false)
            setCrewVisibility(false)
            return
        }

        if (credits.cast?.size != 0
            && PeopleListHelper.populateShowCast(activity, castContainer, credits)) {
            setCastVisibility(true)
        } else {
            setCastVisibility(false)
        }

        if (credits.crew?.size != 0
            && PeopleListHelper.populateShowCrew(activity, crewContainer, credits)) {
            setCrewVisibility(true)
        } else {
            setCrewVisibility(false)
        }
    }

    private fun setCastVisibility(visible: Boolean) {
        castLabel.visibility = if (visible) View.VISIBLE else View.GONE
        castContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setCrewVisibility(visible: Boolean) {
        crewLabel.visibility = if (visible) View.VISIBLE else View.GONE
        crewContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun rateShow() {
        show?.tvdbId?.also {
            RateDialogFragment.newInstanceShow(it).safeShow(context, parentFragmentManager)
        }
    }

    private fun loadTraktRatings() {
        show?.tvdbId?.also {
            val oldRatingFetchJob = ratingFetchJob
            if (oldRatingFetchJob == null || !oldRatingFetchJob.isActive) {
                ratingFetchJob = TraktRatingsFetcher.fetchShowRatingsAsync(requireContext(), it)
            }
        }
    }

    private fun displayLanguageSettings() {
        ShowL10nDialogFragment.show(
            parentFragmentManager,
            languageCode, "showLanguageDialog"
        )
    }

    private fun changeShowLanguage(languageCode: String) {
        this.languageCode = languageCode

        Timber.d("Changing show language to %s", languageCode)
        showTools.storeLanguage(showId, languageCode)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ShowL10nDialogFragment.LanguageChangedEvent) {
        changeShowLanguage(event.selectedLanguageCode)
    }

    private fun createShortcut() {
        if (!Utils.hasAccessToX(activity)) {
            Utils.advertiseSubscription(activity)
            return
        }

        // create the shortcut
        show?.also { show ->
            if (show.tvdbId != null && show.posterSmall != null) {
                val shortcutLiveData = ShortcutCreator(
                    requireContext(),
                    show.title,
                    show.posterSmall,
                    show.tvdbId
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
            ShareUtils.shareShow(activity, it.slug, it.tvdbId ?: 0, it.title)
        }
    }

    companion object {

        private const val ARG_SHOW_ROWID = "show_id"

        @JvmStatic
        fun buildArgs(showRowId: Long): Bundle {
            return bundleOf(ARG_SHOW_ROWID to showRowId)
        }
    }
}
