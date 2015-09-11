/*
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

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.NotificationSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.ThemeUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.analytics.GoogleAnalytics;

/**
 * Allows tweaking of various SeriesGuide settings. Does NOT inherit from {@link
 * com.battlelancer.seriesguide.ui.BaseActivity} to avoid handling actions which might be confusing
 * while adjusting settings.
 */
public class SeriesGuidePreferences extends AppCompatActivity {

    private static final String EXTRA_SETTINGS_SCREEN = "settingsScreen";

    private static final String TAG = "Settings";

    // Preference keys
    private static final String KEY_CLEAR_CACHE = "clearCache";

    public static final String KEY_OFFSET = "com.battlelancer.seriesguide.timeoffset";

    public static final String KEY_DATABASEIMPORTED = "com.battlelancer.seriesguide.dbimported";

    public static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";

    public static final String SUPPORT_MAIL = "support@seriesgui.de";

    private static final String KEY_ABOUT = "aboutPref";

    public static final String KEY_TAPE_INTERVAL = "com.battlelancer.seriesguide.tapeinterval";

    public static @StyleRes int THEME = R.style.Theme_SeriesGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            Fragment f = new SettingsFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.content_frame, f);
            ft.commit();

            // open a sub settings screen if requested
            String settingsScreen = getIntent().getStringExtra(EXTRA_SETTINGS_SCREEN);
            if (settingsScreen != null) {
                switchToSettings(settingsScreen);
            }
        }
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.sgToolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public void onBackPressed() {
        // Because we use the platform fragment manager we need to pop fragments on our own
        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void switchToSettings(String settingsId) {
        Bundle args = new Bundle();
        args.putString(EXTRA_SETTINGS_SCREEN, settingsId);
        Fragment f = new SettingsFragment();
        f.setArguments(args);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, f);
        ft.addToBackStack(null);
        ft.commit();
    }

    private static OnPreferenceChangeListener sNoOpChangeListener
            = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Utils.advertiseSubscription(preference.getContext());
            // prevent value from getting saved
            return false;
        }
    };

    /**
     * Resets and runs the notification service to take care of potential time shifts when e.g.
     * changing the time offset.
     */
    private static void resetAndRunNotificationsService(Context context) {
        NotificationService.resetLastEpisodeAirtime(PreferenceManager
                .getDefaultSharedPreferences(context));
        Utils.runNotificationService(context);
    }

    public static void setListPreferenceSummary(ListPreference listPref) {
        // Set summary to be the user-description for the selected value
        listPref.setSummary(listPref.getEntry().toString().replaceAll("%", "%%"));
    }

    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        public static final String SETTINGS_SCREEN_BASIC = "screen_basic";

        private static final int REQUEST_CODE_RINGTONE = 0;
        private static final int REQUEST_CODE_AUTO_BACKUP = 1;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments() == null ? null
                    : getArguments().getString(EXTRA_SETTINGS_SCREEN);
            if (settings == null) {
                addPreferencesFromResource(R.xml.settings_root);
                setupRootSettings();
            } else if (settings.equals(SETTINGS_SCREEN_BASIC)) {
                addPreferencesFromResource(R.xml.settings_basic);
                setupBasicSettings();
            } else if (settings.equals("screen_notifications")) {
                addPreferencesFromResource(R.xml.settings_notifications);
                setupNotificationSettings();
            } else if (settings.equals("screen_advanced")) {
                addPreferencesFromResource(R.xml.settings_advanced);
                setupAdvancedSettings();
            } else if (settings.equals("screen_about")) {
                addPreferencesFromResource(R.xml.settings_about);
                setupAboutSettings();
            }
        }

        private void setupRootSettings() {
            // show currently set values for list prefs
            setListPreferenceSummary((ListPreference) findPreference(DisplaySettings.KEY_LANGUAGE));

            // set current value of auto-update pref
            ((CheckBoxPreference) findPreference(UpdateSettings.KEY_AUTOUPDATE)).setChecked(
                    SgSyncAdapter.isSyncAutomatically(getActivity()));
        }

        private void setupNotificationSettings() {
            Preference enabledPref = findPreference(NotificationSettings.KEY_ENABLED);
            final Preference thresholdPref = findPreference(NotificationSettings.KEY_THRESHOLD);
            final Preference favOnlyPref = findPreference(NotificationSettings.KEY_FAVONLY);
            final Preference vibratePref = findPreference(NotificationSettings.KEY_VIBRATE);
            final Preference ringtonePref = findPreference(NotificationSettings.KEY_RINGTONE);

            // allow supporters to enable notifications
            if (Utils.hasAccessToX(getActivity())) {
                enabledPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        boolean isChecked = ((CheckBoxPreference) preference).isChecked();
                        if (isChecked) {
                            Utils.trackCustomEvent(getActivity(), TAG, "Notifications", "Enable");
                        } else {
                            Utils.trackCustomEvent(getActivity(), TAG, "Notifications", "Disable");
                        }

                        thresholdPref.setEnabled(isChecked);
                        favOnlyPref.setEnabled(isChecked);
                        vibratePref.setEnabled(isChecked);
                        ringtonePref.setEnabled(isChecked);

                        Utils.runNotificationService(getActivity());
                        return true;
                    }
                });
                favOnlyPref.setOnPreferenceClickListener(
                        new OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                resetAndRunNotificationsService(getActivity());
                                return true;
                            }
                        });
                // disable advanced notification settings if notifications are disabled
                boolean isNotificationsEnabled = NotificationSettings.isNotificationsEnabled(
                        getActivity());
                thresholdPref.setEnabled(isNotificationsEnabled);
                favOnlyPref.setEnabled(isNotificationsEnabled);
                vibratePref.setEnabled(isNotificationsEnabled);
                ringtonePref.setEnabled(isNotificationsEnabled);
            } else {
                enabledPref.setOnPreferenceChangeListener(sNoOpChangeListener);
                ((CheckBoxPreference) enabledPref).setChecked(false);
                enabledPref.setSummary(R.string.onlyx);
                thresholdPref.setEnabled(false);
                favOnlyPref.setEnabled(false);
                vibratePref.setEnabled(false);
                ringtonePref.setEnabled(false);
            }

            setListPreferenceSummary((ListPreference) thresholdPref);
        }

        private void setupBasicSettings() {
            // No aired episodes
            findPreference(DisplaySettings.KEY_NO_RELEASED_EPISODES).setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {

                        public boolean onPreferenceClick(Preference preference) {
                            if (((CheckBoxPreference) preference).isChecked()) {
                                Utils.trackCustomEvent(getActivity(), TAG, "OnlyFutureEpisodes",
                                        "Enable");
                            } else {
                                Utils.trackCustomEvent(getActivity(), TAG, "OnlyFutureEpisodes",
                                        "Disable");
                            }
                            return false;
                        }
                    });

            // No special episodes
            findPreference(DisplaySettings.KEY_HIDE_SPECIALS).setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {

                        public boolean onPreferenceClick(Preference preference) {
                            if (((CheckBoxPreference) preference).isChecked()) {
                                Utils.trackCustomEvent(getActivity(), TAG, "OnlySeasonEpisodes",
                                        "Enable");
                            } else {
                                Utils.trackCustomEvent(getActivity(), TAG, "OnlySeasonEpisodes",
                                        "Disable");
                            }
                            return false;
                        }
                    });

            // Theme switcher
            Preference themePref = findPreference(DisplaySettings.KEY_THEME);
            if (Utils.hasAccessToX(getActivity())) {
                themePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (DisplaySettings.KEY_THEME.equals(preference.getKey())) {
                            ThemeUtils.updateTheme((String) newValue);

                            // restart to apply new theme, go back to this settings screen
                            TaskStackBuilder.create(getActivity())
                                    .addNextIntent(new Intent(getActivity(), ShowsActivity.class))
                                    .addNextIntent(getActivity().getIntent()
                                            .putExtra(EXTRA_SETTINGS_SCREEN, SETTINGS_SCREEN_BASIC))
                                    .startActivities();
                        }
                        return true;
                    }
                });
                setListPreferenceSummary((ListPreference) themePref);
            } else {
                themePref.setOnPreferenceChangeListener(sNoOpChangeListener);
                themePref.setSummary(R.string.onlyx);
            }

            // show currently set values for list prefs
            setListPreferenceSummary(
                    (ListPreference) findPreference(AdvancedSettings.KEY_UPCOMING_LIMIT));
            setListPreferenceSummary(
                    (ListPreference) findPreference(DisplaySettings.KEY_NUMBERFORMAT));
            ListPreference offsetListPref = (ListPreference) findPreference(KEY_OFFSET);
            offsetListPref.setSummary(getString(R.string.pref_offsetsummary,
                    offsetListPref.getEntry()));
        }

        private void setupAdvancedSettings() {
            // Clear image cache
            findPreference(KEY_CLEAR_CACHE)
                    .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                        public boolean onPreferenceClick(Preference preference) {
                            // try to open app info where user can clear app cache folders
                            Intent intent = new Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                            if (!Utils.tryStartActivity(getActivity(), intent, false)) {
                                // try to open all apps view if detail view not available
                                intent = new Intent(
                                        android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                Utils.tryStartActivity(getActivity(), intent, true);
                            }

                            return true;
                        }
                    });

            // GA opt-out
            findPreference(AppSettings.KEY_GOOGLEANALYTICS).setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (preference.getKey().equals(AppSettings.KEY_GOOGLEANALYTICS)) {
                                boolean isEnabled = (Boolean) newValue;
                                GoogleAnalytics.getInstance(getActivity()).setAppOptOut(isEnabled);
                                return true;
                            }
                            return false;
                        }
                    });
        }

        private void setupAboutSettings() {
            // display version number and database version in About pref
            final String versionFinal = Utils.getVersion(getActivity());
            findPreference(KEY_ABOUT).setSummary("v" + versionFinal + " (Database v"
                    + SeriesGuideDatabase.DATABASE_VERSION + ")");
        }

        @Override
        public void onStart() {
            super.onStart();
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                @NonNull Preference preference) {
            String key = preference.getKey();
            if (key != null && key.startsWith("screen_")) {
                ((SeriesGuidePreferences) getActivity()).switchToSettings(key);
                return true;
            }
            if (NotificationSettings.KEY_RINGTONE.equals(key)) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                        RingtoneManager.TYPE_NOTIFICATION);
                // show silent and default options
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        Settings.System.DEFAULT_NOTIFICATION_URI);

                // restore selected sound or silent (null)
                String existingValue = NotificationSettings.getNotificationsRingtone(getActivity());
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        TextUtils.isEmpty(existingValue) ? null : Uri.parse(existingValue));

                startActivityForResult(intent, REQUEST_CODE_RINGTONE);
                return true;
            }
            if (AdvancedSettings.KEY_AUTOBACKUP.equals(key)) {
                TwoStatePreference autoBackupPref = (TwoStatePreference) preference;
                boolean isEnabled = autoBackupPref.isChecked();
                if (isEnabled) {
                    ensureAutoBackupPermission();
                }
                return true;
            }
            if (KEY_ABOUT.equals(key)) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, new AboutSettingsFragment());
                ft.addToBackStack(null);
                ft.commit();
                return true;
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (REQUEST_CODE_RINGTONE == requestCode) {
                if (data != null) {
                    Uri ringtone = data.getParcelableExtra(
                            RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                            .putString(NotificationSettings.KEY_RINGTONE,
                                    ringtone == null ? "" : ringtone.toString())
                            .apply();
                }
                return;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

        private void ensureAutoBackupPermission() {
            // make sure we have the storage write permission
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // don't have it? request it, do task if granted
                requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        REQUEST_CODE_AUTO_BACKUP);
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                @NonNull int[] grantResults) {
            if (requestCode == REQUEST_CODE_AUTO_BACKUP) {
                if (grantResults.length > 0
                        && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (getView() != null) {
                        // disable auto backup as we don't have the required permission
                        TwoStatePreference autoBackupPref = (TwoStatePreference) findPreference(
                                AdvancedSettings.KEY_AUTOBACKUP);
                        autoBackupPref.setChecked(false);
                        Snackbar.make(getView(), R.string.autobackup_permission_missing,
                                Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (pref != null) {
                new BackupManager(pref.getContext()).dataChanged();
            }

            if (AdvancedSettings.KEY_UPCOMING_LIMIT.equals(key)
                    || DisplaySettings.KEY_LANGUAGE.equals(key)
                    || DisplaySettings.KEY_NUMBERFORMAT.equals(key)
                    || DisplaySettings.KEY_THEME.equals(key)
                    || NotificationSettings.KEY_THRESHOLD.equals(key)
                    ) {
                if (pref != null) {
                    setListPreferenceSummary((ListPreference) pref);
                }
            }

            if (DisplaySettings.KEY_LANGUAGE.equals(key)) {
                // reset last edit date of all episodes so they will get updated
                new Thread(new Runnable() {
                    public void run() {
                        ContentValues values = new ContentValues();
                        values.put(Episodes.LAST_EDITED, 0);
                        getActivity().getContentResolver()
                                .update(Episodes.CONTENT_URI, values, null, null);
                    }
                }).start();
            }

            if (key.equals(KEY_OFFSET)) {
                if (pref != null) {
                    ListPreference listPref = (ListPreference) pref;
                    // Set summary to be the user-description for the selected
                    // value
                    listPref.setSummary(
                            getString(R.string.pref_offsetsummary, listPref.getEntry()));

                    resetAndRunNotificationsService(getActivity());
                }
            }

            if (NotificationSettings.KEY_THRESHOLD.equals(key)) {
                if (pref != null) {
                    resetAndRunNotificationsService(getActivity());
                }
            }

            // Toggle auto-update on SyncAdapter
            if (UpdateSettings.KEY_AUTOUPDATE.equals(key)) {
                if (pref != null) {
                    CheckBoxPreference checkBoxPref = (CheckBoxPreference) pref;
                    SgSyncAdapter.setSyncAutomatically(getActivity(), checkBoxPref.isChecked());
                }
            }
        }
    }
}
