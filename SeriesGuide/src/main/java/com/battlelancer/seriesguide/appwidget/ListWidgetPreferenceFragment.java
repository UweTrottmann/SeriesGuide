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
        ListPreference typePref = new ListPreference(getActivity());
        typePref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId);
        typePref.setTitle(R.string.pref_widget_type);
        typePref.setEntries(R.array.widgetType);
        typePref.setEntryValues(R.array.widgetTypeData);
        typePref.setDefaultValue("0");
        typePref.setPositiveButtonText(null);
        typePref.setNegativeButtonText(null);
        preferenceScreen.addPreference(typePref);

        // only favorite shows setting
        CheckBoxPreference onlyFavoritesPref = new CheckBoxPreference(getActivity());
        onlyFavoritesPref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_ONLY_FAVORITES + appWidgetId);
        onlyFavoritesPref.setTitle(R.string.only_favorites);
        onlyFavoritesPref.setDefaultValue(false);
        preferenceScreen.addPreference(onlyFavoritesPref);

        // hide watched setting
        CheckBoxPreference hideWatchedPreference = new CheckBoxPreference(getActivity());
        hideWatchedPreference.setKey(WidgetSettings.KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId);
        hideWatchedPreference.setTitle(R.string.hide_watched);
        hideWatchedPreference.setDefaultValue(true);
        preferenceScreen.addPreference(hideWatchedPreference);

        // widget theme setting
        ListPreference themePref = new ListPreference(getActivity());
        themePref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_THEME + appWidgetId);
        themePref.setTitle(R.string.pref_theme);
        themePref.setEntries(R.array.widgetTheme);
        themePref.setEntryValues(R.array.widgetThemeData);
        themePref.setDefaultValue("0");
        themePref.setPositiveButtonText(null);
        themePref.setNegativeButtonText(null);
        preferenceScreen.addPreference(themePref);

        // background setting
        ListPreference backgroundPref = new ListPreference(getActivity());
        backgroundPref.setKey(
                WidgetSettings.KEY_PREFIX_WIDGET_BACKGROUND_OPACITY + appWidgetId);
        backgroundPref.setTitle(R.string.pref_widget_opacity);
        backgroundPref.setEntries(R.array.widgetOpacity);
        backgroundPref.setEntryValues(R.array.widgetOpacityData);
        backgroundPref.setDefaultValue("50");
        backgroundPref.setPositiveButtonText(null);
        backgroundPref.setNegativeButtonText(null);
        preferenceScreen.addPreference(backgroundPref);

        setPreferenceScreen(preferenceScreen);

        bindPreferenceSummaryToValue(getPreferenceManager().getSharedPreferences(), typePref);
        bindPreferenceSummaryToValue(getPreferenceManager().getSharedPreferences(), backgroundPref);
        bindPreferenceSummaryToValue(getPreferenceManager().getSharedPreferences(), themePref);

        // disable type and background pref for non-supporters
        if (!Utils.hasAccessToX(getActivity())) {
            typePref.setEnabled(false);
            typePref.setSummary(R.string.onlyx);
            themePref.setEnabled(false);
            themePref.setSummary(R.string.onlyx);
            backgroundPref.setEnabled(false);
            backgroundPref.setSummary(R.string.onlyx);
        }
    }

}
