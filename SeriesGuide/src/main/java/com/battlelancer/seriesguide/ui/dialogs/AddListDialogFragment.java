
package com.battlelancer.seriesguide.ui.dialogs;

import com.battlelancer.seriesguide.interfaces.OnListsChangedListener;
import com.battlelancer.seriesguide.provider.SeriesContract.Lists;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.dialogs.ListManageDialogFragment.CharAndDigitInputFilter;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Displays a dialog to add a new list to lists.
 */
public class AddListDialogFragment extends DialogFragment {

    public static AddListDialogFragment newInstance() {
        return new AddListDialogFragment();
    }

    private EditText mTitle;
    private OnListsChangedListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        if (SeriesGuidePreferences.THEME == R.style.AndroidTheme) {
            setStyle(STYLE_NO_TITLE, 0);
        } else {
            setStyle(STYLE_NO_TITLE, R.style.SeriesGuideTheme_Dialog);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.list_manage_dialog, null);

        // set alternate dialog title
        ((TextView) layout.findViewById(R.id.dialogTitle)).setText(R.string.list_add);

        // title
        mTitle = (EditText) layout.findViewById(R.id.title);
        mTitle.setFilters(new InputFilter[] {
                new CharAndDigitInputFilter()
        });

        // buttons
        Button buttonNegative = (Button) layout.findViewById(R.id.buttonNegative);
        buttonNegative.setText(android.R.string.cancel);
        buttonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Button buttonPositive = (Button) layout.findViewById(R.id.buttonPositive);
        buttonPositive.setText(R.string.list_add);
        buttonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTitle.getText().length() == 0) {
                    return;
                }

                // add list
                String listName = mTitle.getText().toString();
                ContentValues values = new ContentValues();
                values.put(Lists.LIST_ID, Lists.generateListId(listName));
                values.put(Lists.NAME, listName);
                getActivity().getContentResolver().insert(Lists.CONTENT_URI, values);

                // refresh view pager
                mListener.onListsChanged();

                dismiss();
            }
        });

        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnListsChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnListsChangedListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Add List Dialog");
    }

    /**
     * Display a dialog which allows to edit the title of this list or remove
     * it.
     */
    public static void showAddListDialog(FragmentManager fm) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("addlistdialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = AddListDialogFragment.newInstance();
        newFragment.show(ft, "addlistdialog");
    }
}
