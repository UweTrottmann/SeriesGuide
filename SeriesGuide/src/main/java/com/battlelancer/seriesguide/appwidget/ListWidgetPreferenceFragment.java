package com.battlelancer.seriesguide.appwidget;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.annotation.ArrayRes;
import android.support.annotation.StringRes;
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

    private PreferenceScreen preferenceScreen;
    private ListPreference typePref;
    private ListPreference showsSortPref;
    private CheckBoxPreference onlyCollectedPref;
    private CheckBoxPreference hideWatchedPreference;
    private CheckBoxPreference isInfinitePref;

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

        typePref = listPref(
                WidgetSettings.KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId,
                R.string.pref_widget_type,
                R.array.widgetType,
                R.array.widgetTypeData,
                getString(R.string.widget_default_type)
        );
        ListPreference themePref = listPref(
                WidgetSettings.KEY_PREFIX_WIDGET_THEME + appWidgetId,
                R.string.pref_theme,
                R.array.widgetTheme,
                R.array.widgetThemeData,
                "0"
        );
        ListPreference backgroundPref = listPref(
                WidgetSettings.KEY_PREFIX_WIDGET_BACKGROUND_OPACITY + appWidgetId,
                R.string.pref_widget_opacity,
                R.array.widgetOpacity,
                R.array.widgetOpacityData,
                WidgetSettings.DEFAULT_WIDGET_BACKGROUND_OPACITY
        );

        showsSortPref = listPref(
                WidgetSettings.KEY_PREFIX_WIDGET_SHOWS_SORT_ORDER + appWidgetId,
                R.string.action_shows_sort,
                R.array.widgetShowSortOrder,
                R.array.widgetShowSortOrderData,
                getString(R.string.widget_default_show_sort_order)
        );

        CheckBoxPreference onlyFavoritesPref = checkBoxPref(
                WidgetSettings.KEY_PREFIX_WIDGET_ONLY_FAVORITES + appWidgetId,
                R.string.only_favorites,
                false
        );
        onlyCollectedPref = checkBoxPref(
                WidgetSettings.KEY_PREFIX_WIDGET_ONLY_COLLECTED + appWidgetId,
                R.string.calendar_only_collected,
                false
        );
        hideWatchedPreference = checkBoxPref(
                WidgetSettings.KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId,
                R.string.hide_watched,
                true
        );
        isInfinitePref = checkBoxPref(
                WidgetSettings.KEY_PREFIX_WIDGET_IS_INFINITE + appWidgetId,
                R.string.pref_infinite_scrolling,
                false
        );

        // use the settings file specific to widgets
        getPreferenceManager().setSharedPreferencesName(WidgetSettings.SETTINGS_FILE);
        getPreferenceManager().setSharedPreferencesMode(0);

        // create a widget specific settings screen
        preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());

        preferenceScreen.addPreference(typePref);
        preferenceScreen.addPreference(showsSortPref);
        preferenceScreen.addPreference(onlyFavoritesPref);
        preferenceScreen.addPreference(onlyCollectedPref);
        preferenceScreen.addPreference(hideWatchedPreference);
        preferenceScreen.addPreference(isInfinitePref);

        PreferenceCategory appearanceCategory = new PreferenceCategory(getActivity());
        appearanceCategory.setTitle(R.string.pref_appearance);
        preferenceScreen.addPreference(appearanceCategory);
        appearanceCategory.addPreference(themePref);
        appearanceCategory.addPreference(backgroundPref);

        setPreferenceScreen(preferenceScreen);

        // after restoring any values prevent persisting changes (only save on user command)
        disablePersistence(preferenceScreen);

        bindPreferenceSummaryToValue(typePref);
        bindPreferenceSummaryToValue(showsSortPref);
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

    private CheckBoxPreference checkBoxPref(String key, @StringRes int titleRes,
            boolean defaultValue) {
        CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getActivity());
        checkBoxPreference.setKey(key);
        checkBoxPreference.setTitle(titleRes);
        checkBoxPreference.setDefaultValue(defaultValue);
        return checkBoxPreference;
    }

    private ListPreference listPref(String key, @StringRes int title, @ArrayRes int entries,
            @ArrayRes int values, String defaultValue) {
        ListPreference listPreference = new ListPreference(getActivity());
        listPreference.setKey(key);
        listPreference.setTitle(title);
        listPreference.setEntries(entries);
        listPreference.setEntryValues(values);
        listPreference.setDefaultValue(defaultValue);
        listPreference.setPositiveButtonText(null);
        listPreference.setNegativeButtonText(null);
        return listPreference;
    }

    private void disablePersistence(PreferenceGroup group) {
        int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                disablePersistence((PreferenceGroup) pref);
            } else {
                pref.setPersistent(false);
            }
        }
    }

    private void save() {
        SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
        savePreferences(preferenceScreen, editor);
        editor.apply();
    }

    private void savePreferences(PreferenceGroup group, SharedPreferences.Editor editor) {
        int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = group.getPreference(i);
            String key = pref.getKey();
            if (pref instanceof PreferenceGroup) {
                savePreferences((PreferenceGroup) pref, editor);
            } else if (pref instanceof CheckBoxPreference) {
                editor.putBoolean(key, ((CheckBoxPreference) pref).isChecked());
            } else if (pref instanceof ListPreference) {
                editor.putString(key, ((ListPreference) pref).getValue());
            }
        }
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
                        showsSortPref.setEnabled(displayingShows);
                        onlyCollectedPref.setEnabled(!displayingShows);
                        hideWatchedPreference.setEnabled(!displayingShows);
                        isInfinitePref.setEnabled(!displayingShows);
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
