package com.battlelancer.seriesguide.ui.overview

import android.content.Intent
import android.database.Cursor
import android.os.AsyncTask
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.loader.app.LoaderManager
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.thetvdbapi.TvdbLinks
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktRatingsTask
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.FullscreenImageActivity
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.comments.TraktCommentsActivity
import com.battlelancer.seriesguide.ui.dialogs.LanguageChoiceDialogFragment
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment
import com.battlelancer.seriesguide.ui.people.PeopleListHelper
import com.battlelancer.seriesguide.ui.people.ShowCreditsLoader
import com.battlelancer.seriesguide.ui.search.SimilarShowsActivity
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.ShortcutCreator
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.uwetrottmann.androidutils.CheatSheet
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
class ShowFragment : Fragment() {

    @BindView(R.id.imageViewShowPosterBackground)
    internal lateinit var imageViewBackground: ImageView

    @BindView(R.id.containerShowPoster)
    internal lateinit var containerPoster: View
    @BindView(R.id.imageViewShowPoster)
    internal lateinit var imageViewPoster: ImageView
    @BindView(R.id.textViewShowStatus)
    internal lateinit var textViewStatus: TextView
    @BindView(R.id.textViewShowReleaseTime)
    internal lateinit var textViewReleaseTime: TextView
    @BindView(R.id.textViewShowRuntime)
    internal lateinit var textViewRuntime: TextView
    @BindView(R.id.textViewShowNetwork)
    internal lateinit var textViewNetwork: TextView
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
    internal lateinit var buttonFavorite: Button
    @BindView(R.id.buttonShowNotify)
    internal lateinit var buttonNotify: Button
    @BindView(R.id.buttonShowHidden)
    internal lateinit var buttonHidden: Button
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

    private var showTvdbId: Int = 0
    private var showCursor: Cursor? = null
    private lateinit var showTools: ShowTools
    private var traktTask: TraktRatingsTask? = null
    private var showSlug: String? = null
    private var showTitle: String? = null
    private var posterPath: String? = null
    private var posterPathSmall: String? = null
    private var languageCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showTools = SgApp.getServicesComponent(requireContext()).showTools()
        arguments?.let {
            showTvdbId = it.getInt(ARG_SHOW_TVDBID)
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

        val loaderManager = LoaderManager.getInstance(this)
        loaderManager.initLoader(OverviewActivity.SHOW_LOADER_ID, null, showLoaderCallbacks)
        loaderManager.initLoader(
            OverviewActivity.SHOW_CREDITS_LOADER_ID, null, creditsLoaderCallbacks
        )

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.show_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_show_manage_lists -> {
                ManageListsDialogFragment.show(parentFragmentManager, showTvdbId, ListItemTypes.SHOW)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    internal interface ShowQuery {
        companion object {

            val PROJECTION = arrayOf(
                Shows._ID,
                Shows.TITLE,
                Shows.STATUS,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.NETWORK,
                Shows.POSTER,
                Shows.POSTER_SMALL,
                Shows.IMDBID,
                Shows.RUNTIME,
                Shows.FAVORITE,
                Shows.OVERVIEW,
                Shows.FIRST_RELEASE,
                Shows.CONTENTRATING,
                Shows.GENRES,
                Shows.RATING_GLOBAL,
                Shows.RATING_VOTES,
                Shows.RATING_USER,
                Shows.LASTEDIT,
                Shows.LANGUAGE,
                Shows.NOTIFY,
                Shows.HIDDEN,
                Shows.SLUG
            )

            const val TITLE = 1
            const val STATUS = 2
            const val RELEASE_TIME = 3
            const val RELEASE_WEEKDAY = 4
            const val RELEASE_TIMEZONE = 5
            const val RELEASE_COUNTRY = 6
            const val NETWORK = 7
            const val POSTER = 8
            const val POSTER_SMALL = 9
            const val IMDBID = 10
            const val RUNTIME = 11
            const val IS_FAVORITE = 12
            const val OVERVIEW = 13
            const val FIRST_RELEASE = 14
            const val CONTENT_RATING = 15
            const val GENRES = 16
            const val RATING_GLOBAL = 17
            const val RATING_VOTES = 18
            const val RATING_USER = 19
            const val LAST_EDIT_MS = 20
            const val LANGUAGE = 21
            const val NOTIFY = 22
            const val HIDDEN = 23
            const val SLUG = 24
        }
    }

    private val showLoaderCallbacks = object : LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            return CursorLoader(
                context!!, Shows.buildShowUri(showTvdbId),
                ShowQuery.PROJECTION, null, null, null
            )
        }

        override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
            if (!isAdded) {
                return
            }
            if (data != null && data.moveToFirst()) {
                showCursor = data
                populateShow(data)
            }
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {
            // do nothing, prefer stale data
        }
    }

    private fun populateShow(showCursor: Cursor) {
        showSlug = showCursor.getString(ShowQuery.SLUG)

        // title
        showTitle = showCursor.getString(ShowQuery.TITLE)
        posterPath = showCursor.getString(ShowQuery.POSTER)
        posterPathSmall = showCursor.getString(ShowQuery.POSTER_SMALL)

        // status
        ShowTools.setStatusAndColor(textViewStatus, showCursor.getInt(ShowQuery.STATUS))

        // next release day and time
        val releaseCountry = showCursor.getString(ShowQuery.RELEASE_COUNTRY)
        val releaseTime = showCursor.getInt(ShowQuery.RELEASE_TIME)
        val network = showCursor.getString(ShowQuery.NETWORK)
        if (releaseTime != -1) {
            val weekDay = showCursor.getInt(ShowQuery.RELEASE_WEEKDAY)
            val release = TimeTools.getShowReleaseDateTime(
                requireContext(),
                releaseTime,
                weekDay,
                showCursor.getString(ShowQuery.RELEASE_TIMEZONE),
                releaseCountry, network
            )
            val dayString = TimeTools.formatToLocalDayOrDaily(activity, release, weekDay)
            val timeString = TimeTools.formatToLocalTime(activity, release)
            textViewReleaseTime.text = String.format("%s %s", dayString, timeString)
        } else {
            textViewReleaseTime.text = null
        }

        // runtime
        textViewRuntime.text = getString(
            R.string.runtime_minutes,
            showCursor.getInt(ShowQuery.RUNTIME).toString()
        )

        // network
        textViewNetwork.text = network

        // favorite button
        val isFavorite = showCursor.getInt(ShowQuery.IS_FAVORITE) == 1
        buttonFavorite.apply {
            ViewTools.setVectorDrawableTop(
                this, if (isFavorite) {
                    R.drawable.ic_star_black_24dp
                } else {
                    R.drawable.ic_star_border_black_24dp
                }
            )
            val labelFavorite = getString(
                if (isFavorite) R.string.context_unfavorite else R.string.context_favorite
            )
            text = labelFavorite
            contentDescription = labelFavorite
            isEnabled = true
            setOnClickListener { v ->
                // disable until action is complete
                v.isEnabled = false
                showTools.storeIsFavorite(showTvdbId, !isFavorite)
            }
        }

        // notifications button
        val notify = showCursor.getInt(ShowQuery.NOTIFY) == 1
        buttonNotify.apply {
            contentDescription = getString(
                if (notify) {
                    R.string.action_episode_notifications_off
                } else {
                    R.string.action_episode_notifications_on
                }
            )
            ViewTools.setVectorDrawableTop(
                this, if (notify) {
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
                    showTools.storeNotify(showTvdbId, !notify)
                } else {
                    Utils.advertiseSubscription(activity)
                }
            }
        }

        // hidden button
        val isHidden = showCursor.getInt(ShowQuery.HIDDEN) == 1
        val label = getString(if (isHidden) R.string.context_unhide else R.string.context_hide)
        buttonHidden.apply {
            contentDescription = label
            text = label
            ViewTools.setVectorDrawableTop(
                this,
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
                showTools.storeIsHidden(showTvdbId, !isHidden)
            }
        }

        // overview
        var overview = showCursor.getString(ShowQuery.OVERVIEW)
        val languageCode = showCursor.getString(ShowQuery.LANGUAGE)
        if (TextUtils.isEmpty(overview)) {
            // no description available, show no translation available message
            overview = getString(
                R.string.no_translation,
                LanguageTools.getShowLanguageStringFor(
                    context,
                    languageCode
                ), getString(R.string.tvdb)
            )
        }
        val lastEditSeconds = showCursor.getLong(ShowQuery.LAST_EDIT_MS)
        textViewOverview.text = TextTools.textWithTvdbSource(
            textViewOverview.context,
            overview, lastEditSeconds
        )

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
        val firstRelease = showCursor.getString(ShowQuery.FIRST_RELEASE)
        ViewTools.setValueOrPlaceholder(
            textViewFirstRelease,
            TimeTools.getShowReleaseYear(firstRelease)
        )

        // content rating
        ViewTools.setValueOrPlaceholder(
            textViewContentRating,
            showCursor.getString(ShowQuery.CONTENT_RATING)
        )
        // genres
        ViewTools.setValueOrPlaceholder(
            textViewGenres,
            TextTools.splitAndKitTVDBStrings(showCursor.getString(ShowQuery.GENRES))
        )

        // trakt rating
        textViewRating.text = TraktTools.buildRatingString(
            showCursor.getDouble(ShowQuery.RATING_GLOBAL)
        )
        textViewRatingVotes.text = TraktTools.buildRatingVotesString(
            activity,
            showCursor.getInt(ShowQuery.RATING_VOTES)
        )

        // user rating
        textViewRatingUser.text = TraktTools.buildUserRatingString(
            activity,
            showCursor.getInt(ShowQuery.RATING_USER)
        )

        // Similar shows button.
        buttonSimilar.setOnClickListener {
            startActivity(SimilarShowsActivity.intent(requireContext(), showTvdbId, showTitle))
        }

        // IMDb button
        val imdbId = showCursor.getString(ShowQuery.IMDBID)
        ServiceUtils.setUpImdbButton(imdbId, buttonImdb)

        // TVDb button
        val tvdbLink = TvdbLinks.show(showSlug, showTvdbId)
        ViewTools.openUriOnClick(buttonTvdb, tvdbLink)
        buttonTvdb.copyTextToClipboardOnLongClick(tvdbLink)

        // trakt button
        val traktLink = TraktTools.buildShowUrl(showTvdbId)
        ViewTools.openUriOnClick(buttonTrakt, traktLink)
        buttonTrakt.copyTextToClipboardOnLongClick(traktLink)

        // web search button
        ServiceUtils.setUpWebSearchButton(showTitle, buttonWebSearch)

        // shout button
        buttonComments.setOnClickListener { v ->
            val i = Intent(activity, TraktCommentsActivity::class.java)
            i.putExtras(
                TraktCommentsActivity.createInitBundleShow(
                    showTitle,
                    showTvdbId
                )
            )
            Utils.startActivityWithAnimation(activity, i, v)
        }

        // poster, full screen poster button
        if (TextUtils.isEmpty(posterPathSmall)) {
            // have no poster
            containerPoster.isClickable = false
            containerPoster.isFocusable = false
        } else {
            // poster and fullscreen button
            TvdbImageTools.loadShowPoster(requireActivity(), imageViewPoster, posterPathSmall)
            containerPoster.isFocusable = true
            containerPoster.setOnClickListener { v ->
                val intent = Intent(activity, FullscreenImageActivity::class.java)
                intent.putExtra(
                    FullscreenImageActivity.EXTRA_PREVIEW_IMAGE,
                    TvdbImageTools.artworkUrl(posterPathSmall)
                )
                intent.putExtra(
                    FullscreenImageActivity.EXTRA_IMAGE,
                    TvdbImageTools.artworkUrl(posterPath)
                )
                Utils.startActivityWithAnimation(activity, intent, v)
            }

            // poster background
            TvdbImageTools.loadShowPosterAlpha(
                requireActivity(),
                imageViewBackground,
                posterPathSmall
            )
        }

        loadTraktRatings()
    }

    private val creditsLoaderCallbacks = object : LoaderCallbacks<Credits?> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Credits?> {
            return ShowCreditsLoader(context!!, showTvdbId, true)
        }

        override fun onLoadFinished(loader: Loader<Credits?>, data: Credits?) {
            if (isAdded) {
                populateCredits(data)
            }
        }

        override fun onLoaderReset(loader: Loader<Credits?>) {

        }
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
        RateDialogFragment.newInstanceShow(showTvdbId).safeShow(context, parentFragmentManager)
    }

    private fun loadTraktRatings() {
        val oldTraktTask = traktTask
        if (oldTraktTask == null || oldTraktTask.status == AsyncTask.Status.FINISHED) {
            val newTraktTask = TraktRatingsTask(context, showTvdbId)
            newTraktTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            traktTask = newTraktTask
        }
    }

    private fun displayLanguageSettings() {
        LanguageChoiceDialogFragment.show(
            parentFragmentManager,
            R.array.languageCodesShows, languageCode, "showLanguageDialog"
        )
    }

    private fun changeShowLanguage(languageCode: String) {
        this.languageCode = languageCode

        Timber.d("Changing show language to %s", languageCode)
        showTools.storeLanguage(showTvdbId, languageCode)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: LanguageChoiceDialogFragment.LanguageChangedEvent) {
        changeShowLanguage(event.selectedLanguageCode)
    }

    private fun createShortcut() {
        if (!Utils.hasAccessToX(activity)) {
            Utils.advertiseSubscription(activity)
            return
        }

        val currentShowTvdbId = showTvdbId
        val currentShowTitle = showTitle
        val currentPosterPath = posterPathSmall
        if (currentShowTvdbId == 0 || currentShowTitle == null || currentPosterPath == null) {
            return
        }

        // create the shortcut
        val shortcutLiveData = ShortcutCreator(
            requireContext(),
            currentShowTitle,
            currentPosterPath,
            currentShowTvdbId
        )
        viewLifecycleOwner.lifecycleScope.launch {
            whenStarted {
                shortcutLiveData.prepareAndPinShortcut()
            }
        }
    }

    private fun shareShow() {
        val currentShowSlug = showSlug
        val currentShowTvdbId = showTvdbId
        val currentShowTitle = showTitle
        if (currentShowSlug != null && currentShowTvdbId != 0 && currentShowTitle != null) {
            ShareUtils.shareShow(activity, currentShowSlug, currentShowTvdbId, currentShowTitle)
        }
    }

    companion object {

        const val ARG_SHOW_TVDBID = "tvdbid"

        @JvmStatic
        fun newInstance(showTvdbId: Int): ShowFragment {
            val f = ShowFragment()

            val args = Bundle()
            args.putInt(ARG_SHOW_TVDBID, showTvdbId)
            f.arguments = args

            return f
        }
    }
}
