package com.battlelancer.seriesguide.ui.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ListsAdapter;
import com.battlelancer.seriesguide.loaders.OrderedListsLoader;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.battlelancer.seriesguide.util.ListsTools;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to reorder lists using a vertical list with drag handles. Currently not accessibility or
 * keyboard friendly (same as extension configuration screen).
 */
public class ListsReorderDialogFragment extends AppCompatDialogFragment {

    public static void show(FragmentManager fragmentManager) {
        ListsReorderDialogFragment f = new ListsReorderDialogFragment();
        f.show(fragmentManager, "lists-reorder-dialog");
    }

    @BindView(R.id.listViewListsReorder) DragSortListView dragSortListView;
    @BindView(R.id.buttonNegative) Button buttonNegative;
    @BindView(R.id.buttonPositive) Button buttonPositive;

    private Unbinder unbinder;
    private ListsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_lists_reorder, container, false);
        unbinder = ButterKnife.bind(this, v);

        DragSortController controller = new DragSortController(dragSortListView,
                R.id.dragGripViewItemList, DragSortController.ON_DOWN,
                DragSortController.CLICK_REMOVE);
        controller.setRemoveEnabled(false);
        dragSortListView.setFloatViewManager(controller);
        dragSortListView.setOnTouchListener(controller);
        dragSortListView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                reorderList(from, to);
            }
        });

        buttonNegative.setText(R.string.discard);
        buttonNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        buttonPositive.setText(android.R.string.ok);
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveListsOrder();
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ListsAdapter(getActivity());
        dragSortListView.setAdapter(adapter);

        getLoaderManager().initLoader(ListsActivity.LISTS_REORDER_LOADER_ID, null,
                listsLoaderCallbacks);
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
