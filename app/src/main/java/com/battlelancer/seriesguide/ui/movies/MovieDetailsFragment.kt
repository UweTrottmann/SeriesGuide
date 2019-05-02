package com.battlelancer.seriesguide.ui.movies

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.SparseArrayCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.palette.graphics.Palette
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.extensions.ActionsHelper
import com.battlelancer.seriesguide.extensions.ExtensionManager
import com.battlelancer.seriesguide.extensions.MovieActionsContract
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.streaming.StreamingSearchConfigureDialog
import com.battlelancer.seriesguide.traktapi.MovieCheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity
import com.battlelancer.seriesguide.ui.FullscreenImageActivity
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.ui.comments.TraktCommentsActivity
import com.battlelancer.seriesguide.ui.dialogs.LanguageChoiceDialogFragment
import com.battlelancer.seriesguide.ui.people.MovieCreditsLoader
import com.battlelancer.seriesguide.ui.people.PeopleListHelper
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.TmdbTools
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.androidutils.CheatSheet
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.Videos
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.util.ArrayList

/**
 * Displays details about one movie including plot, ratings, trailers and a poster.
 */
class MovieDetailsFragment : Fragment(), MovieActionsContract {

    private lateinit var unbinder: Unbinder
    @BindView(R.id.rootLayoutMovie)
    lateinit var rootLayoutMovie: FrameLayout
    @BindView(R.id.progressBar)
    lateinit var progressBar: View
    @BindView(R.id.containerMovieButtons)
    lateinit var containerMovieButtons: View
    @BindView(R.id.dividerMovieButtons)
    lateinit var dividerMovieButtons: View
    @BindView(R.id.buttonMovieCheckIn)
    lateinit var buttonMovieCheckIn: Button
    @BindView(R.id.buttonMovieStreamingSearch)
    lateinit var buttonMovieStreamingSearch: Button
    @BindView(R.id.buttonMovieWatched)
    lateinit var buttonMovieWatched: Button
    @BindView(R.id.buttonMovieCollected)
    lateinit var buttonMovieCollected: Button
    @BindView(R.id.buttonMovieWatchlisted)
    lateinit var buttonMovieWatchlisted: Button
    @BindView(R.id.containerRatings)
    lateinit var containerRatings: View
    @BindView(R.id.textViewRatingsTmdbValue)
    lateinit var textViewRatingsTmdbValue: TextView
    @BindView(R.id.textViewRatingsTmdbVotes)
    lateinit var textViewRatingsTmdbVotes: TextView
    @BindView(R.id.textViewRatingsTraktVotes)
    lateinit var textViewRatingsTraktVotes: TextView
    @BindView(R.id.textViewRatingsTraktValue)
    lateinit var textViewRatingsTraktValue: TextView
    @BindView(R.id.textViewRatingsTraktUserLabel)
    lateinit var textViewRatingsTraktUserLabel: TextView
    @BindView(R.id.textViewRatingsTraktUser)
    lateinit var textViewRatingsTraktUser: TextView
    @BindView(R.id.contentContainerMovie)
    lateinit var contentContainerMovie: NestedScrollView
    @BindView(R.id.contentContainerMovieRight)
    @JvmField
    var contentContainerMovieRight: NestedScrollView? = null
    @BindView(R.id.frameLayoutMoviePoster)
    lateinit var frameLayoutMoviePoster: FrameLayout
    @BindView(R.id.imageViewMoviePoster)
    lateinit var imageViewMoviePoster: ImageView
    @BindView(R.id.textViewMovieTitle)
    lateinit var textViewMovieTitle: TextView
    @BindView(R.id.textViewMovieDescription)
    lateinit var textViewMovieDescription: TextView
    @BindView(R.id.textViewMovieDate)
    lateinit var textViewMovieDate: TextView
    @BindView(R.id.textViewMovieGenresLabel)
    lateinit var textViewMovieGenresLabel: View
    @BindView(R.id.textViewMovieGenres)
    lateinit var textViewMovieGenres: TextView
    @BindView(R.id.containerCast)
    lateinit var containerCast: ViewGroup
    @BindView(R.id.labelCast)
    lateinit var labelCast: View
    @BindView(R.id.containerCrew)
    lateinit var containerCrew: ViewGroup
    @BindView(R.id.labelCrew)
    lateinit var labelCrew: View
    @BindView(R.id.buttonMovieLanguage)
    lateinit var buttonMovieLanguage: Button
    @BindView(R.id.buttonMovieComments)
    lateinit var buttonMovieComments: Button
    @BindView(R.id.containerMovieActions)
    lateinit var containerMovieActions: ViewGroup

    private var tmdbId: Int = 0
    private var movieDetails: MovieDetails? = MovieDetails()
    private var movieTitle: String? = null
    private var trailer: Videos.Video? = null
    private var languageCode: String? = null

    private val handler = Handler()
    private var paletteAsyncTask: AsyncTask<Bitmap, Void, Palette>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_movie, container, false)
        unbinder = ButterKnife.bind(this, view)

        progressBar.isGone = true
        textViewMovieGenresLabel.isGone = true

        // important action buttons
        containerMovieButtons.isGone = true
        containerRatings.isGone = true
        val theme = activity!!.theme
        ViewTools.setVectorIconTop(theme, buttonMovieWatched, R.drawable.ic_watch_black_24dp)
        ViewTools.setVectorIconTop(theme, buttonMovieCollected, R.drawable.ic_collect_black_24dp)
        ViewTools.setVectorIconTop(theme, buttonMovieWatchlisted, R.drawable.ic_list_add_white_24dp)
        ViewTools.setVectorIconLeft(theme, buttonMovieCheckIn, R.drawable.ic_checkin_black_24dp)
        ViewTools.setVectorIconLeft(
            theme,
            buttonMovieStreamingSearch,
            R.drawable.ic_play_arrow_black_24dp
        )
        CheatSheet.setup(buttonMovieCheckIn)

        // language button
        buttonMovieLanguage.isGone = true
        ViewTools.setVectorIconLeft(theme, buttonMovieLanguage, R.drawable.ic_language_white_24dp)
        CheatSheet.setup(buttonMovieLanguage, R.string.pref_language)
        buttonMovieLanguage.setOnClickListener { displayLanguageSettings() }

        // comments button
        buttonMovieComments.isGone = true
        ViewTools.setVectorIconLeft(theme, buttonMovieComments, R.drawable.ic_forum_black_24dp)

        // cast and crew
        setCastVisibility(false)
        setCrewVisibility(false)

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        textViewMovieTitle.copyTextToClipboardOnLongClick()
        textViewMovieDate.copyTextToClipboardOnLongClick()
        textViewMovieDescription.copyTextToClipboardOnLongClick()
        textViewMovieGenres.copyTextToClipboardOnLongClick()

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tmdbId = arguments!!.getInt(ARG_TMDB_ID)
        if (tmdbId <= 0) {
            fragmentManager!!.popBackStack()
            return
        }

        setupViews()

        val args = Bundle()
        args.putInt(ARG_TMDB_ID, tmdbId)
        LoaderManager.getInstance(this).apply {
            initLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args, movieLoaderCallbacks)
            initLoader<Videos.Video>(
                MovieDetailsActivity.LOADER_ID_MOVIE_TRAILERS, args, trailerLoaderCallbacks
            )
            initLoader(MovieDetailsActivity.LOADER_ID_MOVIE_CREDITS, args, creditsLoaderCallbacks)
        }

        setHasOptionsMenu(true)
    }

    private fun setupViews() {
        // avoid overlap with status + action bar (adjust top margin)
        // warning: pre-M status bar not always translucent (e.g. Nexus 10)
        // (using fitsSystemWindows would not work correctly with multiple views)
        val config = (activity as MovieDetailsActivity).systemBarTintManager.config
        val pixelInsetTop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            config.statusBarHeight // full screen, status bar transparent
        } else {
            config.getPixelInsetTop(false) // status bar translucent
        }

        // action bar height is pre-set as top margin, add to it
        val decorationHeightPx = pixelInsetTop + contentContainerMovie.paddingTop
        contentContainerMovie.setPadding(0, decorationHeightPx, 0, 0)

        // dual pane layout?
        contentContainerMovieRight?.setPadding(0, decorationHeightPx, 0, 0)

        // show toolbar title and background when scrolling
        val defaultPaddingPx = resources.getDimensionPixelSize(R.dimen.default_padding)
        val scrollChangeListener = ToolbarScrollChangeListener(defaultPaddingPx, decorationHeightPx)
        contentContainerMovie.setOnScrollChangeListener(scrollChangeListener)
        contentContainerMovieRight?.setOnScrollChangeListener(scrollChangeListener)
    }

    override fun onStart() {
        super.onStart()

        val event = EventBus.getDefault()
            .getStickyEvent(BaseNavDrawerActivity.ServiceActiveEvent::class.java)
        setMovieButtonsEnabled(event == null)

        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()

        // refresh actions when returning, enabled extensions or their actions might have changed
        loadMovieActionsDelayed()
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the fragment from
        // being garbage collected. It also prevents our callback from getting invoked even after the
        // fragment is destroyed.
        Picasso.get().cancelRequest(imageViewMoviePoster)
        // same for Palette task
        paletteAsyncTask?.cancel(true)

        unbinder.unbind()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        movieDetails?.let {
            // choose theme variant
            val isLightTheme = SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light
            inflater.inflate(
                if (isLightTheme) R.menu.movie_details_menu_light else R.menu.movie_details_menu,
                menu
            )

            // enable/disable actions
            val isEnableShare = !it.tmdbMovie()?.title.isNullOrEmpty()
            menu.findItem(R.id.menu_movie_share).apply {
                isEnabled = isEnableShare
                isVisible = isEnableShare
            }

            val isEnableImdb = !it.tmdbMovie()?.imdb_id.isNullOrEmpty()
            menu.findItem(R.id.menu_open_imdb).apply {
                isEnabled = isEnableImdb
                isVisible = isEnableImdb
            }

            val isEnableYoutube = trailer != null
            menu.findItem(R.id.menu_open_youtube).apply {
                isEnabled = isEnableYoutube
                isVisible = isEnableYoutube
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_movie_share) {
            movieDetails?.tmdbMovie()?.let { ShareUtils.shareMovie(activity, tmdbId, it.title) }
            return true
        }
        if (itemId == R.id.menu_open_imdb) {
            movieDetails?.tmdbMovie()?.let { ServiceUtils.openImdb(it.imdb_id, activity) }
            return true
        }
        if (itemId == R.id.menu_open_youtube) {
            trailer?.let { ServiceUtils.openYoutube(it.key, activity) }
            return true
        }
        if (itemId == R.id.menu_open_tmdb) {
            TmdbTools.openTmdbMovie(activity, tmdbId)
        }
        if (itemId == R.id.menu_open_trakt) {
            Utils.launchWebsite(activity, TraktTools.buildMovieUrl(tmdbId))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun populateMovieViews() {
        val movieDetails = this.movieDetails ?: return
        /*
          Get everything from TMDb. Also get additional rating from trakt.
         */
        val tmdbMovie = movieDetails.tmdbMovie() ?: return
        val traktRatings = movieDetails.traktRatings()
        val inCollection = movieDetails.isInCollection
        val inWatchlist = movieDetails.isInWatchlist
        val isWatched = movieDetails.isWatched
        val rating = movieDetails.userRating

        movieTitle = tmdbMovie.title
        textViewMovieTitle.text = tmdbMovie.title
        activity!!.title = tmdbMovie.title
        textViewMovieDescription.text = TextTools.textWithTmdbSource(
            textViewMovieDescription.context,
            tmdbMovie.overview
        )

        // release date and runtime: "July 17, 2009 | 95 min"
        val releaseAndRuntime = StringBuilder()
        tmdbMovie.release_date?.let {
            releaseAndRuntime.append(TimeTools.formatToLocalDate(context, it))
            releaseAndRuntime.append(" | ")
        }
        tmdbMovie.runtime?.let {
            releaseAndRuntime.append(getString(R.string.runtime_minutes, it.toString()))
        }
        textViewMovieDate.text = releaseAndRuntime.toString()

        // hide check-in if not connected to trakt or hexagon is enabled
        val isConnectedToTrakt = TraktCredentials.get(activity).hasCredentials()
        val hideCheckIn = !isConnectedToTrakt || HexagonSettings.isEnabled(activity)
        buttonMovieCheckIn.isGone = hideCheckIn
        // hide streaming search if turned off
        val hideStreamingSearch = StreamingSearch.isTurnedOff(requireContext())
        buttonMovieStreamingSearch.isGone = hideStreamingSearch
        dividerMovieButtons.isGone = hideCheckIn && hideStreamingSearch

        // watched button
        val theme = activity!!.theme
        buttonMovieWatched.also {
            val textRes = if (isWatched) R.string.action_unwatched else R.string.action_watched
            it.setText(textRes)
            CheatSheet.setup(it, textRes)
            if (isWatched) {
                ViewTools.setVectorDrawableTop(theme, it, R.drawable.ic_watched_24dp)
            } else {
                ViewTools.setVectorIconTop(theme, it, R.drawable.ic_watch_black_24dp)
            }
            it.setOnClickListener {
                if (isWatched) {
                    MovieTools.unwatchedMovie(context, tmdbId)
                } else {
                    MovieTools.watchedMovie(context, tmdbId, inWatchlist)
                }
            }
        }

        // collected button
        buttonMovieCollected.also {
            if (inCollection) {
                ViewTools.setVectorDrawableTop(theme, it, R.drawable.ic_collected_24dp)
            } else {
                ViewTools.setVectorIconTop(theme, it, R.drawable.ic_collect_black_24dp)
            }
            val textRes =
                if (inCollection) R.string.action_collection_remove else R.string.action_collection_add
            it.setText(textRes)
            CheatSheet.setup(it, textRes)
            it.setOnClickListener {
                if (inCollection) {
                    MovieTools.removeFromCollection(context, tmdbId)
                } else {
                    MovieTools.addToCollection(context, tmdbId)
                }
            }
        }

        // watchlist button
        buttonMovieWatchlisted.also {
            if (inWatchlist) {
                ViewTools.setVectorDrawableTop(theme, it, R.drawable.ic_list_added_24dp)
            } else {
                ViewTools.setVectorIconTop(theme, it, R.drawable.ic_list_add_white_24dp)
            }
            val textRes = if (inWatchlist) R.string.watchlist_remove else R.string.watchlist_add
            it.setText(textRes)
            CheatSheet.setup(it, textRes)
            it.setOnClickListener {
                if (inWatchlist) {
                    MovieTools.removeFromWatchlist(context, tmdbId)
                } else {
                    MovieTools.addToWatchlist(context, tmdbId)
                }
            }
        }

        // show button bar
        containerMovieButtons.isGone = false

        // language button
        buttonMovieLanguage.also {
            val languageData = LanguageTools.getMovieLanguageData(context)
            if (languageData != null) languageCode = languageData.languageCode
            it.text = languageData?.languageString
            it.isGone = false
        }

        // ratings
        textViewRatingsTmdbValue.text = TraktTools.buildRatingString(tmdbMovie.vote_average)
        textViewRatingsTmdbVotes.text =
            TraktTools.buildRatingVotesString(activity, tmdbMovie.vote_count)
        traktRatings?.let {
            textViewRatingsTraktVotes.text =
                TraktTools.buildRatingVotesString(activity, it.votes)
            textViewRatingsTraktValue.text = TraktTools.buildRatingString(it.rating)
        }
        // if movie is not in database, can't handle user ratings
        if (!inCollection && !inWatchlist && !isWatched) {
            textViewRatingsTraktUserLabel.isGone = true
            textViewRatingsTraktUser.isGone = true
            containerRatings.isClickable = false
            containerRatings.isLongClickable = false // cheat sheet
        } else {
            textViewRatingsTraktUserLabel.isGone = false
            textViewRatingsTraktUser.isGone = false
            textViewRatingsTraktUser.text = TraktTools.buildUserRatingString(activity, rating)
            containerRatings.setOnClickListener { rateMovie() }
            CheatSheet.setup(containerRatings, R.string.action_rate)
        }
        containerRatings.isGone = false

        // genres
        textViewMovieGenresLabel.isGone = false
        ViewTools.setValueOrPlaceholder(
            textViewMovieGenres, TmdbTools.buildGenresString(tmdbMovie.genres)
        )

        // trakt comments link
        buttonMovieComments.setOnClickListener { v ->
            val i = Intent(activity, TraktCommentsActivity::class.java)
            i.putExtras(TraktCommentsActivity.createInitBundleMovie(movieTitle, tmdbId))
            Utils.startActivityWithAnimation(activity, i, v)
        }
        buttonMovieComments.isGone = false

        // load poster, cache on external storage
        if (tmdbMovie.poster_path.isNullOrEmpty()) {
            frameLayoutMoviePoster.isClickable = false
            frameLayoutMoviePoster.isFocusable = false
        } else {
            val smallImageUrl = (TmdbSettings.getImageBaseUrl(activity)
                    + TmdbSettings.POSTER_SIZE_SPEC_W342 + tmdbMovie.poster_path)
            ServiceUtils.loadWithPicasso(activity, smallImageUrl)
                .into(imageViewMoviePoster, object : Callback.EmptyCallback() {
                    override fun onSuccess() {
                        val bitmap = (imageViewMoviePoster.drawable as BitmapDrawable).bitmap
                        paletteAsyncTask = Palette.from(bitmap)
                            .generate { palette ->
                                if (palette != null) {
                                    var color = palette.getVibrantColor(Color.WHITE)
                                    color = ColorUtils.setAlphaComponent(color, 50)
                                    rootLayoutMovie.setBackgroundColor(color)
                                }
                            }
                    }
                })
            // click listener for high resolution poster
            frameLayoutMoviePoster.also {
                it.isFocusable = true
                it.setOnClickListener { view ->
                    val largeImageUrl = (TmdbSettings.getImageBaseUrl(activity)
                            + TmdbSettings.POSTER_SIZE_SPEC_ORIGINAL + tmdbMovie.poster_path)
                    val intent = Intent(activity, FullscreenImageActivity::class.java).apply {
                        putExtra(FullscreenImageActivity.EXTRA_PREVIEW_IMAGE, smallImageUrl)
                        putExtra(FullscreenImageActivity.EXTRA_IMAGE, largeImageUrl)
                    }
                    Utils.startActivityWithAnimation(activity, intent, view)
                }
            }
        }
    }

    private fun populateMovieCreditsViews(credits: Credits?) {
        if (credits == null) {
            setCastVisibility(false)
            setCrewVisibility(false)
            return
        }

        // cast members
        if (credits.cast?.size != 0
            && PeopleListHelper.populateMovieCast(activity, containerCast, credits)) {
            setCastVisibility(true)
        } else {
            setCastVisibility(false)
        }

        // crew members
        if (credits.crew?.size != 0
            && PeopleListHelper.populateMovieCrew(activity, containerCrew, credits)) {
            setCrewVisibility(true)
        } else {
            setCrewVisibility(false)
        }
    }

    @OnClick(R.id.buttonMovieCheckIn)
    fun onButtonCheckInClick() {
        movieTitle?.let {
            if (it.isEmpty()) {
                return
            }
            MovieCheckInDialogFragment.show(fragmentManager, tmdbId, it)
        }
    }

    @OnClick(R.id.buttonMovieStreamingSearch)
    fun onButtonStreamingSearchClick() {
        movieTitle?.let {
            if (it.isEmpty()) {
                return
            }
            if (StreamingSearch.isNotConfigured(requireContext())) {
                showStreamingSearchConfigDialog()
            } else {
                StreamingSearch.searchForMovie(requireContext(), it)
            }
        }
    }

    @OnLongClick(R.id.buttonMovieStreamingSearch)
    fun onButtonStreamingSearchLongClick(): Boolean {
        showStreamingSearchConfigDialog()
        return true
    }

    private fun showStreamingSearchConfigDialog() {
        StreamingSearchConfigureDialog.show(requireFragmentManager())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStreamingSearchConfigured(
        event: StreamingSearchConfigureDialog.StreamingSearchConfiguredEvent
    ) {
        if (event.turnedOff) {
            buttonMovieStreamingSearch.isGone = true
        } else {
            onButtonStreamingSearchClick()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onEventMainThread(event: ExtensionManager.MovieActionReceivedEvent) {
        if (event.movieTmdbId != tmdbId) {
            return
        }
        loadMovieActionsDelayed()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MovieTools.MovieChangedEvent) {
        if (event.movieTmdbId != tmdbId) {
            return
        }
        // re-query to update movie details
        restartMovieLoader()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventEpisodeTask(@Suppress("UNUSED_PARAMETER") event: BaseNavDrawerActivity.ServiceActiveEvent) {
        setMovieButtonsEnabled(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventEpisodeTask(@Suppress("UNUSED_PARAMETER") event: BaseNavDrawerActivity.ServiceCompletedEvent) {
        setMovieButtonsEnabled(true)
    }

    private fun setMovieButtonsEnabled(enabled: Boolean) {
        buttonMovieCheckIn.isEnabled = enabled
        buttonMovieWatched.isEnabled = enabled
        buttonMovieCollected.isEnabled = enabled
        buttonMovieWatchlisted.isEnabled = enabled
        buttonMovieStreamingSearch.isEnabled = enabled
    }

    private fun displayLanguageSettings() {
        LanguageChoiceDialogFragment.show(
            fragmentManager!!, R.array.languageCodesMovies, languageCode, "movieLanguageDialog"
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleLanguageEvent(event: LanguageChoiceDialogFragment.LanguageChangedEvent) {
        if (!AndroidUtils.isNetworkConnected(context)) {
            Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show()
            return
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(DisplaySettings.KEY_MOVIES_LANGUAGE, event.selectedLanguageCode)
        }

        // reload movie details and trailers (but not cast/crew info which is not language dependent)
        restartMovieLoader()
        val args = Bundle().apply {
            putInt(ARG_TMDB_ID, tmdbId)
        }
        LoaderManager.getInstance(this).restartLoader<Videos.Video>(
            MovieDetailsActivity.LOADER_ID_MOVIE_TRAILERS, args, trailerLoaderCallbacks
        )
    }

    override fun loadMovieActions() {
        var actions = ExtensionManager.get()
            .getLatestMovieActions(context, tmdbId)

        // no actions available yet, request extensions to publish them
        if (actions == null || actions.size == 0) {
            actions = ArrayList()

            movieDetails?.tmdbMovie()?.let {
                val movie = com.battlelancer.seriesguide.api.Movie.Builder()
                    .tmdbId(tmdbId)
                    .imdbId(it.imdb_id)
                    .title(it.title)
                    .releaseDate(it.release_date)
                    .build()
                ExtensionManager.get().requestMovieActions(context, movie)
            }
        }

        Timber.d("loadMovieActions: received %s actions for %s", actions.size, tmdbId)
        ActionsHelper.populateActions(
            activity!!.layoutInflater, activity!!.theme, containerMovieActions, actions
        )
    }

    private val movieActionsRunnable = Runnable {
        if (!isAdded) {
            return@Runnable // we need an activity for this, abort.
        }
        loadMovieActions()
    }

    override fun loadMovieActionsDelayed() {
        handler.removeCallbacks(movieActionsRunnable)
        handler.postDelayed(
            movieActionsRunnable, MovieActionsContract.ACTION_LOADER_DELAY_MILLIS.toLong()
        )
    }

    private fun rateMovie() {
        RateDialogFragment.newInstanceMovie(tmdbId).safeShow(context, fragmentManager)
    }

    private fun setCrewVisibility(visible: Boolean) {
        labelCrew.isGone = !visible
        containerCrew.isGone = !visible
    }

    private fun setCastVisibility(visible: Boolean) {
        labelCast.isGone = !visible
        containerCast.isGone = !visible
    }

    private fun restartMovieLoader() {
        val args = Bundle()
        args.putInt(ARG_TMDB_ID, tmdbId)
        LoaderManager.getInstance(this)
            .restartLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args, movieLoaderCallbacks)
    }

    private val movieLoaderCallbacks = object : LoaderManager.LoaderCallbacks<MovieDetails> {
        override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<MovieDetails> {
            progressBar.isGone = false
            return MovieLoader(context, args!!.getInt(ARG_TMDB_ID))
        }

        override fun onLoadFinished(movieLoader: Loader<MovieDetails>, movieDetails: MovieDetails) {
            if (!isAdded) {
                return
            }
            this@MovieDetailsFragment.movieDetails = movieDetails
            progressBar.isGone = true

            // we need at least values from database or tmdb
            if (movieDetails.tmdbMovie() != null) {
                populateMovieViews()
                loadMovieActions()
                activity!!.invalidateOptionsMenu()
            } else {
                // if there is no local data and loading from network failed
                textViewMovieDescription.text = if (AndroidUtils.isNetworkConnected(context)) {
                    getString(R.string.api_error_generic, getString(R.string.tmdb))
                } else {
                    getString(R.string.offline)
                }
            }
        }

        override fun onLoaderReset(movieLoader: Loader<MovieDetails>) {
            // nothing to do
        }
    }

    private val trailerLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Videos.Video> {
        override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Videos.Video> {
            return MovieTrailersLoader(context, args!!.getInt(ARG_TMDB_ID))
        }

        override fun onLoadFinished(
            trailersLoader: Loader<Videos.Video>,
            trailer: Videos.Video?
        ) {
            if (!isAdded) {
                return
            }
            if (trailer != null) {
                this@MovieDetailsFragment.trailer = trailer
                activity!!.invalidateOptionsMenu()
            }
        }

        override fun onLoaderReset(trailersLoader: Loader<Videos.Video>) {
            // do nothing
        }
    }

    private val creditsLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Credits?> {
        override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Credits?> {
            return MovieCreditsLoader(context!!, args!!.getInt(ARG_TMDB_ID))
        }

        override fun onLoadFinished(creditsLoader: Loader<Credits?>, credits: Credits?) {
            if (!isAdded) {
                return
            }
            populateMovieCreditsViews(credits)
        }

        override fun onLoaderReset(creditsLoader: Loader<Credits?>) {
            // do nothing
        }
    }

    private inner class ToolbarScrollChangeListener(
        private val overlayThresholdPx: Int,
        private val titleThresholdPx: Int
    ) : NestedScrollView.OnScrollChangeListener {

        // we have determined by science that a capacity of 2 is good in our case :)
        private val showOverlayMap: SparseArrayCompat<Boolean> = SparseArrayCompat(2)
        private var showOverlay: Boolean = false
        private var showTitle: Boolean = false

        override fun onScrollChange(
            v: NestedScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int
        ) {
            val actionBar = (activity as AppCompatActivity?)?.supportActionBar ?: return

            val viewId = v.id

            var shouldShowOverlayTemp = scrollY > overlayThresholdPx
            showOverlayMap.put(viewId, shouldShowOverlayTemp)
            for (i in 0 until showOverlayMap.size()) {
                shouldShowOverlayTemp = shouldShowOverlayTemp or showOverlayMap.valueAt(i)
            }
            val shouldShowOverlay = shouldShowOverlayTemp

            if (!showOverlay && shouldShowOverlay) {
                val primaryColor = ContextCompat.getColor(
                    v.context,
                    Utils.resolveAttributeToResourceId(
                        v.context.theme, R.attr.sgColorStatusBarOverlay
                    )
                )
                actionBar.setBackgroundDrawable(ColorDrawable(primaryColor))
            } else if (showOverlay && !shouldShowOverlay) {
                actionBar.setBackgroundDrawable(null)
            }
            showOverlay = shouldShowOverlay

            // only main container should show/hide title
            if (viewId == R.id.contentContainerMovie) {
                val shouldShowTitle = scrollY > titleThresholdPx
                if (!showTitle && shouldShowTitle) {
                    movieDetails?.tmdbMovie()?.let {
                        actionBar.title = it.title
                        actionBar.setDisplayShowTitleEnabled(true)
                    }
                } else if (showTitle && !shouldShowTitle) {
                    actionBar.setDisplayShowTitleEnabled(false)
                }
                showTitle = shouldShowTitle
            }
        }
    }

    companion object {

        const val ARG_TMDB_ID = "tmdbid"

        @JvmStatic
        fun newInstance(tmdbId: Int): MovieDetailsFragment {
            return MovieDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TMDB_ID, tmdbId)
                }
            }
        }
    }
}
