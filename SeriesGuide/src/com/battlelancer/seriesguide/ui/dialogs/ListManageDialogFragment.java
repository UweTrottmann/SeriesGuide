
package com.battlelancer.seriesguide.ui.dialogs;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Lists;

public class ListManageDialogFragment extends DialogFragment {

    public static ListManageDialogFragment newInstance(String listId) {
        ListManageDialogFragment f = new ListManageDialogFragment();

        Bundle args = new Bundle();
        args.putString("listid", listId);
        f.setArguments(args);

        return f;
    }

    private EditText mTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, R.style.SeriesGuideTheme_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.list_manage_dialog, null);

        // title
        mTitle = (EditText) layout.findViewById(R.id.title);

        // buttons
        Button buttonNegative = (Button) layout.findViewById(R.id.buttonNegative);
        buttonNegative.setText(R.string.list_remove);
        buttonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // remove list and items
                String listId = getArguments().getString("listid");
                getActivity().getContentResolver().delete(Lists.buildListUri(listId), null, null);
                getActivity().getContentResolver().delete(ListItems.CONTENT_URI,
                        Lists.LIST_ID + "=?", new String[] {
                            listId
                        });

                // TODO remove tab from view pager

                dismiss();
            }
        });
        Button buttonPositive = (Button) layout.findViewById(R.id.buttonPositive);
        buttonPositive.setText(android.R.string.ok);
        buttonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // update title
                String listId = getArguments().getString("listid");
                ContentValues values = new ContentValues();
                values.put(Lists.NAME, mTitle.getText().toString());
                getActivity().getContentResolver().update(Lists.buildListUri(listId), values, null,
                        null);

                // TODO refresh view pager

                dismiss();
            }
        });

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);

        String listId = getArguments().getString("listid");
        final Cursor list = getActivity().getContentResolver()
                .query(Lists.buildListUri(listId), new String[] {
                        Lists.NAME
                }, null, null, null);
        list.moveToFirst();
        
        mTitle.setText(list.getString(0));
        
        list.close();
    }

    /**
     * Display a dialog which allows to edit the title of this list or remove
     * it.
     */
    public static void showListManageDialog(String listId, FragmentManager fm) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("listmanagedialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = ListManageDialogFragment.newInstance(listId);
        newFragment.show(ft, "listmanagedialog");
    }
}
