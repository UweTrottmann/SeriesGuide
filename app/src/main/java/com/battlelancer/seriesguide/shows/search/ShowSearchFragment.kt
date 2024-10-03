// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search

import android.app.SearchManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.databinding.FragmentShowSearchBinding
import com.battlelancer.seriesguide.shows.ShowMenuItemClickListener
import com.battlelancer.seriesguide.shows.search.discover.ShowsDiscoverPagingActivity
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.widgets.EmptyView
import com.battlelancer.seriesguide.util.TabClickEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays show search results.
 */
class ShowSearchFragment : BaseSearchFragment() {

    private var binding: FragmentShowSearchBinding? = null
    private val model by viewModels<ShowSearchViewModel>()
    private lateinit var adapter: ShowSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentShowSearchBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override val emptyView: View
        get() = binding!!.textViewSearchShowsEmpty

    override val gridView: GridView
        get() = binding!!.gridViewSearchShows

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (emptyView as EmptyView).setButtonClickListener {
            navigateToAddShow()
        }
        adapter = ShowSearchAdapter(requireContext(), itemClickListener).also {
            gridView.adapter = it
        }

        model.shows.observe(viewLifecycleOwner) { shows ->
            adapter.setData(shows)
            updateEmptyState(shows.isNullOrEmpty(), !model.searchTerm.value.isNullOrEmpty())
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        // load for given query (if just created)
        val args = initialSearchArgs
        if (args != null) {
            updateQuery(args)
        }
    }

    private fun navigateToAddShow() {
        model.searchTerm.value
            .let { ShowsDiscoverPagingActivity.intentSearch(requireContext(), it) }
            .also { startActivity(it) }
    }

    private val optionsMenuProvider = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.shows_search_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_action_shows_search_add -> {
                    navigateToAddShow()
                    true
                }

                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchActivityImpl.SearchQueryEvent) {
        updateQuery(event.args)
    }

    private fun updateQuery(args: Bundle) {
        val query = args.getString(SearchManager.QUERY)
        model.searchTerm.value = query
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventTabClick(event: TabClickEvent) {
        if (event.position == SearchActivityImpl.TAB_POSITION_SHOWS) {
            gridView.smoothScrollToPosition(0)
        }
    }

    private val itemClickListener = object : ShowSearchAdapter.ItemClickListener {

        override fun onItemClick(anchor: View, viewHolder: ShowSearchAdapter.ShowViewHolder) {
            OverviewActivity.intentShow(requireContext(), viewHolder.showId).let {
                ActivityCompat.startActivity(
                    requireActivity(), it,
                    ActivityOptionsCompat.makeScaleUpAnimation(
                        anchor, 0, 0,
                        anchor.width, anchor.height
                    ).toBundle()
                )
            }
        }

        override fun onMoreOptionsClick(anchor: View, viewHolder: ShowSearchAdapter.ShowViewHolder) {
            PopupMenu(anchor.context, anchor).apply {
                inflate(R.menu.shows_popup_menu)
                menu.apply {
                    // show/hide some menu items depending on show properties
                    findItem(
                        R.id.menu_action_shows_favorites_add
                    ).isVisible = !viewHolder.isFavorited
                    findItem(
                        R.id.menu_action_shows_favorites_remove
                    ).isVisible = viewHolder.isFavorited
                    findItem(R.id.menu_action_shows_hide).isVisible = !viewHolder.isHidden
                    findItem(R.id.menu_action_shows_unhide).isVisible = viewHolder.isHidden

                    // hide unused actions
                    findItem(R.id.menu_action_shows_watched_next).isVisible = false
                }
                setOnMenuItemClickListener(
                    ShowMenuItemClickListener(
                        requireContext(),
                        parentFragmentManager,
                        viewHolder.showId,
                        0
                    )
                )
            }.show()
        }

        override fun onFavoriteClick(showId: Long, isFavorite: Boolean) {
            SgApp.getServicesComponent(requireContext()).showTools()
                .storeIsFavorite(showId, isFavorite)
        }
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.gridViewSearchShows
    }
}
