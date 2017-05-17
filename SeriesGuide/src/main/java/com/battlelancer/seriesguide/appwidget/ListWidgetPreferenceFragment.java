package com.battlelancer.seriesguide.appwidget;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.WidgetSettings;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Shows settings fragment for a specific app widget, hosted inside a {@link ListWidgetConfigure}
 * activity.
 */
public class ListWidgetPreferenceFragment extends PreferenceFragment {

    private ListPreference typePref;
    private ListPreference sortPref;
    private CheckBoxPreference onlyFavoritesPref;
    private CheckBoxPreference onlyCollectedPref;
    private CheckBoxPreference hideWatchedPreference;
    private ListPreference themePref;
    private ListPreference backgroundPref;

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
        setHasOptionsMenu(true);

        int appWidgetId = getArguments().getInt("appWidgetId");

        // widget type setting
        typePref = new ListPreference(getActivity());
        typePref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId);
        typePref.setTitle(R.string.pref_widget_type);
        typePref.setEntries(R.array.widgetType);
        typePref.setEntryValues(R.array.widgetTypeData);
        typePref.setDefaultValue(getString(R.string.widget_default_type));
        typePref.setPositiveButtonText(null);
        typePref.setNegativeButtonText(null);

        // widget show type sort order setting
        sortPref = new ListPreference(getActivity());
        sortPref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_SHOWS_SORT_ORDER + appWidgetId);
        sortPref.setTitle(R.string.action_shows_sort);
        sortPref.setEntries(R.array.widgetShowSortOrder);
        sortPref.setEntryValues(R.array.widgetShowSortOrderData);
        sortPref.setDefaultValue(getString(R.string.widget_default_show_sort_order));
        sortPref.setPositiveButtonText(null);
        sortPref.setNegativeButtonText(null);

        // only favorite shows setting
        onlyFavoritesPref = new CheckBoxPreference(getActivity());
        onlyFavoritesPref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_ONLY_FAVORITES + appWidgetId);
        onlyFavoritesPref.setTitle(R.string.only_favorites);
        onlyFavoritesPref.setDefaultValue(false);

        // only collected episodes setting
        onlyCollectedPref = new CheckBoxPreference(getActivity());
        onlyCollectedPref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_ONLY_COLLECTED + appWidgetId);
        onlyCollectedPref.setTitle(R.string.calendar_only_collected);
        onlyCollectedPref.setDefaultValue(false);

        // hide watched setting
        hideWatchedPreference = new CheckBoxPreference(getActivity());
        hideWatchedPreference.setKey(WidgetSettings.KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId);
        hideWatchedPreference.setTitle(R.string.hide_watched);
        hideWatchedPreference.setDefaultValue(true);

        // widget theme setting
        themePref = new ListPreference(getActivity());
        themePref.setKey(WidgetSettings.KEY_PREFIX_WIDGET_THEME + appWidgetId);
        themePref.setTitle(R.string.pref_theme);
        themePref.setEntries(R.array.widgetTheme);
        themePref.setEntryValues(R.array.widgetThemeData);
        themePref.setDefaultValue("0");
        themePref.setPositiveButtonText(null);
        themePref.setNegativeButtonText(null);

        // background setting
        backgroundPref = new ListPreference(getActivity());
        backgroundPref.setKey(
                WidgetSettings.KEY_PREFIX_WIDGET_BACKGROUND_OPACITY + appWidgetId);
        backgroundPref.setTitle(R.string.pref_widget_opacity);
        backgroundPref.setEntries(R.array.widgetOpacity);
        backgroundPref.setEntryValues(R.array.widgetOpacityData);
        backgroundPref.setDefaultValue(WidgetSettings.DEFAULT_WIDGET_BACKGROUND_OPACITY);
        backgroundPref.setPositiveButtonText(null);
        backgroundPref.setNegativeButtonText(null);

        // use the settings file specific to widgets
        getPreferenceManager().setSharedPreferencesName(WidgetSettings.SETTINGS_FILE);
        getPreferenceManager().setSharedPreferencesMode(0);
        // create a widget specific settings screen
        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                getActivity());

        // add preferences to screen
        preferenceScreen.addPreference(typePref);
        preferenceScreen.addPreference(sortPref);
        preferenceScreen.addPreference(onlyFavoritesPref);
        preferenceScreen.addPreference(onlyCollectedPref);
        preferenceScreen.addPreference(hideWatchedPreference);
        preferenceScreen.addPreference(themePref);
        preferenceScreen.addPreference(backgroundPref);
        setPreferenceScreen(preferenceScreen);

        // after restoring any values prevent persisting changes (only save on user command)
        typePref.setPersistent(false);
        sortPref.setPersistent(false);
        onlyFavoritesPref.setPersistent(false);
        onlyCollectedPref.setPersistent(false);
        hideWatchedPreference.setPersistent(false);
        themePref.setPersistent(false);
        backgroundPref.setPersistent(false);

        bindPreferenceSummaryToValue(typePref);
        bindPreferenceSummaryToValue(sortPref);
        bindPreferenceSummaryToValue(backgroundPref);
        bindPreferenceSummaryToValue(themePref);

        if (!Utils.hasAccessToX(getActivity())) {
            // disable saving prefs not available for non-supporters
            Preference.OnPreferenceChangeListener onDisablePreferenceChangeListener
                    = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Utils.advertiseSubscription(getActivity());
                    return false;
                }
            };
            typePref.setOnPreferenceChangeListener(onDisablePreferenceChangeListener);
            typePref.setSummary(R.string.onlyx);
            themePref.setOnPreferenceChangeListener(onDisablePreferenceChangeListener);
            themePref.setSummary(R.string.onlyx);
            backgroundPref.setOnPreferenceChangeListener(onDisablePreferenceChangeListener);
            backgroundPref.setSummary(R.string.onlyx);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // trigger the listener to handle the current state
        preferenceChangeListener.onPreferenceChange(typePref, typePref.getValue());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.widget_config_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_save) {
            save(); // disabled persistence, so save ourselves
            ((ListWidgetConfigure) getActivity()).updateWidget();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
        editor.putString(typePref.getKey(), typePref.getValue());
        editor.putString(sortPref.getKey(), sortPref.getValue());
        editor.putBoolean(onlyFavoritesPref.getKey(), onlyFavoritesPref.isChecked());
        editor.putBoolean(onlyCollectedPref.getKey(), onlyCollectedPref.isChecked());
        editor.putBoolean(hideWatchedPreference.getKey(), hideWatchedPreference.isChecked());
        editor.putString(themePref.getKey(), themePref.getValue());
        editor.putString(backgroundPref.getKey(), backgroundPref.getValue());
        editor.apply();
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    public void bindPreferenceSummaryToValue(ListPreference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(preferenceChangeListener);

        // Trigger the listener immediately with the preference's current value.
        preferenceChangeListener.onPreferenceChange(preference, preference.getValue());
    }

    private Preference.OnPreferenceChangeListener preferenceChangeListener =
            new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // disable episode related setting if selecting show widget type
            if (typePref.getKey().equals(preference.getKey())) {
                String newTypeValue = (String) newValue;
                boolean displayingShows = getString(R.string.widget_type_shows)
                        .equals(newTypeValue);
                sortPref.setEnabled(displayingShows);
                onlyCollectedPref.setEnabled(!displayingShows);
                hideWatchedPreference.setEnabled(!displayingShows);
            }

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
}
