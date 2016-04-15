
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
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.interfaces.OnListsChangedListener;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.util.Utils;
import java.util.HashSet;

/**
 * Displays a dialog to add a new list to lists.
 */
public class AddListDialogFragment extends DialogFragment {

    public static AddListDialogFragment newInstance() {
        return new AddListDialogFragment();
    }

    @Bind(R.id.textInputLayoutListManageListName) TextInputLayout textInputLayoutName;
    @Bind(R.id.buttonNegative) Button buttonNegative;
    @Bind(R.id.buttonPositive) Button buttonPositive;
    private OnListsChangedListener listsChangedListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.dialog_list_manage, container, false);
        ButterKnife.bind(this, layout);

        // title
        final EditText editTextName = textInputLayoutName.getEditText();
        if (editTextName != null) {
            editTextName.addTextChangedListener(
                    new ListNameTextWatcher(getContext(),
                            textInputLayoutName, buttonPositive, null));
        }

        // buttons
        buttonNegative.setText(android.R.string.cancel);
        buttonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        buttonPositive.setText(R.string.list_add);
        buttonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextName == null) {
                    return;
                }

                // add list
                String listName = editTextName.getText().toString();
                ContentValues values = new ContentValues();
                values.put(Lists.LIST_ID, Lists.generateListId(listName));
                values.put(Lists.NAME, listName);
                getActivity().getContentResolver().insert(Lists.CONTENT_URI, values);

                // refresh view pager
                listsChangedListener.onListsChanged();

                dismiss();
            }
        });
        buttonPositive.setEnabled(false);

        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listsChangedListener = (OnListsChangedListener) activity;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.unbind(this);
    }

    /**
     * Display a dialog which allows to edit the title of this list or remove it.
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

    /**
     * Disables the given button if the watched text has only whitespace or the list name is already
     * used. Does currently not protect against a new list resulting in the same list id (if
     * inserted just resets the properties of the existing list).
     */
    public static class ListNameTextWatcher implements TextWatcher {
        private Context context;
        private TextInputLayout textInputLayoutName;
        private TextView buttonPositive;
        private final HashSet<String> listNames;
        @Nullable
        private String currentName;

        public ListNameTextWatcher(Context context, TextInputLayout textInputLayoutName,
                TextView buttonPositive, @Nullable String currentName) {
            this.context = context;
            this.textInputLayoutName = textInputLayoutName;
            this.buttonPositive = buttonPositive;
            this.currentName = currentName;
            Cursor listNameQuery = context.getContentResolver()
                    .query(Lists.CONTENT_URI, new String[] { Lists._ID, Lists.NAME }, null, null,
                            null);
            listNames = new HashSet<>();
            if (listNameQuery != null) {
                while (listNameQuery.moveToNext()) {
                    listNames.add(listNameQuery.getString(1));
                }
                listNameQuery.close();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String name = s.toString().trim();
            if (name.length() == 0) {
                buttonPositive.setEnabled(false);
                return;
            }
            if (currentName != null && currentName.equals(name)) {
                buttonPositive.setEnabled(true);
                return;
            }
            if (listNames.contains(name)) {
                textInputLayoutName.setError(
                        context.getString(R.string.error_name_already_exists));
                textInputLayoutName.setErrorEnabled(true);
                buttonPositive.setEnabled(false);
            } else {
                textInputLayoutName.setError(null);
                textInputLayoutName.setErrorEnabled(false);
                buttonPositive.setEnabled(true);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
