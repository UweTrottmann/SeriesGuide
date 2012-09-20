
package com.battlelancer.seriesguide.ui.dialogs;

import android.content.Context;
import android.database.Cursor;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

public class ListsDialogFragment extends DialogFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static ListsDialogFragment newInstance(String itemId, int itemType) {
        ListsDialogFragment f = new ListsDialogFragment();
        Bundle args = new Bundle();
        args.putString("itemid", itemId);
        args.putInt("itemtype", itemType);
        f.setArguments(args);
        return f;
    }

    private ListView mListView;
    private ListsAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, R.style.SeriesGuideTheme_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.list_dialog, null);

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
                // TODO: add item to selected lists
                // if (mListView != null) {
                // SparseBooleanArray checkedLists =
                // mListView.getCheckedItemPositions();
                // String itemId = getArguments().getString("itemid");
                // int itemType = getArguments().getInt("itemtype");
                // checkedLists.
                // for (long listId : checkedLists) {
                // ContentValues values = new ContentValues();
                // String listId = mAdapter.getItem(position)
                // String id = ListItems.generateListItemId(itemId, itemType,
                // listId);
                // }
                // }
                dismiss();
            }
        });

        // lists list
        mListView = (ListView) layout.findViewById(R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);

        mAdapter = new ListsAdapter(getActivity(), null, 0);
        mListView.setAdapter(mAdapter);

        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // filter for this item, but keep other lists
        String itemId = args.getString("itemid");
        int itemType = args.getInt("itemtype");

        return new CursorLoader(getActivity(), Lists.buildListsWithListItemUri(ListItems
                .generateListItemIdWildcard(itemId, itemType)),
                ListsQuery.PROJECTION, null, null, null);
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

        public ListsAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!mDataValid) {
                throw new IllegalStateException(
                        "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            
            View v;
            if (convertView == null) {
                v = newView(mContext, mCursor, parent);
            } else {
                v = convertView;
            }
            
            CheckedTextView checkedView = (CheckedTextView) v.findViewById(android.R.id.text1);
            checkedView.setText(mCursor.getString(ListsQuery.NAME));

            // check list entry if this item is already added to it
            String itemId = mCursor.getString(ListsQuery.LIST_ITEM_ID);
            boolean isInList = !TextUtils.isEmpty(itemId);
            mListView.setItemChecked(position, isInList);
            
            return v;
        }

        @Override
        public void bindView(View arg0, Context arg1, Cursor arg2) {
        }
        
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, parent,
                    false);
            return v;
        }

    }

    interface ListsQuery {
        String[] PROJECTION = new String[] {
                Tables.LISTS + "." + Lists._ID, Tables.LISTS + "." + Lists.LIST_ID, Lists.NAME,
                ListItems.LIST_ITEM_ID
        };

        int NAME = 2;

        int LIST_ITEM_ID = 3;
    }

    /**
     * Display a dialog which asks if the user wants to add the given show to
     * one or more lists.
     * 
     * @param itemId TVDb/database id of the item to add
     * @param itemType type of the item to add (show, season or episode)
     * @param fm
     */
    public static void showListsDialog(String itemId, int itemType, FragmentManager fm) {
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
        DialogFragment newFragment = ListsDialogFragment.newInstance(itemId, itemType);
        newFragment.show(ft, "listsdialog");
    }
}
