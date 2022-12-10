package com.battlelancer.seriesguide.movies

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentNowBinding
import com.battlelancer.seriesguide.history.HistoryActivity
import com.battlelancer.seriesguide.movies.MoviesActivityViewModel.ScrollTabToTopEvent
import com.battlelancer.seriesguide.movies.details.MovieDetailsActivity
import com.battlelancer.seriesguide.shows.history.NowAdapter
import com.battlelancer.seriesguide.shows.history.NowAdapter.NowItem
import com.battlelancer.seriesguide.shows.history.TraktRecentEpisodeHistoryLoader
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools

/**
 * Displays recently watched movies, today's releases and recent watches from trakt friends (if
 * connected to trakt).
 */
class MoviesNowFragment : Fragment() {

    private var binding: FragmentNowBinding? = null

    private lateinit var adapter: MoviesNowAdapter
    private var isLoadingRecentlyWatched = false
    private var isLoadingFriends = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentNowBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding!!

        val swipeRefreshLayout = binding.swipeRefreshLayoutNow
        swipeRefreshLayout.setSwipeableChildren(R.id.scrollViewNow, R.id.recyclerViewNow)
        swipeRefreshLayout.setOnRefreshListener { refreshStream() }
        swipeRefreshLayout.setProgressViewOffset(
            false,
            resources.getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_start_margin
            ),
            resources.getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_end_margin
            )
        )
        binding.emptyViewNow.setText(R.string.now_movies_empty)

        showError(null)
        binding.includeSnackbar.buttonSnackbar.setText(R.string.refresh)
        binding.includeSnackbar.buttonSnackbar.setOnClickListener { refreshStream() }

        adapter = MoviesNowAdapter(requireContext(), itemClickListener)
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                updateEmptyState()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateEmptyState()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateEmptyState()
            }
        })

        // recycler view layout manager
        val spanCount = resources.getInteger(R.integer.grid_column_count)
        val layoutManager = GridLayoutManager(activity, spanCount)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (position >= adapter.itemCount) {
                    return 1
                }
                // make headers and more links span all columns
                val type = adapter.getItem(position).type
                return if (type == NowAdapter.ItemType.HEADER || type == NowAdapter.ItemType.MORE_LINK) spanCount else 1
            }
        }
        binding.recyclerViewNow.layoutManager = layoutManager
        binding.recyclerViewNow.setHasFixedSize(true)
        ViewModelProvider(requireActivity())[MoviesActivityViewModel::class.java]
            .scrollTabToTopLiveData
            .observe(viewLifecycleOwner) { event: ScrollTabToTopEvent? ->
                if (event != null
                    && event.tabPosition == MoviesActivityImpl.TAB_POSITION_NOW) {
                    binding.recyclerViewNow.smoothScrollToPosition(0)
                }
            }
        ViewTools.setSwipeRefreshLayoutColors(
            requireActivity().theme,
            binding.swipeRefreshLayoutNow
        )
        binding.recyclerViewNow.adapter = adapter

        // If connected to Trakt, replace local history with Trakt history, show friends history.
        if (TraktCredentials.get(requireContext()).hasCredentials()) {
            isLoadingRecentlyWatched = true
            isLoadingFriends = true
            showProgressBar(true)
            val loaderManager = LoaderManager.getInstance(this)
            loaderManager.initLoader(
                MoviesActivityImpl.NOW_TRAKT_USER_LOADER_ID, null,
                recentlyTraktCallbacks
            )
            loaderManager.initLoader(
                MoviesActivityImpl.NOW_TRAKT_FRIENDS_LOADER_ID, null,
                traktFriendsHistoryCallbacks
            )
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private val optionsMenuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.movies_now_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val itemId = menuItem.itemId
            if (itemId == R.id.menu_action_movies_now_refresh) {
                refreshStream()
                return true
            }
            return false
        }
    }

    private fun refreshStream() {
        showProgressBar(true)
        showError(null)

        // User might get disconnected while this exists,
        // so properly clean up old loaders so they won't interfere.
        if (TraktCredentials.get(requireContext()).hasCredentials()) {
            isLoadingRecentlyWatched = true
            val loaderManager = LoaderManager.getInstance(this)
            loaderManager.restartLoader(
                MoviesActivityImpl.NOW_TRAKT_USER_LOADER_ID, null,
                recentlyTraktCallbacks
            )
            isLoadingFriends = true
            loaderManager.restartLoader(
                MoviesActivityImpl.NOW_TRAKT_FRIENDS_LOADER_ID, null,
                traktFriendsHistoryCallbacks
            )
        } else {
            // destroy trakt loaders and remove any shown error message
            destroyLoaderIfExists(MoviesActivityImpl.NOW_TRAKT_USER_LOADER_ID)
            destroyLoaderIfExists(MoviesActivityImpl.NOW_TRAKT_FRIENDS_LOADER_ID)
            showError(null)
        }
    }

    private fun destroyLoaderIfExists(loaderId: Int) {
        val loaderManager = LoaderManager.getInstance(this)
        if (loaderManager.getLoader<Any>(loaderId) != null) {
            loaderManager.destroyLoader(loaderId)
        }
    }

    private fun showError(errorText: String?) {
        val binding = binding ?: return
        val show = errorText != null
        if (show) {
            binding.includeSnackbar.textViewSnackbar.text = errorText
        }
        val snackbar = binding.includeSnackbar.containerSnackbar
        if (snackbar.visibility == (if (show) View.VISIBLE else View.GONE)) {
            // already in desired state, avoid replaying animation
            return
        }
        snackbar.startAnimation(
            AnimationUtils.loadAnimation(
                snackbar.context,
                if (show) R.anim.fade_in else R.anim.fade_out
            )
        )
        snackbar.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Show or hide the progress bar of the SwipeRefreshLayout.
     */
    private fun showProgressBar(show: Boolean) {
        // Only hide if all loaders have finished loading.
        if (!show) {
            if (isLoadingRecentlyWatched || isLoadingFriends) {
                return
            }
        }
        binding?.swipeRefreshLayoutNow?.isRefreshing = show
    }

    private fun updateEmptyState() {
        val binding = binding ?: return
        val isEmpty = adapter.itemCount == 0
        binding.recyclerViewNow.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyViewNow.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private val itemClickListener: NowAdapter.ItemClickListener =
        object : NowAdapter.ItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = adapter.getItem(position)

                // More history link clicked?
                if (item.type == NowAdapter.ItemType.MORE_LINK) {
                    startActivity(
                        Intent(activity, HistoryActivity::class.java).putExtra(
                            HistoryActivity.InitBundle.HISTORY_TYPE,
                            HistoryActivity.DISPLAY_MOVIE_HISTORY
                        )
                    )
                    return
                }

                val movieTmdbId = item.movieTmdbId ?: return

                // display movie details
                val i = MovieDetailsActivity.intentMovie(activity, movieTmdbId)

                // simple scale up animation as there are no images
                Utils.startActivityWithAnimation(activity, i, view)
            }
        }

    private val recentlyTraktCallbacks: LoaderManager.LoaderCallbacks<TraktRecentEpisodeHistoryLoader.Result> =
        object : LoaderManager.LoaderCallbacks<TraktRecentEpisodeHistoryLoader.Result> {
            override fun onCreateLoader(
                id: Int,
                args: Bundle?
            ): Loader<TraktRecentEpisodeHistoryLoader.Result> {
                return TraktRecentMovieHistoryLoader(activity)
            }

            override fun onLoadFinished(
                loader: Loader<TraktRecentEpisodeHistoryLoader.Result>,
                data: TraktRecentEpisodeHistoryLoader.Result
            ) {
                adapter.setRecentlyWatched(data.items)
                isLoadingRecentlyWatched = false
                showProgressBar(false)
                showError(data.errorText)
            }

            override fun onLoaderReset(loader: Loader<TraktRecentEpisodeHistoryLoader.Result>) {
                // clear existing data
                adapter.setRecentlyWatched(null)
            }
        }

    private val traktFriendsHistoryCallbacks: LoaderManager.LoaderCallbacks<List<NowItem>?> =
        object : LoaderManager.LoaderCallbacks<List<NowItem>?> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<NowItem>?> {
                return TraktFriendsMovieHistoryLoader(requireContext())
            }

            override fun onLoadFinished(
                loader: Loader<List<NowItem>?>,
                data: List<NowItem>?
            ) {
                adapter.setFriendsRecentlyWatched(data)
                isLoadingFriends = false
                showProgressBar(false)
            }

            override fun onLoaderReset(loader: Loader<List<NowItem>?>) {
                // clear existing data
                adapter.setFriendsRecentlyWatched(null)
            }
        }

    companion object {
        const val liftOnScrollTargetViewId = R.id.recyclerViewNow
    }
}