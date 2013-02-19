/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;

import com.google.analytics.tracking.android.EasyTracker;

/**
 * A dialog displaying various given sorting methods, saving them to the given
 * preference upon selection by the user.
 */
public class SortDialogFragment extends DialogFragment {

    public static SortDialogFragment newInstance(int itemArrayResource, int itemDataArrayResource,
            int selectedItemIndex, String preferenceKey, int dialogTitleResource) {
        SortDialogFragment f = new SortDialogFragment();

        Bundle args = new Bundle();
        args.putInt("itemarray", itemArrayResource);
        args.putInt("itemdata", itemDataArrayResource);
        args.putInt("selected", selectedItemIndex);
        args.putString("prefkey", preferenceKey);
        args.putInt("dialogtitle", dialogTitleResource);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().sendView("Sort Dialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final CharSequence[] items = getResources().getStringArray(
                getArguments().getInt("itemarray"));

        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(getArguments().getInt("dialogtitle")))
                .setSingleChoiceItems(items, getArguments().getInt("selected"),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                final SharedPreferences.Editor editor = PreferenceManager
                                        .getDefaultSharedPreferences(getActivity()).edit();
                                editor.putString(
                                        getArguments().getString("prefkey"),
                                        (getResources().getStringArray(getArguments().getInt(
                                                "itemdata")))[item]);
                                editor.commit();
                                dismiss();
                            }
                        }).create();
    }
}
