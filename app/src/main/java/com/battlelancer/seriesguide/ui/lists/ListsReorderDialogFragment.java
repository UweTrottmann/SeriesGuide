package com.battlelancer.seriesguide.ui.lists;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortController;
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortListView;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to reorder lists using a vertical list with drag handles. Currently not accessibility or
 * keyboard friendly (same as extension configuration screen).
 */
public class ListsReorderDialogFragment extends AppCompatDialogFragment {

    @BindView(R.id.listViewListsReorder) DragSortListView dragSortListView;
    @BindView(R.id.buttonNegative) Button buttonNegative;
    @BindView(R.id.buttonPositive) Button buttonPositive;

    private Unbinder unbinder;
    private ListsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_lists_reorder, container, false);
        unbinder = ButterKnife.bind(this, view);

        DragSortController controller = new DragSortController(dragSortListView,
                R.id.dragGripViewItemList, DragSortController.ON_DOWN,
                DragSortController.CLICK_REMOVE);
        controller.setRemoveEnabled(false);
        dragSortListView.setFloatViewManager(controller);
        dragSortListView.setOnTouchListener(controller);
        dragSortListView.setDropListener(this::reorderList);

        buttonNegative.setText(R.string.discard);
        buttonNegative.setOnClickListener(v -> dismiss());

        buttonPositive.setText(android.R.string.ok);
        buttonPositive.setOnClickListener(v -> {
            saveListsOrder();
            dismiss();
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ListsAdapter(getActivity());
        dragSortListView.setAdapter(adapter);

        LoaderManager.getInstance(this)
                .initLoader(ListsActivity.LISTS_REORDER_LOADER_ID, null, listsLoaderCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    private void reorderList(int from, int to) {
        if (adapter == null) {
            return;
        }
        adapter.reorderList(from, to);
    }

    private void saveListsOrder() {
        if (adapter == null) {
            return;
        }

        int count = adapter.getCount();
        List<String> listIdsInOrder = new ArrayList<>(count);
        for (int position = 0; position < count; position++) {
            OrderedListsLoader.OrderedList list = adapter.getItem(position);
            if (list != null && !TextUtils.isEmpty(list.id)) {
                listIdsInOrder.add(list.id);
            }
        }
        ListsTools.reorderLists(getContext(), listIdsInOrder);
    }

    private LoaderManager.LoaderCallbacks<List<OrderedListsLoader.OrderedList>> listsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<OrderedListsLoader.OrderedList>>() {
        @Override
        public Loader<List<OrderedListsLoader.OrderedList>> onCreateLoader(int id, Bundle args) {
            return new OrderedListsLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<OrderedListsLoader.OrderedList>> loader,
                List<OrderedListsLoader.OrderedList> data) {
            if (!isAdded()) {
                return;
            }

            adapter.setData(data);
        }

        @Override
        public void onLoaderReset(Loader<List<OrderedListsLoader.OrderedList>> loader) {
            adapter.setData(null);
        }
    };
}
