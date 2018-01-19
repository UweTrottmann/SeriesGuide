package com.battlelancer.seriesguide.ui.lists;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatDialogFragment;
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
import com.battlelancer.seriesguide.util.SeasonTools;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a dialog displaying all lists, allowing to add the given show, season or episode to any
 * number of them.
 */
public class ManageListsDialogFragment extends AppCompatDialogFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    private static ManageListsDialogFragment newInstance(int itemTvdbId,
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

    private ListView listView;
    private ListsAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.dialog_manage_lists, container, false);

        // buttons
        Button dontAddButton = layout.findViewById(R.id.buttonNegative);
        dontAddButton.setText(android.R.string.cancel);
        dontAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Button addButton = layout.findViewById(R.id.buttonPositive);
        addButton.setText(android.R.string.ok);
        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // add item to selected lists, remove from previously selected lists
                SparseBooleanArray checkedLists = adapter.getCheckedPositions();
                List<String> addToTheseLists = new ArrayList<>();
                List<String> removeFromTheseLists = new ArrayList<>();
                for (int position = 0; position < adapter.getCount(); position++) {
                    final Cursor listEntry = (Cursor) adapter.getItem(position);

                    boolean wasListChecked = !TextUtils.isEmpty(listEntry
                            .getString(ListsQuery.LIST_ITEM_ID));
                    boolean isListChecked = checkedLists.get(position);

                    String listId = listEntry.getString(ListsQuery.LIST_ID);
                    if (TextUtils.isEmpty(listId)) {
                        continue; // skip, no id
                    }
                    if (wasListChecked && !isListChecked) {
                        // remove from list
                        removeFromTheseLists.add(listId);
                    } else if (!wasListChecked && isListChecked) {
                        // add to list
                        addToTheseLists.add(listId);
                    }
                }

                int itemTvdbId = getArguments().getInt(InitBundle.INT_ITEM_TVDB_ID);
                int itemType = getArguments().getInt(InitBundle.INT_ITEM_TYPE);
                ListsTools.changeListsOfItem(getContext(), itemTvdbId, itemType,
                        addToTheseLists, removeFromTheseLists);

                dismiss();
            }
        });

        // lists list
        listView = layout.findViewById(R.id.list);
        /*
         * As using CHOICE_MODE_MULTIPLE does not seem to work before Jelly
         * Bean, do everything ourselves.
         */
        listView.setOnItemClickListener(this);

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle args) {
        super.onActivityCreated(args);

        // display item title
        final int itemTvdbId = getArguments().getInt(InitBundle.INT_ITEM_TVDB_ID);
        final int itemType = getArguments().getInt(InitBundle.INT_ITEM_TYPE);
        //noinspection ConstantConditions // fragment has a view
        final TextView itemTitle = getView().findViewById(R.id.item);
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
        //noinspection ConstantConditions // itemType might not match
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

        adapter = new ListsAdapter(getActivity());
        listView.setAdapter(adapter);

        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Checkable checkable = (Checkable) view;
        checkable.toggle();
        adapter.setItemChecked(position, checkable.isChecked());
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
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private class ListsAdapter extends CursorAdapter {

        private LayoutInflater layoutInflater;
        private SparseBooleanArray checkedItems;

        public ListsAdapter(Context context) {
            super(context, null, 0);
            layoutInflater = LayoutInflater.from(context);
            checkedItems = new SparseBooleanArray();
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CheckedTextView checkedView = view.findViewById(android.R.id.text1);
            checkedView.setText(cursor.getString(ListsQuery.NAME));

            int position = cursor.getPosition();

            // prefer state set by user over database
            boolean isChecked;
            if (checkedItems.indexOfKey(position) >= 0) {
                // user has changed checked state, prefer it
                isChecked = checkedItems.get(position);
            } else {
                // otherwise prefer database state, check if item is in this list
                String itemId = cursor.getString(ListsQuery.LIST_ITEM_ID);
                isChecked = !TextUtils.isEmpty(itemId);
                checkedItems.put(position, isChecked);
            }
            checkedView.setChecked(isChecked);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return layoutInflater.inflate(R.layout.item_list_checked, parent, false);
        }

        void setItemChecked(int position, boolean value) {
            checkedItems.put(position, value);
        }

        SparseBooleanArray getCheckedPositions() {
            return checkedItems;
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
