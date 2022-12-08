package com.battlelancer.seriesguide.shows.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
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
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedJob
import com.battlelancer.seriesguide.shows.ShowsActivityImpl
import com.battlelancer.seriesguide.shows.ShowsActivityViewModel
import com.battlelancer.seriesguide.shows.episodes.EpisodesActivity
import com.battlelancer.seriesguide.shows.history.NowAdapter.NowItem
import com.battlelancer.seriesguide.shows.search.discover.AddShowDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceCompletedEvent
import com.battlelancer.seriesguide.util.ViewTools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays recently watched episodes and recent episodes from friends (if connected to trakt).
 */
class ShowsNowFragment : Fragment() {

    private var binding: FragmentNowBinding? = null

    private lateinit var adapter: NowAdapter
    private var isLoadingRecentlyWatched = false
    private var isLoadingFriends = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentNowBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        binding.emptyViewNow.setText(R.string.now_empty)

        showError(null)
        binding.includeSnackbar.buttonSnackbar.setText(R.string.refresh)
        binding.includeSnackbar.buttonSnackbar.setOnClickListener { refreshStream() }

        // define dataset
        adapter = NowAdapter(requireContext(), itemClickListener)
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
                return if (type == NowAdapter.ItemType.HEADER || type == NowAdapter.ItemType.MORE_LINK) {
                    spanCount
                } else {
                    1
                }
            }
        }
        binding.recyclerViewNow.layoutManager = layoutManager
        binding.recyclerViewNow.setHasFixedSize(true)
        binding.recyclerViewNow.adapter = adapter

        ViewModelProvider(requireActivity()).get(ShowsActivityViewModel::class.java)
            .scrollTabToTopLiveData
            .observe(viewLifecycleOwner) { tabPosition: Int? ->
                if (tabPosition != null
                    && tabPosition == ShowsActivityImpl.Tab.NOW.index) {
                    binding.recyclerViewNow.smoothScrollToPosition(0)
                }
            }

        ViewTools.setSwipeRefreshLayoutColors(
            requireActivity().theme,
            binding.swipeRefreshLayoutNow
        )

        // if connected to trakt, replace local history with trakt history, show friends history
        if (TraktCredentials.get(requireContext()).hasCredentials()) {
            isLoadingRecentlyWatched = true
            isLoadingFriends = true
            showProgressBar(true)
            val loaderManager = LoaderManager.getInstance(this)
            loaderManager.initLoader(
                ShowsActivityImpl.NOW_TRAKT_USER_LOADER_ID, null,
                recentlyTraktCallbacks
            )
            loaderManager.initLoader(
                ShowsActivityImpl.NOW_TRAKT_FRIENDS_LOADER_ID, null,
                traktFriendsHistoryCallbacks
            )
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)

        /*
        Init recently watched loader here the earliest.
        So we can restart them if they already exist to ensure up to date data (the loaders do
        not react to database changes themselves) and avoid loading data twice in a row.
        */
        if (!TraktCredentials.get(requireContext()).hasCredentials()) {
            isLoadingRecentlyWatched = true
            initAndMaybeRestartLoader()
        }
    }

    /**
     * Init the loader. If the loader already exists, will restart it (the default behavior of init
     * would be to get the last loaded data).
     */
    private fun initAndMaybeRestartLoader() {
        val loaderId = ShowsActivityImpl.NOW_RECENTLY_LOADER_ID
        val loaderManager = LoaderManager.getInstance(this)
        val isLoaderExists = loaderManager.getLoader<Any>(loaderId) != null
        loaderManager.initLoader(loaderId, null, recentlyLocalCallbacks)
        if (isLoaderExists) {
            loaderManager.restartLoader(loaderId, null, recentlyLocalCallbacks)
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private val optionsMenuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.shows_now_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val itemId = menuItem.itemId
            if (itemId == R.id.menu_action_shows_now_refresh) {
                refreshStream()
                return true
            }
            return false
        }
    }

    private fun refreshStream() {
        showProgressBar(true)
        showError(null)

        // if connected to trakt, replace local history with trakt history, show friends history
        // user might get disconnected during our life-time,
        // so properly clean up old loaders so they won't interfere
        isLoadingRecentlyWatched = true
        val loaderManager = LoaderManager.getInstance(this)
        if (TraktCredentials.get(requireContext()).hasCredentials()) {
            destroyLoaderIfExists(ShowsActivityImpl.NOW_RECENTLY_LOADER_ID)

            loaderManager.restartLoader(
                ShowsActivityImpl.NOW_TRAKT_USER_LOADER_ID, null,
                recentlyTraktCallbacks
            )
            isLoadingFriends = true
            loaderManager.restartLoader(
                ShowsActivityImpl.NOW_TRAKT_FRIENDS_LOADER_ID, null,
                traktFriendsHistoryCallbacks
            )
        } else {
            // destroy trakt loaders and remove any shown error message
            destroyLoaderIfExists(ShowsActivityImpl.NOW_TRAKT_USER_LOADER_ID)
            destroyLoaderIfExists(ShowsActivityImpl.NOW_TRAKT_FRIENDS_LOADER_ID)
            showError(null)

            loaderManager.restartLoader(
                ShowsActivityImpl.NOW_RECENTLY_LOADER_ID, null,
                recentlyLocalCallbacks
            )
        }
    }

    private fun destroyLoaderIfExists(loaderId: Int) {
        val loaderManager = LoaderManager.getInstance(this)
        if (loaderManager.getLoader<Any>(loaderId) != null) {
            loaderManager.destroyLoader(loaderId)
        }
    }

    /**
     * Starts an activity to display the given episode.
     */
    private fun showDetails(view: View, episodeId: Long) {
        val intent = EpisodesActivity.intentEpisode(episodeId, requireContext())

        ActivityCompat.startActivity(
            requireContext(), intent,
            ActivityOptionsCompat
                .makeScaleUpAnimation(view, 0, 0, view.width, view.height)
                .toBundle()
        )
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
     * Show or hide the progress bar of the SwipeRefreshLayout wrapping view.
     */
    private fun showProgressBar(show: Boolean) {
        // only hide if everybody has finished loading
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
        binding.recyclerViewNow.visibility =
            if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyViewNow.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventEpisodeTask(event: ServiceCompletedEvent) {
        if (event.flagJob == null || !event.isSuccessful) {
            return  // no changes applied
        }
        if (!isAdded) {
            return  // no longer added to activity
        }
        // reload recently watched if user set or unset an episode watched
        // however, if connected to trakt do not show local history
        if (event.flagJob is EpisodeWatchedJob
            && !TraktCredentials.get(requireContext()).hasCredentials()) {
            isLoadingRecentlyWatched = true
            LoaderManager.getInstance(this)
                .restartLoader(
                    ShowsActivityImpl.NOW_RECENTLY_LOADER_ID, null,
                    recentlyLocalCallbacks
                )
        }
    }

    private val itemClickListener: NowAdapter.ItemClickListener =
        object : NowAdapter.ItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = adapter.getItem(position)

                // more history link?
                if (item.type == NowAdapter.ItemType.MORE_LINK) {
                    startActivity(
                        Intent(activity, HistoryActivity::class.java).putExtra(
                            HistoryActivity.InitBundle.HISTORY_TYPE,
                            HistoryActivity.DISPLAY_EPISODE_HISTORY
                        )
                    )
                    return
                }
                val episodeRowId = item.episodeRowId
                val showTmdbId = item.showTmdbId
                if (episodeRowId != null && episodeRowId > 0) {
                    // episode in database: display details
                    showDetails(view, episodeRowId)
                } else if (showTmdbId != null && showTmdbId > 0) {
                    // episode missing: show likely not in database, suggest adding it
                    AddShowDialogFragment.show(parentFragmentManager, showTmdbId)
                }
            }
        }

    private val recentlyLocalCallbacks: LoaderManager.LoaderCallbacks<MutableList<NowItem>> =
        object : LoaderManager.LoaderCallbacks<MutableList<NowItem>> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<MutableList<NowItem>> {
                return RecentlyWatchedLoader(requireContext())
            }

            override fun onLoadFinished(
                loader: Loader<MutableList<NowItem>>,
                data: MutableList<NowItem>
            ) {
                adapter.setRecentlyWatched(data)
                isLoadingRecentlyWatched = false
                showProgressBar(false)
            }

            override fun onLoaderReset(loader: Loader<MutableList<NowItem>>) {
                // clear existing data
                adapter.setRecentlyWatched(null)
            }
        }

    private val recentlyTraktCallbacks: LoaderManager.LoaderCallbacks<TraktRecentEpisodeHistoryLoader.Result> =
        object : LoaderManager.LoaderCallbacks<TraktRecentEpisodeHistoryLoader.Result> {
            override fun onCreateLoader(
                id: Int,
                args: Bundle?
            ): Loader<TraktRecentEpisodeHistoryLoader.Result> {
                return TraktRecentEpisodeHistoryLoader(activity)
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
                return TraktFriendsEpisodeHistoryLoader(requireContext())
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
}
