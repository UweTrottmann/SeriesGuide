package com.battlelancer.seriesguide.appwidget;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
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
 * Shows settings fragment for a specific app widget, hosted inside a {@link ListWidgetConfigure}
 * activity.
 */
public class ListWidgetPreferenceFragment extends BaseSettingsFragment {

    @SuppressWarnings("FieldCanBeLocal") private SharedPreferences.OnSharedPreferenceChangeListener
            preferenceChangeListener;

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
        final ListPreference typePref = new ListPreference(getActivity());
        typePref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId);
        typePref.setTitle(R.string.pref_widget_type);
        typePref.setEntries(R.array.widgetType);
        typePref.setEntryValues(R.array.widgetTypeData);
        typePref.setDefaultValue(getString(R.string.widget_default_type));
        typePref.setPositiveButtonText(null);
        typePref.setNegativeButtonText(null);
        preferenceScreen.addPreference(typePref);

        // widget show type sort order setting
        final ListPreference sortPref = new ListPreference(getActivity());
        sortPref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_SHOWS_SORT_ORDER + appWidgetId);
        sortPref.setTitle(R.string.action_shows_sort);
        sortPref.setEntries(R.array.widgetShowSortOrder);
        sortPref.setEntryValues(R.array.widgetShowSortOrderData);
        sortPref.setDefaultValue(getString(R.string.widget_default_show_sort_order));
        sortPref.setPositiveButtonText(null);
        sortPref.setNegativeButtonText(null);
        preferenceScreen.addPreference(sortPref);

        // only favorite shows setting
        final CheckBoxPreference onlyFavoritesPref = new CheckBoxPreference(getActivity());
        onlyFavoritesPref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_ONLY_FAVORITES + appWidgetId);
        onlyFavoritesPref.setTitle(R.string.only_favorites);
        onlyFavoritesPref.setDefaultValue(false);
        preferenceScreen.addPreference(onlyFavoritesPref);

        // hide watched setting
        final CheckBoxPreference hideWatchedPreference = new CheckBoxPreference(getActivity());
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
        backgroundPref.setDefaultValue(WidgetSettings.DEFAULT_WIDGET_BACKGROUND_OPACITY);
        backgroundPref.setPositiveButtonText(null);
        backgroundPref.setNegativeButtonText(null);
        preferenceScreen.addPreference(backgroundPref);

        setPreferenceScreen(preferenceScreen);

        bindPreferenceSummaryToValue(getPreferenceManager().getSharedPreferences(), typePref);
        bindPreferenceSummaryToValue(getPreferenceManager().getSharedPreferences(), sortPref);
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

        // disable episode related setting if selecting show widget type
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                    String key) {
                if (!isAdded()) {
                    return; // no longer attached to activity
                }
                if (typePref.getKey().equals(key)) {
                    String newTypeValue = typePref.getValue();
                    boolean displayingShows = getString(R.string.widget_type_shows)
                            .equals(newTypeValue);
                    sortPref.setEnabled(displayingShows);
                    hideWatchedPreference.setEnabled(!displayingShows);
                }
            }
        };
        // trigger the listener to handle the current state
        preferenceChangeListener.onSharedPreferenceChanged(
                getPreferenceManager().getSharedPreferences(), typePref.getKey());
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}
