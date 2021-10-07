package com.battlelancer.seriesguide.ui.search

import android.app.SearchManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.shows.ShowMenuItemClickListener
import com.battlelancer.seriesguide.util.TabClickEvent
import com.battlelancer.seriesguide.widgets.EmptyView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays show search results.
 */
class ShowSearchFragment : BaseSearchFragment() {

    private val model by viewModels<ShowSearchViewModel>()
    private lateinit var adapter: ShowSearchAdapter
    private lateinit var searchTriggerListener: SearchTriggerListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_show_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (emptyView as EmptyView).setButtonClickListener {
            searchTriggerListener.switchToDiscoverAndSearch()
        }
        adapter = ShowSearchAdapter(requireContext(), onItemClickListener).also {
            gridView.adapter = it
        }

        if (activity is SearchTriggerListener) {
            searchTriggerListener = activity as SearchTriggerListener
        } else {
            throw IllegalArgumentException("Activity does not implement SearchTriggerListener")
        }

        model.shows.observe(viewLifecycleOwner) { shows ->
            adapter.setData(shows)
            updateEmptyState(shows.isNullOrEmpty(), !model.searchTerm.value.isNullOrEmpty())
        }

        // load for given query (if just created)
        val args = initialSearchArgs
        if (args != null) {
            updateQuery(args)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: SearchActivity.SearchQueryEvent) {
        updateQuery(event.args)
    }

    private fun updateQuery(args: Bundle) {
        val query = args.getString(SearchManager.QUERY)
        model.searchTerm.value = query
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventTabClick(event: TabClickEvent) {
        if (event.position == SearchActivity.TAB_POSITION_SHOWS) {
            gridView.smoothScrollToPosition(0)
        }
    }

    private val onItemClickListener = object : ShowSearchAdapter.OnItemClickListener {

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

        override fun onMenuClick(anchor: View, viewHolder: ShowSearchAdapter.ShowViewHolder) {
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
                        context,
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
}
