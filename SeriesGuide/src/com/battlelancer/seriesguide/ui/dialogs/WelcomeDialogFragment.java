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

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

public class WelcomeDialogFragment extends DialogFragment {

    private static final String TAG = "WelcomeDialogFragment";

    public static boolean hasSeenWelcomeDialog(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean("accepted_eula", false);
    }

    public static WelcomeDialogFragment newInstance() {
        WelcomeDialogFragment f = new WelcomeDialogFragment();
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().trackView("Welcome Dialog");
    }

    public static void showWelcomeDialog(FragmentActivity activity) {
        // show welcome dialog
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("welcome-dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        WelcomeDialogFragment newFragment = WelcomeDialogFragment.newInstance();
        newFragment.show(ft, "welcome-dialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.welcome_dialog, null, false);

        // language chooser
        Spinner spinner = (Spinner) v.findViewById(R.id.welcome_setuplanguage);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnLanguageSelectedListener());

        // trakt connect button
        v.findViewById(R.id.welcome_setuptrakt).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EasyTracker.getTracker().trackEvent(TAG, "Click", "Setup trakt account", (long) 0);
                TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                        .newInstance();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                newFragment.show(ft, "traktdialog");
            }
        });

        final CheckBox cb = (CheckBox) v.findViewById(R.id.welcome_sendusagedata);
        final FragmentActivity activity = getActivity();

        return new AlertDialog.Builder(activity).setIcon(R.drawable.icon)
                .setTitle(R.string.welcome_message).setView(v)
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                SharedPreferences sp = PreferenceManager
                                        .getDefaultSharedPreferences(activity);
                                sp.edit()
                                        .putBoolean("accepted_eula", true)
                                        .putBoolean(SeriesGuidePreferences.KEY_GOOGLEANALYTICS,
                                                cb.isChecked()).commit();
                                // GA opt-out right here
                                GoogleAnalytics.getInstance(activity).setAppOptOut(cb.isChecked());
                                return null;
                            }
                        }.execute();
                    }
                }).setCancelable(false).create();
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
}
