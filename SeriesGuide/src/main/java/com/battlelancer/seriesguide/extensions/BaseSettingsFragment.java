package com.battlelancer.seriesguide.extensions;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * A {@link android.preference.PreferenceFragment} which has a helper method to easily display the
 * current settings value of a {@link android.preference.ListPreference}.
 */
public abstract class BaseSettingsFragment extends PreferenceFragment {

    private static Preference.OnPreferenceChangeListener
            sSetSummaryListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                String stringValue = newValue.toString();
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value. Escape '%'.
                preference.setSummary(
                        index >= 0
                                ? (listPreference.getEntries()[index])
                                .toString().replaceAll("%", "%%")
                                : null
                );
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    protected static void bindPreferenceSummaryToValue(SharedPreferences prefs,
            Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sSetSummaryListener);

        // Trigger the listener immediately with the preference's current value.
        sSetSummaryListener
                .onPreferenceChange(preference, prefs.getString(preference.getKey(), ""));
    }
}
