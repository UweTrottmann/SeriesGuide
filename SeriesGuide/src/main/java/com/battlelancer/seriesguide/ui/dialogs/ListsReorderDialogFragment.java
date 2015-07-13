/*
 * Copyright 2015 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui.dialogs;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ListsAdapter;
import com.battlelancer.seriesguide.loaders.OrderedListsLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Dialog to reorder lists using a vertical list with drag handles. Currently not accessibility or
 * keyboard friendly (same as extension configuration screen).
 */
public class ListsReorderDialogFragment extends DialogFragment {

    public static void show(FragmentManager fragmentManager) {
        ListsReorderDialogFragment f = new ListsReorderDialogFragment();
        f.show(fragmentManager, "lists-reorder-dialog");
    }

    @Bind(R.id.listViewListsReorder) DragSortListView dragSortListView;
    @Bind(R.id.buttonNegative) Button buttonNegative;
    @Bind(R.id.buttonPositive) Button buttonPositive;

    private ListsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_lists_reorder, container, false);
        ButterKnife.bind(this, v);

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

        ButterKnife.unbind(this);
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

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (int position = 0; position < adapter.getCount(); position++) {
            OrderedListsLoader.OrderedList list = adapter.getItem(position);
            batch.add(ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Lists.buildListUri(list.id))
                    .withValue(SeriesGuideContract.Lists.ORDER, position)
                    .build());
        }

        try {
            DBUtils.applyInSmallBatches(getActivity(), batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "drop: failed to save reordered lists.");
        }

        EventBus.getDefault().post(new ListsActivity.ListsChangedEvent());
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
