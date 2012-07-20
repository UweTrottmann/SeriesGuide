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

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.SearchResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * A DialogFragment allowing the user to decide whether to add a show to his
 * show database.
 * 
 * @author Uwe Trottmann
 */
public class AddDialogFragment extends DialogFragment {

    public interface OnAddShowListener {
        public void onAddShow(SearchResult show);
    }

    private OnAddShowListener mListener;

    public static AddDialogFragment newInstance(SearchResult show) {
        AddDialogFragment f = new AddDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable("show", show);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAddShowListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAddShowListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SearchResult show = getArguments().getParcelable("show");

        return new AlertDialog.Builder(getActivity()).setTitle(show.title)
                .setMessage(show.overview)
                .setPositiveButton(R.string.add_show, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mListener.onAddShow(show);
                    }
                }).setNegativeButton(R.string.dont_add_show, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
    }

    /**
     * Display a dialog which asks if the user wants to add the given show to
     * his show database. If necessary an AsyncTask will be started which takes
     * care of adding the show.
     * 
     * @param show
     * @param fm
     */
    public static void showAddDialog(SearchResult show, FragmentManager fm) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = AddDialogFragment.newInstance(show);
        newFragment.show(ft, "dialog");
    }
}
