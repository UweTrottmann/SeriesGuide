
/*
 * Copyright 2014 Uwe Trottmann
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
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.battlelancer.seriesguide.util.Utils;
import java.util.ArrayList;
import timber.log.Timber;

/**
 * Displays a dialog displaying all lists, allowing to add the given show, season or episode to any
 * number of them.
 */
public class ManageListsDialogFragment extends DialogFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    public static ManageListsDialogFragment newInstance(int itemTvdbId,
            @SeriesGuideContract.ListItemTypes int itemType) {
        ManageListsDialogFragment f = new ManageListsDialogFragment();
        Bundle args = new Bundle();
        args.putInt(InitBundle.INT_ITEM_TVDB_ID, itemTvdbId);
        args.putInt(InitBundle.INT_ITEM_TYPE, itemType);
        f.setArguments(args);
        return f;
    }

    public interface InitBundle {
        String INT_ITEM_TVDB_ID = "item-tvdbid";
        String INT_ITEM_TYPE = "item-type";
    }

    private ListView mListView;

    private ListsAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.dialog_manage_lists, container, false);

        // buttons
        Button dontAddButton = (Button) layout.findViewById(R.id.buttonNegative);
        dontAddButton.setText(android.R.string.cancel);
        dontAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Button addButton = (Button) layout.findViewById(R.id.buttonPositive);
        addButton.setText(android.R.string.ok);
        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // add item to selected lists
                int itemTvdbId = getArguments().getInt(InitBundle.INT_ITEM_TVDB_ID);
                int itemType = getArguments().getInt(InitBundle.INT_ITEM_TYPE);
                SparseBooleanArray checkedLists = mAdapter.getCheckedPositions();

                final ArrayList<ContentProviderOperation> batch = new ArrayList<>();

                for (int position = 0; position < mAdapter.getCount(); position++) {
                    final Cursor listEntry = (Cursor) mAdapter.getItem(position);

                    boolean wasListChecked = !TextUtils.isEmpty(listEntry
                            .getString(ListsQuery.LIST_ITEM_ID));
                    boolean isListChecked = checkedLists.get(position);

                    String listId = listEntry.getString(ListsQuery.LIST_ID);
                    String listItemId = ListItems.generateListItemId(itemTvdbId, itemType, listId);

                    if (wasListChecked && !isListChecked) {
                        // remove from list
                        batch.add(ContentProviderOperation.newDelete(
                                ListItems.buildListItemUri(listItemId)).build());
                    } else if (!wasListChecked && isListChecked) {
                        // add to list
                        ContentValues values = new ContentValues();
                        values.put(ListItems.LIST_ITEM_ID, listItemId);
                        values.put(ListItems.ITEM_REF_ID, itemTvdbId);
                        values.put(ListItems.TYPE, itemType);
                        values.put(Lists.LIST_ID, listId);
                        batch.add(ContentProviderOperation.newInsert(ListItems.CONTENT_URI)
                                .withValues(values)
                                .build());
                    }
                }

                // apply ops
                try {
                    DBUtils.applyInSmallBatches(getActivity(), batch);
                } catch (OperationApplicationException e) {
                    Timber.e(e, "Applying list changes failed");
                }

                getActivity().getContentResolver().notifyChange(ListItems.CONTENT_WITH_DETAILS_URI,
                        null);

                dismiss();
            }
        });

        // lists list
        mListView = (ListView) layout.findViewById(R.id.list);
        /*
         * As using CHOICE_MODE_MULTIPLE does not seem to work before Jelly
         * Bean, do everything ourselves.
         */
        mListView.setOnItemClickListener(this);

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle args) {
        super.onActivityCreated(args);

        // display item title
        final int itemTvdbId = getArguments().getInt(InitBundle.INT_ITEM_TVDB_ID);
        final int itemType = getArguments().getInt(InitBundle.INT_ITEM_TYPE);
        final TextView itemTitle = (TextView) getView().findViewById(R.id.item);
        Uri uri = null;
        String[] projection = null;
        switch (itemType) {
            case 1:
                // show
                uri = Shows.buildShowUri(itemTvdbId);
                projection = new String[] {
                        Shows._ID, Shows.TITLE
                };
                break;
            case 2:
                // season
                uri = Seasons.buildSeasonUri(itemTvdbId);
                projection = new String[] {
                        Seasons._ID, Seasons.COMBINED
                };
                break;
            case 3:
                // episode
                uri = Episodes.buildEpisodeUri(itemTvdbId);
                projection = new String[] {
                        Episodes._ID, Episodes.TITLE
                };
                break;
        }
        if (uri != null && projection != null) {
            Cursor item = getActivity().getContentResolver().query(uri, projection, null, null,
                    null);
            if (item != null && item.moveToFirst()) {
                if (itemType == 2) {
                    // season just has a number, build string
                    itemTitle.setText(SeasonTools.getSeasonString(getActivity(), item.getInt(1)));
                } else {
                    // shows and episodes
                    itemTitle.setText(item.getString(1));
                }
            }

            if (item != null) {
                item.close();
            }
        }

        mAdapter = new ListsAdapter(getActivity(), null, 0);
        mListView.setAdapter(mAdapter);

        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Manage Lists Dialog");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Checkable checkable = (Checkable) view;
        checkable.toggle();
        mAdapter.setItemChecked(position, checkable.isChecked());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // filter for this item, but keep other lists
        int itemTvdbId = args.getInt(InitBundle.INT_ITEM_TVDB_ID);
        int itemType = args.getInt(InitBundle.INT_ITEM_TYPE);

        return new CursorLoader(getActivity(), Lists.buildListsWithListItemUri(ListItems
                .generateListItemIdWildcard(itemTvdbId, itemType)),
                ListsQuery.PROJECTION, null, null, Lists.SORT_ORDER_THEN_NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private class ListsAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        private SparseBooleanArray mCheckedItems;

        public ListsAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mInflater = LayoutInflater.from(context);
            mCheckedItems = new SparseBooleanArray();
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CheckedTextView checkedView = (CheckedTextView) view.findViewById(android.R.id.text1);
            checkedView.setText(cursor.getString(ListsQuery.NAME));
            checkedView.setTextAppearance(context, R.style.TextAppearance);

            int position = cursor.getPosition();

            // prefer state set by user over database
            boolean isChecked;
            if (mCheckedItems.indexOfKey(position) >= 0) {
                // user has changed checked state, prefer it
                isChecked = mCheckedItems.get(position);
            } else {
                // otherwise prefer database state, check if item is in this list
                String itemId = cursor.getString(ListsQuery.LIST_ITEM_ID);
                isChecked = !TextUtils.isEmpty(itemId);
                mCheckedItems.put(position, isChecked);
            }
            checkedView.setChecked(isChecked);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.item_list_checked, parent, false);
        }

        public void setItemChecked(int position, boolean value) {
            mCheckedItems.put(position, value);
        }

        public SparseBooleanArray getCheckedPositions() {
            return mCheckedItems;
        }
    }

    interface ListsQuery {

        String[] PROJECTION = new String[] {
                Tables.LISTS + "." + Lists._ID, Tables.LISTS + "." + Lists.LIST_ID, Lists.NAME,
                ListItems.LIST_ITEM_ID
        };

        int LIST_ID = 1;

        int NAME = 2;

        int LIST_ITEM_ID = 3;
    }

    /**
     * Display a dialog which asks if the user wants to add the given show to one or more lists.
     *
     * @param itemTvdbId TVDb id of the item to add
     * @param itemType type of the item to add (show, season or episode)
     */
    public static void showListsDialog(int itemTvdbId,
            @SeriesGuideContract.ListItemTypes int itemType, FragmentManager fm) {
        if (fm == null) {
            return;
        }

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("listsdialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = ManageListsDialogFragment.newInstance(itemTvdbId, itemType);
        newFragment.show(ft, "listsdialog");
    }
}
