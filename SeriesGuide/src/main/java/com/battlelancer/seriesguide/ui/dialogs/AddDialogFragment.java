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

import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.R;

import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A DialogFragment allowing the user to decide whether to add a show to his show database.
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAddShowListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAddShowListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Add Dialog");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.add_dialog, null);
        final SearchResult show = getArguments().getParcelable("show");

        // title and description
        ((TextView) layout.findViewById(R.id.title)).setText(show.title);
        ((TextView) layout.findViewById(R.id.description)).setText(show.overview);

        // buttons
        Button dontAddButton = (Button) layout.findViewById(R.id.buttonNegative);
        dontAddButton.setText(R.string.dont_add_show);
        dontAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Button addButton = (Button) layout.findViewById(R.id.buttonPositive);
        addButton.setText(R.string.action_shows_add);
        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onAddShow(show);
                dismiss();
            }
        });

        // poster
        if (DisplaySettings.isVeryLargeScreen(getActivity())) {
            if (show.poster != null) {
                ImageView posterView = (ImageView) layout.findViewById(R.id.poster);
                posterView.setVisibility(View.VISIBLE);
                ImageDownloader.getInstance(getActivity()).download(show.poster, posterView, false);
            }
        }

        return layout;
    }

    /**
     * Display a dialog which asks if the user wants to add the given show to his show database. If
     * necessary an AsyncTask will be started which takes care of adding the show.
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
