package com.battlelancer.seriesguide.ui.lists

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.ui.OverviewActivity.Companion.intentShow
import com.battlelancer.seriesguide.ui.lists.ListItemsAdapter.ListItemViewHolder
import com.battlelancer.seriesguide.ui.lists.ListsDistillationSettings.ListsSortOrderChangedEvent
import com.battlelancer.seriesguide.ui.shows.BaseShowsAdapter
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays one user created list of shows.
 */
class SgListFragment : Fragment() {

    private lateinit var adapter: ListItemsAdapter
    private var emptyView: TextView? = null

    private val listsActivityViewModel by viewModels<ListsActivityViewModel>(ownerProducer = { requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        emptyView = view.findViewById(R.id.emptyViewList)
        ViewTools.setVectorDrawableTop(emptyView, R.drawable.ic_list_white_24dp)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ListItemsAdapter(requireActivity(), onItemClickListener)

        val gridView = view.findViewById<GridView>(R.id.gridViewList)
        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(gridView, true)
        gridView.adapter = adapter
        gridView.emptyView = emptyView

        listsActivityViewModel.scrollTabToTopLiveData
            .observe(viewLifecycleOwner, { tabPosition: Int? ->
                if (tabPosition != null
                    && tabPosition == requireArguments().getInt(ARG_LIST_POSITION)) {
                    gridView.smoothScrollToPosition(0)
                }
            })

        LoaderManager.getInstance(this)
            .initLoader(LOADER_ID, requireArguments(), loaderCallbacks)
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
        emptyView = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: ListsSortOrderChangedEvent?) {
        // sort order has changed, reload lists
        LoaderManager.getInstance(this)
            .restartLoader(LOADER_ID, requireArguments(), loaderCallbacks)
    }

    private val loaderCallbacks: LoaderManager.LoaderCallbacks<Cursor> =
        object : LoaderManager.LoaderCallbacks<Cursor> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
                val listId = requireArguments().getString(ARG_LIST_ID)
                return CursorLoader(
                    requireContext(),
                    ListItems.CONTENT_WITH_DETAILS_URI,
                    ListItemsAdapter.Query.PROJECTION,
                    // items of this list, but exclude any if show was removed from the database
                    // (the join on show data will fail, hence the show id will be 0/null)
                    ListItems.SELECTION_LIST + " AND " + SgShow2Columns.REF_SHOW_ID + ">0",
                    arrayOf(listId),
                    ListsDistillationSettings.getSortQuery(activity)
                )
            }

            override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
                adapter.swapCursor(data)
            }

            override fun onLoaderReset(loader: Loader<Cursor>) {
                adapter.swapCursor(null)
            }
        }

    private val onItemClickListener: BaseShowsAdapter.OnItemClickListener =
        object : BaseShowsAdapter.OnItemClickListener {
            override fun onItemClick(
                anchor: View,
                showViewHolder: BaseShowsAdapter.ShowViewHolder
            ) {
                val viewHolder = showViewHolder as ListItemViewHolder
                Utils.startActivityWithAnimation(
                    requireActivity(),
                    intentShow(requireActivity(), viewHolder.showId),
                    anchor
                )
            }

            override fun onMenuClick(view: View, viewHolder: BaseShowsAdapter.ShowViewHolder) {
                if (viewHolder is ListItemViewHolder) {
                    val popupMenu = PopupMenu(view.context, view)
                    popupMenu.inflate(R.menu.lists_popup_menu)
                    popupMenu.setOnMenuItemClickListener(
                        PopupMenuItemClickListener(
                            requireContext(), parentFragmentManager,
                            viewHolder.itemId, viewHolder.showId
                        )
                    )
                    // Hide manage lists option for legacy show items, only allow removal.
                    if (viewHolder.itemType != ListItemTypes.TMDB_SHOW) {
                        popupMenu.menu.removeItem(R.id.menu_action_lists_manage)
                    }
                    popupMenu.show()
                }
            }

            override fun onFavoriteClick(showId: Long, isFavorite: Boolean) {
                getServicesComponent(requireContext()).showTools()
                    .storeIsFavorite(showId, isFavorite)
            }
        }

    private class PopupMenuItemClickListener(
        private val context: Context,
        private val fragmentManager: FragmentManager,
        private val itemId: String,
        private val showId: Long
    ) : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == R.id.menu_action_lists_manage) {
                ManageListsDialogFragment.show(fragmentManager, showId)
                return true
            } else if (id == R.id.menu_action_lists_remove) {
                ListsTools.removeListItem(context, itemId)
                return true
            }
            return false
        }
    }

    companion object {
        /** LoaderManager is created unique to fragment, so use same id for all of them  */
        private const val LOADER_ID = 1
        private const val ARG_LIST_ID = "LIST_ID"
        private const val ARG_LIST_POSITION = "LIST_POSITION"

        @JvmStatic
        fun newInstance(listId: String, listPosition: Int): SgListFragment {
            val f = SgListFragment()

            val args = Bundle()
            args.putString(ARG_LIST_ID, listId)
            args.putInt(ARG_LIST_POSITION, listPosition)
            f.arguments = args

            return f
        }
    }
}