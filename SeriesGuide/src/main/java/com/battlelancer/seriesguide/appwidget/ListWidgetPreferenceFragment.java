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

package com.battlelancer.seriesguide.appwidget;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.WidgetSettings;
import com.battlelancer.seriesguide.ui.BaseSettingsFragment;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Shows settings fragment for a specific app widget, hosted inside a
 * {@link ListWidgetConfigure} activity.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ListWidgetPreferenceFragment extends BaseSettingsFragment {

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

}
