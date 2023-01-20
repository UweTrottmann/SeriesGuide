package com.battlelancer.seriesguide.movies

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesDiscoverBinding
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment.LocalizationChangedEvent
import com.battlelancer.seriesguide.movies.MoviesActivityViewModel.ScrollTabToTopEvent
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays links to movie lists (see [MoviesDiscoverAdapter]) and movies currently in theaters.
 */
class MoviesDiscoverFragment : Fragment() {

    private var binding: FragmentMoviesDiscoverBinding? = null
    private lateinit var adapter: MoviesDiscoverAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentMoviesDiscoverBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding!!

        binding.swipeRefreshLayoutMoviesDiscover.setOnRefreshListener(onRefreshListener)
        binding.swipeRefreshLayoutMoviesDiscover.isRefreshing = false
        ViewTools.setSwipeRefreshLayoutColors(
            requireActivity().theme,
            binding.swipeRefreshLayoutMoviesDiscover
        )

        adapter = MoviesDiscoverAdapter(
            requireContext(),
            DiscoverItemClickListener(requireContext())
        )

        val layoutManager = AutoGridLayoutManager(
            context,
            R.dimen.movie_grid_columnWidth, 2, 6
        )
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = adapter.getItemViewType(position)
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_LINK) {
                    return 3
                }
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_HEADER) {
                    return layoutManager.spanCount
                }
                return if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_MOVIE) {
                    2
                } else 0
            }
        }

        binding.recyclerViewMoviesDiscover.setHasFixedSize(true)
        binding.recyclerViewMoviesDiscover.layoutManager = layoutManager
        binding.recyclerViewMoviesDiscover.adapter = adapter

        ViewModelProvider(requireActivity())[MoviesActivityViewModel::class.java]
            .scrollTabToTopLiveData
            .observe(viewLifecycleOwner) { event: ScrollTabToTopEvent? ->
                if (event != null
                    && event.tabPosition == MoviesActivityImpl.TAB_POSITION_DISCOVER) {
                    binding.recyclerViewMoviesDiscover.smoothScrollToPosition(0)
                }
            }

        LoaderManager.getInstance(this).initLoader(0, null, nowPlayingLoaderCallbacks)

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
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

    private val optionsMenuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.movies_discover_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val itemId = menuItem.itemId
            if (itemId == R.id.menu_action_movies_search_change_language) {
                MovieLocalizationDialogFragment.show(parentFragmentManager)
                return true
            }
            return false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventLanguageChanged(event: LocalizationChangedEvent?) {
        LoaderManager.getInstance(this).restartLoader(0, null, nowPlayingLoaderCallbacks)
    }

    private class DiscoverItemClickListener(context: Context) : MovieClickListenerImpl(context),
        MoviesDiscoverAdapter.ItemClickListener {
        override fun onClickLink(link: MoviesDiscoverLink, anchor: View) {
            val intent = Intent(context, MoviesSearchActivity::class.java)
            intent.putExtra(MoviesSearchActivity.EXTRA_ID_LINK, link.id)
            Utils.startActivityWithAnimation(context, intent, anchor)
        }
    }

    private val nowPlayingLoaderCallbacks: LoaderManager.LoaderCallbacks<MoviesDiscoverLoader.Result> =
        object : LoaderManager.LoaderCallbacks<MoviesDiscoverLoader.Result> {
            override fun onCreateLoader(
                id: Int,
                args: Bundle?
            ): Loader<MoviesDiscoverLoader.Result> {
                return MoviesDiscoverLoader(requireContext())
            }

            override fun onLoadFinished(
                loader: Loader<MoviesDiscoverLoader.Result>,
                data: MoviesDiscoverLoader.Result
            ) {
                binding?.swipeRefreshLayoutMoviesDiscover?.isRefreshing = false
                adapter.updateMovies(data.results)
            }

            override fun onLoaderReset(loader: Loader<MoviesDiscoverLoader.Result>) {
                adapter.updateMovies(null)
            }
        }
    private val onRefreshListener = OnRefreshListener {
        LoaderManager.getInstance(this@MoviesDiscoverFragment)
            .restartLoader(0, null, nowPlayingLoaderCallbacks)
    }

    companion object {
        const val liftOnScrollTargetViewId = R.id.recyclerViewMoviesDiscover
    }
}