
package com.battlelancer.seriesguide.ui.dialogs;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Lists;

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
    private SimpleCursorAdapter mAdapter;

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

        mAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_multiple_choice,
                null, new String[] {
                    Lists.NAME
                }, new int[] {
                        android.R.id.text1
                }, 0);
        mListView.setAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), Lists.CONTENT_URI, ListsQuery.PROJECTION, null,
                null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    interface ListsQuery {
        String[] PROJECTION = new String[] {
                Lists._ID, Lists.NAME
        };
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
