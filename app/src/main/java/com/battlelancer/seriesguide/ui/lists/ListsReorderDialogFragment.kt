package com.battlelancer.seriesguide.ui.lists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.ListsActivity
import com.battlelancer.seriesguide.ui.lists.OrderedListsLoader.OrderedList
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortController
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortListView
import java.util.ArrayList

/**
 * Dialog to reorder lists using a vertical list with drag handles. Currently not accessibility or
 * keyboard friendly (same as extension configuration screen).
 */
class ListsReorderDialogFragment : AppCompatDialogFragment() {

    @BindView(R.id.listViewListsReorder)
    var dragSortListView: DragSortListView? = null

    @BindView(R.id.buttonNegative)
    var buttonNegative: Button? = null

    @BindView(R.id.buttonPositive)
    var buttonPositive: Button? = null

    private var unbinder: Unbinder? = null
    private lateinit var adapter: ListsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_lists_reorder, container, false)
        unbinder = ButterKnife.bind(this, view)

        val controller = DragSortController(
            dragSortListView,
            R.id.dragGripViewItemList, DragSortController.ON_DOWN,
            DragSortController.CLICK_REMOVE
        )
        controller.isRemoveEnabled = false
        dragSortListView!!.setFloatViewManager(controller)
        dragSortListView!!.setOnTouchListener(controller)
        dragSortListView!!.setDropListener { from: Int, to: Int -> reorderList(from, to) }

        buttonNegative!!.setText(R.string.discard)
        buttonNegative!!.setOnClickListener { v: View? -> dismiss() }

        buttonPositive!!.setText(android.R.string.ok)
        buttonPositive!!.setOnClickListener { v: View? ->
            saveListsOrder()
            dismiss()
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ListsAdapter(activity)
        dragSortListView!!.adapter = adapter

        LoaderManager.getInstance(this)
            .initLoader(ListsActivity.LISTS_REORDER_LOADER_ID, null, listsLoaderCallbacks)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
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