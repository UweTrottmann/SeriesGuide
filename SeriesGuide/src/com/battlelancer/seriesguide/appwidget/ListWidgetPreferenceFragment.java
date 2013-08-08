/*
 * Copyright (C) 2013 Uwe Trottmann 
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

package com.battlelancer.seriesguide.appwidget;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.battlelancer.seriesguide.settings.WidgetSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

/**
 * Shows settings fragment for a specific app widget, hosted inside a
 * {@link ListWidgetConfigure} activity.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ListWidgetPreferenceFragment extends PreferenceFragment {

    public static ListWidgetPreferenceFragment newInstance(int appWidgetId) {
        ListWidgetPreferenceFragment f = new ListWidgetPreferenceFragment();

        Bundle args = new Bundle();
        args.putInt("appWidgetId", appWidgetId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // use the settings file specific to widgets
        getPreferenceManager().setSharedPreferencesName(WidgetSettings.SETTINGS_FILE);
        getPreferenceManager().setSharedPreferencesMode(0);

        // create a widget specific settings screen
        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                getActivity());

        int appWidgetId = getArguments().getInt("appWidgetId");

        // widget type setting
        ListPreference typePreference = new ListPreference(getActivity());
        typePreference.setKey(WidgetSettings.KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId);
        typePreference.setTitle(R.string.pref_widget_type);
        typePreference.setEntries(R.array.widgetType);
        typePreference.setEntryValues(R.array.widgetTypeData);
        typePreference.setDefaultValue("0");
        typePreference.setPositiveButtonText(null);
        typePreference.setNegativeButtonText(null);
        preferenceScreen.addPreference(typePreference);

        // hide watched setting
        CheckBoxPreference hideWatchedPreference = new CheckBoxPreference(getActivity());
        hideWatchedPreference.setKey(WidgetSettings.KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId);
        hideWatchedPreference.setTitle(R.string.hide_watched);
        hideWatchedPreference.setDefaultValue(true);
        preferenceScreen.addPreference(hideWatchedPreference);

        // background setting
        ListPreference backgroundPreference = new ListPreference(getActivity());
        backgroundPreference.setKey(WidgetSettings.KEY_PREFIX_WIDGET_BACKGROUND_COLOR + appWidgetId);
        backgroundPreference.setTitle(R.string.pref_widget_opacity);
        backgroundPreference.setEntries(R.array.widgetOpacity);
        backgroundPreference.setEntryValues(R.array.widgetOpacityData);
        backgroundPreference.setDefaultValue("50");
        backgroundPreference.setPositiveButtonText(null);
        backgroundPreference.setNegativeButtonText(null);
        preferenceScreen.addPreference(backgroundPreference);

        setPreferenceScreen(preferenceScreen);

        bindPreferenceSummaryToValue(getPreferenceManager().getSharedPreferences(), typePreference);
        bindPreferenceSummaryToValue(getPreferenceManager().getSharedPreferences(),
                backgroundPreference);

        // disable type and background pref for non-supporters
        if (!Utils.hasAccessToX(getActivity())) {
            typePreference.setEnabled(false);
            typePreference.setSummary(R.string.onlyx);
            backgroundPreference.setEnabled(false);
            backgroundPreference.setSummary(R.string.onlyx);
        }
    }

    private static OnPreferenceChangeListener sSetSummaryListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                String stringValue = newValue.toString();
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? (listPreference.getEntries()[index])
                                        .toString().replaceAll("%", "%%")
                                : null);
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
    public static void bindPreferenceSummaryToValue(SharedPreferences prefs, Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sSetSummaryListener);

        // Trigger the listener immediately with the preference's current value.
        sSetSummaryListener
                .onPreferenceChange(preference, prefs.getString(preference.getKey(), ""));
    }

}
