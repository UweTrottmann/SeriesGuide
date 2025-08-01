// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.movies.details

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.collection.SparseArrayCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.palette.graphics.Palette
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.billing.BillingTools
import com.battlelancer.seriesguide.comments.TraktCommentsActivity
import com.battlelancer.seriesguide.databinding.FragmentMovieBinding
import com.battlelancer.seriesguide.extensions.ActionsHelper
import com.battlelancer.seriesguide.extensions.ExtensionManager
import com.battlelancer.seriesguide.extensions.MovieActionsContract
import com.battlelancer.seriesguide.getSgAppContainer
import com.battlelancer.seriesguide.movies.MovieLoader
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.movies.collection.MovieCollectionActivity
import com.battlelancer.seriesguide.movies.similar.SimilarMoviesActivity
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.people.Credits
import com.battlelancer.seriesguide.people.PeopleListHelper
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.traktapi.MovieCheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.RateDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.FullscreenImageActivity
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.Metacritic
import com.battlelancer.seriesguide.util.RatingsTools.initialize
import com.battlelancer.seriesguide.util.RatingsTools.setLink
import com.battlelancer.seriesguide.util.RatingsTools.setRatingValues
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.WebTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.battlelancer.seriesguide.util.startActivityWithAnimation
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

/**
 * Displays details about one movie including plot, ratings, trailers and a poster.
 */
class MovieDetailsFragment : Fragment(), MovieActionsContract {

    private var _binding: FragmentMovieBinding? = null
    private val binding get() = _binding!!

    private var tmdbId: Int = 0
    private var movieDetails: MovieDetails? =
        MovieDetails()
    private var movieTitle: String? = null
    private var trailerYoutubeId: String? = null
    private val model: MovieDetailsModel by viewModels {
        MovieDetailsModelFactory(tmdbId, requireActivity().application)
    }
    private lateinit var scrollChangeListener: ToolbarScrollChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tmdbId = requireArguments().getInt(ARG_TMDB_ID)
        if (tmdbId <= 0) {
            parentFragmentManager.popBackStack()
            return
        }
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
        binding.labelMovieLastUpdated.isGone = true

        // some action buttons
        binding.containerMovieButtons.apply {
            root.isGone = true
            // trailer button
            buttonMovieTrailer.apply {
                setOnClickListener {
                    trailerYoutubeId?.let { ServiceUtils.openYoutube(it, requireContext()) }
                }
                isEnabled = false
            }
            // release dates button
            buttonMovieReleaseDates.setOnClickListener {
                WebTools.openInCustomTab(
                    requireContext(),
                    TmdbTools.buildMovieReleaseDatesUrl(tmdbId)
                )
            }
            // similar movies button
            buttonMovieSimilar.setOnClickListener {
                movieDetails?.tmdbMovie()
                    ?.title
                    ?.let {
                        startActivity(
                            SimilarMoviesActivity.intent(
                                requireContext(),
                                tmdbId,
                                it
                            )
                        )
                    }
            }
            buttonMovieShare.setOnClickListener {
                movieDetails?.tmdbMovie()
                    ?.title
                    ?.let { ShareUtils.shareMovie(requireActivity(), tmdbId, it) }
            }
            buttonMovieCalendar.setOnClickListener {
                movieDetails?.tmdbMovie()?.also {
                    val title = it.title
                    val releaseTimeMs = it.release_date?.time
                    if (title != null && releaseTimeMs != null) {
                        ShareUtils.suggestAllDayCalendarEvent(
                            requireContext(),
                            title,
                            releaseTimeMs
                        )
                    }
                }
            }
            buttonMovieCheckIn.setOnClickListener { onButtonCheckInClick() }
            TooltipCompat.setTooltipText(
                buttonMovieCheckIn,
                buttonMovieCheckIn.contentDescription
            )
            StreamingSearch.initButtons(
                buttonMovieStreamingSearch,
                buttonMovieStreamingSearchInfo,
                parentFragmentManager
            )
        }
        // ratings
        binding.containerRatings.apply {
            root.isGone = true // to animate in later
            initialize { rateMovie() }
            ratingViewTmdb.setLink(requireContext(), TmdbTools.buildMovieUrl(tmdbId))
            ratingViewTrakt.setLink(requireContext(), TraktTools.buildMovieUrl(tmdbId))
        }

        // language button
        binding.buttonMovieLanguage.isGone = true
        TooltipCompat.setTooltipText(
            binding.buttonMovieLanguage,
            binding.buttonMovieLanguage.context.getString(R.string.pref_language)
        )
        binding.buttonMovieLanguage.setOnClickListener {
            MovieLocalizationDialogFragment.show(parentFragmentManager)
        }

        if (requireActivity().getSgAppContainer().preventExternalLinks) {
            binding.containerMovieBottom.root.isGone = true
        }

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
        setupViews()

        val args = Bundle()
        args.putInt(ARG_TMDB_ID, tmdbId)
        LoaderManager.getInstance(this).apply {
            initLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args, movieLoaderCallbacks)
            initLoader(
                MovieDetailsActivity.LOADER_ID_MOVIE_TRAILERS, args, trailerLoaderCallbacks
            )
        }
        model.credits.observe(viewLifecycleOwner) {
            populateMovieCreditsViews(it)
        }
        model.watchProvider.observe(viewLifecycleOwner) { watchInfo ->
            StreamingSearch.configureButton(
                binding.containerMovieButtons.buttonMovieStreamingSearch,
                watchInfo,
                requireActivity().getSgAppContainer().preventExternalLinks
            )
        }
    }

    private fun setupViews() {
        // show toolbar title and background when scrolling
        val defaultPaddingPx = resources.getDimensionPixelSize(R.dimen.large_padding)
        val scrollChangeListener = ToolbarScrollChangeListener(defaultPaddingPx)
            .also { scrollChangeListener = it }
        binding.contentContainerMovie.setOnScrollChangeListener(scrollChangeListener)
        binding.contentContainerMovieRight?.setOnScrollChangeListener(scrollChangeListener)

        ThemeUtils.applyBottomPaddingForNavigationBar(binding.contentContainerMovie)
        binding.contentContainerMovieRight?.let { ThemeUtils.applyBottomPaddingForNavigationBar(it) }
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

        val releaseDate = tmdbMovie.release_date
        val releaseDateAndLength = TextTools.dotSeparate(
            releaseDate?.let { TimeTools.formatToLocalDate(context, it) },
            tmdbMovie.runtime?.let { TimeTools.formatToHoursAndMinutes(resources, it) }
        )

        // Use movie title, release date and length in app bar (shown when scrolling)
        // Set activity title for accessibility tools
        (requireActivity() as AppCompatActivity).apply {
            title = tmdbMovie.title
            supportActionBar?.let {
                it.title = tmdbMovie.title
                it.subtitle = releaseDateAndLength
            }
        }

        binding.textViewMovieDescription.text = TextTools.textWithTmdbSource(
            binding.textViewMovieDescription.context,
            tmdbMovie.overview
        )

        binding.containerMovieButtons.buttonMovieShare.isEnabled = movieTitle != null

        // release date and runtime: "July 17, 2009 · 1 h 5 min"
        binding.textViewMovieDate.text = releaseDateAndLength

        // hide create event button if release date is yesterday or older
        binding.containerMovieButtons.buttonMovieCalendar.isGone =
            releaseDate == null || Instant.ofEpochMilli(releaseDate.time)
                .isBefore(ZonedDateTime.now().minusDays(1).toInstant())

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
                    MovieTools.watchedMovie(requireContext(), tmdbId, plays, inWatchlist)
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
                    MovieTools.removeFromCollection(requireContext(), tmdbId)
                } else {
                    MovieTools.addToCollection(requireContext(), tmdbId)
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
                    MovieTools.removeFromWatchlist(requireContext(), tmdbId)
                } else {
                    MovieTools.addToWatchlist(requireContext(), tmdbId)
                }
            }
        }

        // Comments
        binding.containerMovieButtons.buttonMovieComments.apply {
            setOnClickListener { v ->
                val tmdbId = tmdbId
                if (tmdbId > 0) {
                    val i = TraktCommentsActivity.intentMovie(requireContext(), movieTitle, tmdbId)
                    requireActivity().startActivityWithAnimation(i, v)
                }
            }
        }

        // Metacritic search
        binding.containerMovieButtons.buttonMovieMetacritic.apply {
            // Metacritic only has English titles so mostly English speaking users will use it,
            // so its likely the original language of the movie is English.
            val titleOrNull = if (tmdbMovie.original_language == "en") {
                tmdbMovie.original_title
            } else tmdbMovie.title
            isGone = titleOrNull.isNullOrEmpty()
            setOnClickListener {
                titleOrNull?.let { Metacritic.searchForMovie(requireContext(), it) }
            }
        }

        // Show collection button if movies is part of one
        binding.containerMovieButtons.buttonMovieCollection.apply {
            val collection = tmdbMovie.belongs_to_collection
            val collectionId = collection?.id
            val collectionName = collection?.name
            if (collectionId != null && collectionName != null) {
                setOnClickListener {
                    startActivity(
                        MovieCollectionActivity.intent(
                            requireContext(),
                            collectionId,
                            collectionName
                        )
                    )
                }
                text = collectionName
                isVisible = true
            } else {
                isGone = true
            }
        }

        // show buttons after configuring them
        binding.containerMovieButtons.root.isGone = false

        // language button
        binding.buttonMovieLanguage.also {
            it.text = LanguageTools.getMovieLanguageStringFor(
                requireContext(),
                null,
                MoviesSettings.getMoviesLanguage(requireContext())
            )
            it.isGone = false
        }

        // ratings
        binding.containerRatings.apply {
            setRatingValues(
                tmdbMovie.vote_average,
                tmdbMovie.vote_count,
                traktRatings?.rating,
                traktRatings?.votes
            )

            // if movie is not in database, can't handle user ratings
            if (!inCollection && !inWatchlist && !isWatched) {
                groupRatingsUser.isGone = true
            } else {
                groupRatingsUser.isGone = false
                textViewRatingsUser.text =
                    TraktTools.buildUserRatingString(activity, rating)
            }
            root.isGone = false
        }

        // genres
        binding.textViewMovieGenresLabel.isGone = false
        ViewTools.setValueOrPlaceholder(
            binding.textViewMovieGenres, TmdbTools.buildGenresString(tmdbMovie.genres)
        )

        // links
        binding.containerMovieBottom.buttonMovieTmdb.setOnClickListener {
            WebTools.openInApp(requireContext(), TmdbTools.buildMovieUrl(tmdbId))
        }
        binding.containerMovieBottom.buttonMovieTrakt.setOnClickListener {
            WebTools.openInApp(requireContext(), TraktTools.buildMovieUrl(tmdbId))
        }
        binding.containerMovieBottom.buttonMovieImdb.apply {
            val imdbId = tmdbMovie.imdb_id
            isGone = imdbId.isNullOrEmpty()
            setOnClickListener {
                imdbId?.let { ServiceUtils.openImdb(it, requireContext()) }
            }
        }

        // When this movie was last updated by this app
        binding.labelMovieLastUpdated.isGone = false
        binding.textMovieLastUpdated.text =
            TimeTools.formatToLocalDateAndTime(requireContext(), movieDetails.lastUpdatedMillis)

        // load poster, cache on external storage
        if (tmdbMovie.poster_path.isNullOrEmpty()) {
            binding.frameLayoutMoviePoster.isClickable = false
            binding.frameLayoutMoviePoster.isFocusable = false
        } else {
            val smallImageUrl = (TmdbSettings.getImageBaseUrl(requireContext())
                    + TmdbSettings.POSTER_SIZE_SPEC_W342 + tmdbMovie.poster_path)
            ImageTools.loadWithPicasso(requireContext(), smallImageUrl)
                .into(binding.imageViewMoviePoster, object : Callback.EmptyCallback() {
                    override fun onSuccess() {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val bitmap =
                                (binding.imageViewMoviePoster.drawable as BitmapDrawable).bitmap

                            val (colorBackground, colorAppBarLifted) = withContext(Dispatchers.Default) {
                                val palette = try {
                                    Palette.from(bitmap).generate()
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to generate palette.")
                                    null
                                }
                                val vibrantColor = palette?.getVibrantColor(Color.WHITE)
                                Pair(
                                    vibrantColor?.let { ColorUtils.setAlphaComponent(it, 50) },
                                    vibrantColor?.let { ColorUtils.setAlphaComponent(it, 80) }
                                )
                            }

                            colorBackground?.let {
                                // Color fragment background
                                binding.rootLayoutMovie.setBackgroundColor(it)

                                // Color app bar background
                                scrollChangeListener.appBarBackground = colorBackground
                                scrollChangeListener.appBarBackgroundLifted = colorAppBarLifted
                                sgAppBarLayout.apply {
                                    background = ColorDrawable(
                                        if (scrollChangeListener.showOverlay) {
                                            colorAppBarLifted!!
                                        } else {
                                            colorBackground
                                        }
                                    )
                                }
                            }
                        }
                    }
                })
            // click listener for high resolution poster
            binding.frameLayoutMoviePoster.also {
                it.isFocusable = true
                it.setOnClickListener { view ->
                    val posterPath = tmdbMovie.poster_path ?: return@setOnClickListener
                    val largeImageUrl =
                        TmdbSettings.getImageOriginalUrl(requireContext(), posterPath)
                    val intent = FullscreenImageActivity.intent(
                        requireActivity(),
                        smallImageUrl,
                        largeImageUrl
                    )
                    requireActivity().startActivityWithAnimation(intent, view)
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
                R.id.watched_popup_menu_watch_again -> if (BillingTools.hasAccessToPaidFeatures(context)) {
                    MovieTools.watchedMovie(context, movieTmdbId, plays, inWatchlist)
                } else {
                    BillingTools.advertiseSubscription(context)
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
        val peopleListHelper = PeopleListHelper()
        // cast members
        if (peopleListHelper.populateMovieCast(
                requireContext(),
                binding.moviePeople.containerCast,
                credits
            )) {
            setCastVisibility(true)
        } else {
            setCastVisibility(false)
        }

        // crew members
        if (peopleListHelper.populateMovieCrew(
                requireContext(),
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
    fun onEvent(event: MovieChangedEvent) {
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
                layoutInflater, binding.containerMovieActions, actions
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
        movieDetails?.let {
            RateDialogFragment.newInstanceMovie(tmdbId, it.userRating)
                .safeShow(requireContext(), parentFragmentManager)
        }
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

    private val trailerLoaderCallbacks = object : LoaderManager.LoaderCallbacks<String?> {
        override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<String?> {
            return MovieTrailersLoader(requireContext(), args!!.getInt(ARG_TMDB_ID))
        }

        override fun onLoadFinished(
            trailersLoader: Loader<String?>,
            videoId: String?
        ) {
            if (videoId != null) {
                this@MovieDetailsFragment.trailerYoutubeId = videoId
                binding.containerMovieButtons.buttonMovieTrailer.isEnabled = true
            }
        }

        override fun onLoaderReset(trailersLoader: Loader<String?>) {
            // do nothing
        }
    }

    private val sgAppBarLayout
        get() = (activity as MovieDetailsActivity).sgAppBarLayout

    private inner class ToolbarScrollChangeListener(
        private val overlayThresholdPx: Int
    ) : NestedScrollView.OnScrollChangeListener {

        var appBarBackground: Int? = null
        var appBarBackgroundLifted: Int? = null

        // we have determined by science that a capacity of 2 is good in our case :)
        private val showOverlayMap: SparseArrayCompat<Boolean> = SparseArrayCompat(2)
        var showOverlay: Boolean = false
            private set
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
                val drawableColor = appBarBackgroundLifted ?: ContextCompat.getColor(
                    v.context,
                    ThemeUtils.resolveAttributeToResourceId(
                        v.context.theme, R.attr.sgColorStatusBarOverlay
                    )
                )
                sgAppBarLayout.background = ColorDrawable(drawableColor)
            } else if (showOverlay && !shouldShowOverlay) {
                val drawableColor = appBarBackground ?: Color.TRANSPARENT
                sgAppBarLayout.background = ColorDrawable(drawableColor)
            }
            showOverlay = shouldShowOverlay

            // Only show/hide title if main container displaying title is scrolled.
            if (viewId == R.id.contentContainerMovie) {
                if (!showTitle && shouldShowOverlay) {
                    movieDetails?.tmdbMovie()?.let {
                        actionBar.title = it.title
                        actionBar.setDisplayShowTitleEnabled(true)
                    }
                } else if (showTitle && !shouldShowOverlay) {
                    actionBar.setDisplayShowTitleEnabled(false)
                }
                showTitle = shouldShowOverlay
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

    /**
     * Post to make [MovieDetailsFragment] update movie details.
     */
    class MovieChangedEvent(var movieTmdbId: Int)
}
