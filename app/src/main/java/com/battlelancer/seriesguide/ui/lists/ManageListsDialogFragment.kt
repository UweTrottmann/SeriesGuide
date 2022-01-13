package com.battlelancer.seriesguide.ui.lists;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.provider.SgShow2Minimal;
import com.battlelancer.seriesguide.util.DialogTools;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a dialog displaying all user created lists,
 * allowing to add or remove the given show for any.
 */
public class ManageListsDialogFragment extends AppCompatDialogFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    public static final String TAG = "listsdialog";
    private static final String ARG_LONG_SHOW_ID = "show_id";

    private static ManageListsDialogFragment newInstance(long showId) {
        ManageListsDialogFragment f = new ManageListsDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_LONG_SHOW_ID, showId);
        f.setArguments(args);
        return f;
    }

    /**
     * Display a dialog which asks if the user wants to add the given show to one or more lists.
     */
    public static boolean show(FragmentManager fm, long showId) {
        if (showId <= 0) return false;
        // replace any currently showing list dialog (do not add it to the back stack)
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        return DialogTools.safeShow(ManageListsDialogFragment.newInstance(showId), fm, ft, TAG);
    }

    private ListView listView;
    private ListsAdapter adapter;
    private long showId;
    private int showTmdbId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showId = requireArguments().getLong(ARG_LONG_SHOW_ID);

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
        dontAddButton.setOnClickListener(v -> dismiss());
        Button addButton = layout.findViewById(R.id.buttonPositive);
        addButton.setText(android.R.string.ok);
        addButton.setOnClickListener(v -> {
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

            ListsTools.changeListsOfItem(requireContext(), showTmdbId,
                    ListItemTypes.TMDB_SHOW, addToTheseLists, removeFromTheseLists);

            dismiss();
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

        adapter = new ListsAdapter(getActivity());
        listView.setAdapter(adapter);

        SgShow2Minimal showDetails = SgRoomDatabase.getInstance(requireContext()).sgShow2Helper()
                .getShowMinimal(showId);
        if (showDetails == null || showDetails.getTmdbId() == null || showDetails.getTmdbId() == 0) {
            dismiss();
            return;
        }
        showTmdbId = showDetails.getTmdbId();

        // display item title
        final TextView itemTitle = getView().findViewById(R.id.item);
        itemTitle.setText(showDetails.getTitle());

        // load data
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Checkable checkable = (Checkable) view;
        checkable.toggle();
        adapter.setItemChecked(position, checkable.isChecked());
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // filter for this item, but keep other lists
        Uri uri = Lists.buildListsWithListItemUri(
                ListItems.generateListItemIdWildcard(showTmdbId, ListItemTypes.TMDB_SHOW));
        return new CursorLoader(requireContext(), uri, ListsQuery.PROJECTION,
                null, null, Lists.SORT_ORDER_THEN_NAME);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private static class ListsAdapter extends CursorAdapter {

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

        String[] PROJECTION = new String[]{
                Tables.LISTS + "." + Lists._ID, Tables.LISTS + "." + Lists.LIST_ID, Lists.NAME,
                ListItems.LIST_ITEM_ID
        };

        int LIST_ID = 1;

        int NAME = 2;

        int LIST_ITEM_ID = 3;
    }
}
