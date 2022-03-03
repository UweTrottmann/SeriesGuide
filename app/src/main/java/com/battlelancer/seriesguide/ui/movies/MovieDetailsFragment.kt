package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.collection.SparseArrayCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.palette.graphics.Palette
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.FragmentMovieBinding
import com.battlelancer.seriesguide.extensions.ActionsHelper
import com.battlelancer.seriesguide.extensions.ExtensionManager
import com.battlelancer.seriesguide.extensions.MovieActionsContract
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.traktapi.MovieCheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.FullscreenImageActivity
import com.battlelancer.seriesguide.ui.comments.TraktCommentsActivity
import com.battlelancer.seriesguide.ui.people.PeopleListHelper
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.Metacritic
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
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.Videos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.util.ArrayList

/**
 * Displays details about one movie including plot, ratings, trailers and a poster.
 */
class MovieDetailsFragment : Fragment(), MovieActionsContract {

    private var _binding: FragmentMovieBinding? = null
    private val binding get() = _binding!!

    private var tmdbId: Int = 0
    private var movieDetails: MovieDetails? = MovieDetails()
    private var movieTitle: String? = null
    private var trailer: Videos.Video? = null
    private val model: MovieDetailsModel by viewModels {
        MovieDetailsModelFactory(tmdbId, requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.progressBar.isGone = true
        binding.textViewMovieGenresLabel.isGone = true

        // trailer button
        binding.buttonMovieTrailer.setOnClickListener {
            trailer?.let { ServiceUtils.openYoutube(it.key, activity) }
        }
        binding.buttonMovieTrailer.isGone = true
        binding.buttonMovieTrailer.isEnabled = false

        // important action buttons
        binding.containerMovieButtons.root.isGone = true
        binding.containerMovieButtons.buttonMovieCheckIn.setOnClickListener { onButtonCheckInClick() }
        StreamingSearch.initButtons(
            binding.containerMovieButtons.buttonMovieStreamingSearch,
            binding.containerMovieButtons.buttonMovieStreamingSearchInfo,
            parentFragmentManager
        )
        binding.containerRatings.root.isGone = true
        TooltipCompat.setTooltipText(
            binding.containerMovieButtons.buttonMovieCheckIn,
            binding.containerMovieButtons.buttonMovieCheckIn.contentDescription
        )

        // language button
        binding.buttonMovieLanguage.isGone = true
        TooltipCompat.setTooltipText(
            binding.buttonMovieLanguage,
            binding.buttonMovieLanguage.context.getString(R.string.pref_language)
        )
        binding.buttonMovieLanguage.setOnClickListener {
            MovieLocalizationDialogFragment.show(parentFragmentManager)
        }

        // comments button
        binding.buttonMovieComments.isGone = true

        // cast and crew
        setCastVisibility(false)
        setCrewVisibility(false)

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        binding.textViewMovieTitle.copyTextToClipboardOnLongClick()
        binding.textViewMovieDate.copyTextToClipboardOnLongClick()
        binding.textViewMovieDescription.copyTextToClipboardOnLongClick()
        binding.textViewMovieGenres.copyTextToClipboardOnLongClick()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tmdbId = requireArguments().getInt(ARG_TMDB_ID)
        if (tmdbId <= 0) {
            parentFragmentManager.popBackStack()
            return
        }

        setupViews()

        val args = Bundle()
        args.putInt(ARG_TMDB_ID, tmdbId)
        LoaderManager.getInstance(this).apply {
            initLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args, movieLoaderCallbacks)
            initLoader(
                MovieDetailsActivity.LOADER_ID_MOVIE_TRAILERS, args, trailerLoaderCallbacks
            )
        }
        model.credits.observe(viewLifecycleOwner, {
            populateMovieCreditsViews(it)
        })
        model.watchProvider.observe(viewLifecycleOwner, { watchInfo ->
            StreamingSearch.configureButton(
                binding.containerMovieButtons.buttonMovieStreamingSearch,
                watchInfo
            )
        })

        setHasOptionsMenu(true)
    }

    private fun setupViews() {
        // avoid overlap with status + action bar (adjust top margin)
        // warning: pre-M status bar not always translucent (e.g. Nexus 10)
        // (using fitsSystemWindows would not work correctly with multiple views)
        val config = (activity as MovieDetailsActivity).systemBarTintManager.config
        val pixelInsetTop = if (AndroidUtils.isMarshmallowOrHigher) {
            config.statusBarHeight // full screen, status bar transparent
        } else {
            config.getPixelInsetTop(false) // status bar translucent
        }

        // action bar height is pre-set as top margin, add to it
        val decorationHeightPx = pixelInsetTop + binding.contentContainerMovie.paddingTop
        binding.contentContainerMovie.setPadding(0, decorationHeightPx, 0, 0)

        // dual pane layout?
        binding.contentContainerMovieRight?.setPadding(0, decorationHeightPx, 0, 0)

        // show toolbar title and background when scrolling
        val defaultPaddingPx = resources.getDimensionPixelSize(R.dimen.default_padding)
        val scrollChangeListener = ToolbarScrollChangeListener(defaultPaddingPx, decorationHeightPx)
        binding.contentContainerMovie.setOnScrollChangeListener(scrollChangeListener)
        binding.contentContainerMovieRight?.setOnScrollChangeListener(scrollChangeListener)
    }

    override fun onStart() {
        super.onStart()

        val event = EventBus.getDefault()
            .getStickyEvent(BaseMessageActivity.ServiceActiveEvent::class.java)
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
        Picasso.get().cancelRequest(binding.imageViewMoviePoster)

        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        movieDetails?.let {
            inflater.inflate(R.menu.movie_details_menu, menu)

            // enable/disable actions
            val hasTitle = !it.tmdbMovie()?.title.isNullOrEmpty()
            menu.findItem(R.id.menu_movie_share).apply {
                isEnabled = hasTitle
                isVisible = hasTitle
            }
            menu.findItem(R.id.menu_open_metacritic).apply {
                isEnabled = hasTitle
                isVisible = hasTitle
            }

            val isEnableImdb = !it.tmdbMovie()?.imdb_id.isNullOrEmpty()
            menu.findItem(R.id.menu_open_imdb).apply {
                isEnabled = isEnableImdb
                isVisible = isEnableImdb
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_movie_share -> {
                movieDetails?.tmdbMovie()
                    ?.title
                    ?.let { ShareUtils.shareMovie(activity, tmdbId, it) }
                true
            }
            R.id.menu_open_imdb -> {
                movieDetails?.tmdbMovie()
                    ?.let { ServiceUtils.openImdb(it.imdb_id, activity) }
                true
            }
            R.id.menu_open_metacritic -> {
                // Metacritic only has English titles, so using the original title is the best bet.
                val titleOrNull = movieDetails?.tmdbMovie()
                    ?.let { it.original_title ?: it.title }
                titleOrNull?.let { Metacritic.searchForMovie(requireContext(), it) }
                true
            }
            R.id.menu_open_tmdb -> {
                TmdbTools.openTmdbMovie(activity, tmdbId)
                true
            }
            R.id.menu_open_trakt -> {
                Utils.launchWebsite(activity, TraktTools.buildMovieUrl(tmdbId))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        val plays = movieDetails.plays
        val rating = movieDetails.userRating

        movieTitle = tmdbMovie.title
        binding.textViewMovieTitle.text = tmdbMovie.title
        requireActivity().title = tmdbMovie.title
        binding.textViewMovieDescription.text = TextTools.textWithTmdbSource(
            binding.textViewMovieDescription.context,
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
        binding.textViewMovieDate.text = releaseAndRuntime.toString()

        // show trailer button (but trailer is loaded separately, just for animation)
        binding.buttonMovieTrailer.isGone = false

        // hide check-in if not connected to trakt or hexagon is enabled
        val isConnectedToTrakt = TraktCredentials.get(requireContext()).hasCredentials()
        val hideCheckIn = !isConnectedToTrakt || HexagonSettings.isEnabled(requireContext())
        binding.containerMovieButtons.buttonMovieCheckIn.isGone = hideCheckIn

        // watched button
        binding.containerMovieButtons.buttonMovieWatched.also {
            it.text = TextTools.getWatchedButtonText(requireContext(), isWatched, plays)
            TooltipCompat.setTooltipText(
                it, it.context.getString(
                    if (isWatched) R.string.action_unwatched else R.string.action_watched
                )
            )
            if (isWatched) {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_watched_24dp)
            } else {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_watch_black_24dp)
            }
            it.setOnClickListener { view ->
                if (isWatched) {
                    PopupMenu(view.context, view)
                        .apply {
                            inflate(R.menu.watched_popup_menu)
                            setOnMenuItemClickListener(
                                WatchedPopupMenuListener(
                                    requireContext(),
                                    tmdbId,
                                    plays,
                                    inWatchlist
                                )
                            )
                        }
                        .show()
                } else {
                    MovieTools.watchedMovie(context, tmdbId, plays, inWatchlist)
                }
            }
        }

        // collected button
        binding.containerMovieButtons.buttonMovieCollected.also {
            if (inCollection) {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_collected_24dp)
            } else {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_collect_black_24dp)
            }
            it.setText(
                if (inCollection) R.string.state_in_collection else R.string.action_collection_add
            )
            TooltipCompat.setTooltipText(
                it,
                it.context.getString(
                    if (inCollection) R.string.action_collection_remove else R.string.action_collection_add
                )
            )
            it.setOnClickListener {
                if (inCollection) {
                    MovieTools.removeFromCollection(context, tmdbId)
                } else {
                    MovieTools.addToCollection(context, tmdbId)
                }
            }
        }

        // watchlist button
        binding.containerMovieButtons.buttonMovieWatchlisted.also {
            if (inWatchlist) {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_list_added_24dp)
            } else {
                ViewTools.setVectorDrawableTop(it, R.drawable.ic_list_add_white_24dp)
            }
            it.setText(
                if (inWatchlist) R.string.state_on_watchlist else R.string.watchlist_add
            )
            TooltipCompat.setTooltipText(
                it, it.context.getString(
                    if (inWatchlist) R.string.watchlist_remove else R.string.watchlist_add
                )
            )
            it.setOnClickListener {
                if (inWatchlist) {
                    MovieTools.removeFromWatchlist(context, tmdbId)
                } else {
                    MovieTools.addToWatchlist(context, tmdbId)
                }
            }
        }

        // show button bar
        binding.containerMovieButtons.root.isGone = false

        // language button
        binding.buttonMovieLanguage.also {
            it.text = LanguageTools.getMovieLanguageStringFor(requireContext(), null)
            it.isGone = false
        }

        // ratings
        binding.containerRatings.textViewRatingsTmdbValue.text = TraktTools.buildRatingString(
            tmdbMovie.vote_average
        )
        binding.containerRatings.textViewRatingsTmdbVotes.text =
            TraktTools.buildRatingVotesString(activity, tmdbMovie.vote_count)
        traktRatings?.let {
            binding.containerRatings.textViewRatingsTraktVotes.text =
                TraktTools.buildRatingVotesString(activity, it.votes)
            binding.containerRatings.textViewRatingsTraktValue.text = TraktTools.buildRatingString(
                it.rating
            )
        }
        // if movie is not in database, can't handle user ratings
        if (!inCollection && !inWatchlist && !isWatched) {
            binding.containerRatings.textViewRatingsTraktUserLabel.isGone = true
            binding.containerRatings.textViewRatingsTraktUser.isGone = true
            binding.containerRatings.root.isClickable = false
            binding.containerRatings.root.isLongClickable = false // cheat sheet
        } else {
            binding.containerRatings.textViewRatingsTraktUserLabel.isGone = false
            binding.containerRatings.textViewRatingsTraktUser.isGone = false
            binding.containerRatings.textViewRatingsTraktUser.text =
                TraktTools.buildUserRatingString(
                    activity,
                    rating
                )
            binding.containerRatings.root.setOnClickListener { rateMovie() }
            TooltipCompat.setTooltipText(
                binding.containerRatings.root,
                binding.containerRatings.root.context.getString(R.string.action_rate)
            )
        }
        binding.containerRatings.root.isGone = false

        // genres
        binding.textViewMovieGenresLabel.isGone = false
        ViewTools.setValueOrPlaceholder(
            binding.textViewMovieGenres, TmdbTools.buildGenresString(tmdbMovie.genres)
        )

        // trakt comments link
        binding.buttonMovieComments.setOnClickListener { v ->
            val i = TraktCommentsActivity.intentMovie(requireContext(), movieTitle, tmdbId)
            Utils.startActivityWithAnimation(activity, i, v)
        }
        binding.buttonMovieComments.isGone = false

        // load poster, cache on external storage
        if (tmdbMovie.poster_path.isNullOrEmpty()) {
            binding.frameLayoutMoviePoster.isClickable = false
            binding.frameLayoutMoviePoster.isFocusable = false
        } else {
            val smallImageUrl = (TmdbSettings.getImageBaseUrl(activity)
                    + TmdbSettings.POSTER_SIZE_SPEC_W342 + tmdbMovie.poster_path)
            ServiceUtils.loadWithPicasso(activity, smallImageUrl)
                .into(binding.imageViewMoviePoster, object : Callback.EmptyCallback() {
                    override fun onSuccess() {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val bitmap =
                                (binding.imageViewMoviePoster.drawable as BitmapDrawable).bitmap

                            val color = withContext(Dispatchers.Default) {
                                val palette = try {
                                    Palette.from(bitmap).generate()
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to generate palette.")
                                    null
                                }
                                palette
                                    ?.getVibrantColor(Color.WHITE)
                                    ?.let { ColorUtils.setAlphaComponent(it, 50) }
                            }

                            color?.let { binding.rootLayoutMovie.setBackgroundColor(it) }
                        }
                    }
                })
            // click listener for high resolution poster
            binding.frameLayoutMoviePoster.also {
                it.isFocusable = true
                it.setOnClickListener { view ->
                    val largeImageUrl =
                        TmdbSettings.getImageOriginalUrl(activity, tmdbMovie.poster_path)
                    val intent = FullscreenImageActivity.intent(
                        requireActivity(),
                        smallImageUrl,
                        largeImageUrl
                    )
                    Utils.startActivityWithAnimation(activity, intent, view)
                }
            }
        }
    }

    /**
     * Menu click listener to watch again (supporters only) or set unwatched.
     */
    private class WatchedPopupMenuListener(
        val context: Context,
        val movieTmdbId: Int,
        val plays: Int,
        val inWatchlist: Boolean
    ) : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.watched_popup_menu_watch_again -> if (Utils.hasAccessToX(context)) {
                    MovieTools.watchedMovie(context, movieTmdbId, plays, inWatchlist)
                } else {
                    Utils.advertiseSubscription(context)
                }
                R.id.watched_popup_menu_set_not_watched -> MovieTools.unwatchedMovie(
                    context,
                    movieTmdbId
                )
            }
            return true
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
            && PeopleListHelper.populateMovieCast(
                activity,
                binding.moviePeople.containerCast,
                credits
            )) {
            setCastVisibility(true)
        } else {
            setCastVisibility(false)
        }

        // crew members
        if (credits.crew?.size != 0
            && PeopleListHelper.populateMovieCrew(
                activity,
                binding.moviePeople.containerCrew,
                credits
            )) {
            setCrewVisibility(true)
        } else {
            setCrewVisibility(false)
        }
    }

    private fun onButtonCheckInClick() {
        movieTitle?.let {
            if (it.isEmpty()) {
                return
            }
            MovieCheckInDialogFragment.show(parentFragmentManager, tmdbId, it)
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
    fun onEventEpisodeTask(@Suppress("UNUSED_PARAMETER") event: BaseMessageActivity.ServiceActiveEvent) {
        setMovieButtonsEnabled(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventEpisodeTask(@Suppress("UNUSED_PARAMETER") event: BaseMessageActivity.ServiceCompletedEvent) {
        setMovieButtonsEnabled(true)
    }

    private fun setMovieButtonsEnabled(enabled: Boolean) {
        binding.containerMovieButtons.apply {
            buttonMovieCheckIn.isEnabled = enabled
            buttonMovieWatched.isEnabled = enabled
            buttonMovieCollected.isEnabled = enabled
            buttonMovieWatchlisted.isEnabled = enabled
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleLanguageEvent(@Suppress("UNUSED_PARAMETER") event: MovieLocalizationDialogFragment.LocalizationChangedEvent) {
        // reload movie details and trailers (but not cast/crew info which is not language dependent)
        restartMovieLoader()
        val args = Bundle().apply {
            putInt(ARG_TMDB_ID, tmdbId)
        }
        LoaderManager.getInstance(this).restartLoader(
            MovieDetailsActivity.LOADER_ID_MOVIE_TRAILERS, args, trailerLoaderCallbacks
        )
    }

    override fun loadMovieActions() {
        var actions = ExtensionManager.get(context)
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
                ExtensionManager.get(context).requestMovieActions(context, movie)
            }
        }

        Timber.d("loadMovieActions: received %s actions for %s", actions.size, tmdbId)
        requireActivity().run {
            ActionsHelper.populateActions(
                layoutInflater, theme, binding.containerMovieActions, actions
            )
        }
    }

    private var loadActionsJob: Job? = null

    override fun loadMovieActionsDelayed() {
        // Simple de-bounce: cancel any waiting job.
        loadActionsJob?.cancel()
        loadActionsJob = lifecycleScope.launch {
            delay(MovieActionsContract.ACTION_LOADER_DELAY_MILLIS.toLong())
            loadMovieActions()
        }
    }

    private fun rateMovie() {
        RateDialogFragment.newInstanceMovie(tmdbId).safeShow(context, parentFragmentManager)
    }

    private fun setCrewVisibility(visible: Boolean) {
        binding.moviePeople.labelCrew.isGone = !visible
        binding.moviePeople.containerCrew.isGone = !visible
    }

    private fun setCastVisibility(visible: Boolean) {
        binding.moviePeople.labelCast.isGone = !visible
        binding.moviePeople.containerCast.isGone = !visible
    }

    private fun restartMovieLoader() {
        val args = Bundle()
        args.putInt(ARG_TMDB_ID, tmdbId)
        LoaderManager.getInstance(this)
            .restartLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args, movieLoaderCallbacks)
    }

    private val movieLoaderCallbacks = object : LoaderManager.LoaderCallbacks<MovieDetails> {
        override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<MovieDetails> {
            binding.progressBar.isGone = false
            return MovieLoader(requireContext(), args!!.getInt(ARG_TMDB_ID))
        }

        override fun onLoadFinished(movieLoader: Loader<MovieDetails>, movieDetails: MovieDetails) {
            if (!isAdded) {
                return
            }
            this@MovieDetailsFragment.movieDetails = movieDetails
            binding.progressBar.isGone = true

            // we need at least values from database or tmdb
            if (movieDetails.tmdbMovie() != null) {
                populateMovieViews()
                loadMovieActions()
                activity!!.invalidateOptionsMenu()
            } else {
                // if there is no local data and loading from network failed
                binding.textViewMovieDescription.text =
                    if (AndroidUtils.isNetworkConnected(requireContext())) {
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
                binding.buttonMovieTrailer.isEnabled = true
            }
        }

        override fun onLoaderReset(trailersLoader: Loader<Videos.Video>) {
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
