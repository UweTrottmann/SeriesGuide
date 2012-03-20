
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.ShareUtils.TraktCredentialsDialogFragment;

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

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent(TAG, "Click", label, 0);
    }

    public static WelcomeDialogFragment newInstance() {
        WelcomeDialogFragment f = new WelcomeDialogFragment();
        return f;
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
                fireTrackerEvent("Setup trakt account");
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
