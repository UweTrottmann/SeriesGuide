
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
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.interfaces.OnListsChangedListener;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.ui.dialogs.ListManageDialogFragment.CharAndDigitInputFilter;
import com.battlelancer.seriesguide.util.Utils;

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
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.dialog_list_manage, container, false);

        // set alternate dialog title
        ((TextView) layout.findViewById(R.id.textViewListManageDialogTitle)).setText(R.string.list_add);

        // title
        mTitle = (EditText) layout.findViewById(R.id.editTextListManageListTitle);
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
