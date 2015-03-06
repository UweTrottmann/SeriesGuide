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

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ListsAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

/**
 * Dialog to reorder lists using a vertical list with drag handles. Currently not accessibility or
 * keyboard friendly (same as extension configuration screen).
 */
public class ListsReorderDialogFragment extends DialogFragment {

    public static void show(FragmentManager fragmentManager) {
        ListsReorderDialogFragment f = new ListsReorderDialogFragment();
        f.show(fragmentManager, "lists-reorder-dialog");
    }

    @InjectView(R.id.listViewListsReorder) DragSortListView dragSortListView;
    @InjectView(R.id.buttonNegative) Button buttonNegative;
    @InjectView(R.id.buttonPositive) Button buttonPositive;

    private ListsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_lists_reorder, container, false);
        ButterKnife.inject(this, v);

        DragSortController controller = new DragSortController(dragSortListView,
                R.id.dragGripViewItemList, DragSortController.ON_DOWN,
                DragSortController.CLICK_REMOVE);
        controller.setRemoveEnabled(false);
        dragSortListView.setFloatViewManager(controller);
        dragSortListView.setOnTouchListener(controller);

        buttonNegative.setText(android.R.string.cancel);
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

        ButterKnife.reset(this);
    }

    private void saveListsOrder() {
        // TODO
    }

    private LoaderManager.LoaderCallbacks<Cursor> listsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), SeriesGuideContract.Lists.CONTENT_URI,
                    new String[] { SeriesGuideContract.Lists._ID, SeriesGuideContract.Lists.NAME },
                    null, null, SeriesGuideContract.Lists.SORT_ORDER_THEN_NAME);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (!isAdded()) {
                return;
            }

            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };
}
