package com.battlelancer.seriesguide.ui.lists

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogListsReorderBinding
import com.battlelancer.seriesguide.ui.ListsActivity
import com.battlelancer.seriesguide.ui.lists.OrderedListsLoader.OrderedList
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortController
import java.util.ArrayList

/**
 * Dialog to reorder lists using a vertical list with drag handles. Currently not accessibility or
 * keyboard friendly (same as extension configuration screen).
 */
class ListsReorderDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogListsReorderBinding? = null
    private lateinit var adapter: ListsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogListsReorderBinding.inflate(layoutInflater)
        this.binding = binding

        val controller = DragSortController(
            binding.listViewListsReorder,
            R.id.dragGripViewItemList, DragSortController.ON_DOWN,
            DragSortController.CLICK_REMOVE
        )
        controller.isRemoveEnabled = false
        binding.listViewListsReorder.setFloatViewManager(controller)
        binding.listViewListsReorder.setOnTouchListener(controller)
        binding.listViewListsReorder.setDropListener { from: Int, to: Int -> reorderList(from, to) }

        adapter = ListsAdapter(activity)
        binding.listViewListsReorder.adapter = adapter

        LoaderManager.getInstance(this)
            .initLoader(ListsActivity.LISTS_REORDER_LOADER_ID, null, listsLoaderCallbacks)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(R.string.action_lists_reorder)
            .setNegativeButton(R.string.discard, null)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                saveListsOrder()
                dismiss()
            }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.binding = null
    }

    private fun reorderList(from: Int, to: Int) {
        adapter.reorderList(from, to)
    }

    private fun saveListsOrder() {
        val count = adapter.count
        val listIdsInOrder: MutableList<String> = ArrayList(count)
        for (position in 0 until count) {
            val list = adapter.getItem(position)
            if (list != null && !list.id.isNullOrEmpty()) {
                listIdsInOrder.add(list.id)
            }
        }
        ListsTools.reorderLists(requireContext(), listIdsInOrder)
    }

    private val listsLoaderCallbacks: LoaderManager.LoaderCallbacks<List<OrderedList>> =
        object : LoaderManager.LoaderCallbacks<List<OrderedList>> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<OrderedList>> {
                return OrderedListsLoader(activity)
            }

            override fun onLoadFinished(
                loader: Loader<List<OrderedList>>,
                data: List<OrderedList>
            ) {
                if (!isAdded) {
                    return
                }
                adapter.setData(data)
            }

            override fun onLoaderReset(loader: Loader<List<OrderedList>>) {
                adapter.setData(null)
            }
        }
}