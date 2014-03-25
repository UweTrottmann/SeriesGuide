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

package com.battlelancer.seriesguide.ui;

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
    public static void bindPreferenceSummaryToValue(SharedPreferences prefs,
            Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sSetSummaryListener);

        // Trigger the listener immediately with the preference's current value.
        sSetSummaryListener
                .onPreferenceChange(preference, prefs.getString(preference.getKey(), ""));
    }
}
