package com.battlelancer.seriesguide.lists

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.databinding.FragmentListBinding
import com.battlelancer.seriesguide.lists.ListsDistillationSettings.ListsSortOrderChangedEvent
import com.battlelancer.seriesguide.lists.database.SgListItemWithDetails
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays one user created list of shows.
 */
class SgListFragment : Fragment() {

    private var binding: FragmentListBinding? = null

    private val model by viewModels<SgListItemViewModel> {
        SgListItemViewModelFactory(
            requireArguments().getString(ARG_LIST_ID)!!,
            requireActivity().application
        )
    }
    private val listsActivityViewModel by viewModels<ListsActivityViewModel>(ownerProducer = { requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return FragmentListBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding!!

        ViewTools.setVectorDrawableTop(binding.emptyViewList, R.drawable.ic_list_white_24dp)

        val adapter = SgListItemAdapter(requireContext(), onItemClickListener)

        val recyclerView = binding.recyclerViewListItems.also {
            it.setHasFixedSize(true)
            it.layoutManager =
                AutoGridLayoutManager(
                    requireContext(), R.dimen.showgrid_columnWidth, 1, 1
                )
            it.adapter = adapter
        }

        listsActivityViewModel.scrollTabToTopLiveData
            .observe(viewLifecycleOwner) { tabPosition: Int? ->
                if (tabPosition != null
                    && tabPosition == requireArguments().getInt(ARG_LIST_POSITION)) {
                    recyclerView.smoothScrollToPosition(0)
                }
            }

        model.sgListItemLiveData.observe(viewLifecycleOwner) {
            val bindingOnDemand = this.binding ?: return@observe
            bindingOnDemand.recyclerViewListItems.isGone = it.isEmpty()
            bindingOnDemand.emptyViewList.isGone = it.isNotEmpty()
            adapter.submitList(it)
        }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: ListsSortOrderChangedEvent?) {
        // sort order has changed, reload lists
        model.updateQuery()
    }

    private val onItemClickListener: SgListItemAdapter.OnItemClickListener =
        object : SgListItemAdapter.OnItemClickListener {
            override fun onItemClick(anchor: View, item: SgListItemWithDetails) {
                Utils.startActivityWithAnimation(
                    requireActivity(),
                    OverviewActivity.intentShow(requireActivity(), item.showId),
                    anchor
                )
            }

            override fun onMenuClick(anchor: View, item: SgListItemWithDetails) {
                val popupMenu = PopupMenu(anchor.context, anchor)
                popupMenu.inflate(R.menu.lists_popup_menu)
                popupMenu.setOnMenuItemClickListener(
                    PopupMenuItemClickListener(
                        requireContext(), parentFragmentManager,
                        item.listItemId, item.showId
                    )
                )
                // Hide manage lists option for legacy show items, only allow removal.
                if (item.type != ListItemTypes.TMDB_SHOW) {
                    popupMenu.menu.removeItem(R.id.menu_action_lists_manage)
                }
                popupMenu.show()
            }


            override fun onFavoriteClick(showId: Long, isFavorite: Boolean) {
                SgApp.getServicesComponent(requireContext()).showTools()
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
        const val liftOnScrollTargetViewId = R.id.recyclerViewListItems

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