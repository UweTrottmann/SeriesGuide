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

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.migration.MigrationActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Helps the user to get familiar with the basic functions of SeriesGuide. Shown
 * only on first start up.
 *
 * @author Uwe Trottmann
 */
public class FirstRunFragment extends SherlockFragment {

    private static final String PREF_KEY_FIRSTRUN = "accepted_eula";

    protected static final String TAG = "First Run";

    private OnFirstRunDismissedListener mListener;

    public static FirstRunFragment newInstance() {
        FirstRunFragment f = new FirstRunFragment();
        return f;
    }

    public interface OnFirstRunDismissedListener {
        public void onFirstRunDismissed();
    }

    public static boolean hasSeenFirstRunFragment(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KEY_FIRSTRUN, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.firstrun_fragment, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnFirstRunDismissedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFirstRunDismissedListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // add button
        getView().findViewById(R.id.buttonFirstRunAddShow).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fireTrackerEvent("Add show");
                startActivity(new Intent(getActivity(), AddActivity.class));
                setFirstRunDismissed();
            }
        });

        // migrate button
        getView().findViewById(R.id.buttonFirstRunMigrate).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), MigrationActivity.class));
                setFirstRunDismissed();
            }
        });

        // language chooser
        Spinner spinner = (Spinner) getView().findViewById(R.id.welcome_setuplanguage);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnLanguageSelectedListener());

        // trakt connect button
        getView().findViewById(R.id.welcome_setuptrakt).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fireTrackerEvent("Connect trakt");

                Intent i = new Intent(getActivity(), ConnectTraktActivity.class);
                startActivity(i);
            }
        });

        // dismiss button
        View buttonDismiss = getView().findViewById(R.id.buttonFirstRunDismiss);
        CheatSheet.setup(buttonDismiss);
        buttonDismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fireTrackerEvent("Dismiss");
                setFirstRunDismissed();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), TAG);
    }

    private void setFirstRunDismissed() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(PREF_KEY_FIRSTRUN, true).commit();

        // display shows fragment again, better use an interface!
        mListener.onFirstRunDismissed();
    }

    public class OnLanguageSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            final String value = getResources().getStringArray(R.array.languageData)[pos];
            prefs.edit().putString(SeriesGuidePreferences.KEY_LANGUAGE, value).commit();
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    private void fireTrackerEvent(String label) {
        Utils.trackClick(getActivity(), TAG, label);
    }
}
