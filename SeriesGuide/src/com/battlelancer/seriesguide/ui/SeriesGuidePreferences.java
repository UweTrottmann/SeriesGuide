/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.KeyEvent;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import java.util.List;

/**
 * Allows tweaking of various SeriesGuide settings.
 */
public class SeriesGuidePreferences extends SherlockPreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private static final String KEY_CLEAR_CACHE = "clearCache";

    private static final String KEY_GETGLUE_DISCONNECT = "clearGetGlueCredentials";

    // Actions for legacy settings
    final static String ACTION_PREFS_BASIC = "com.battlelancer.seriesguide.PREFS_BASIC";

    private static final Object ACTION_PREFS_SHARING = "com.battlelancer.seriesguide.PREFS_SHARING";

    final static String ACTION_PREFS_ADVANCED = "com.battlelancer.seriesguide.PREFS_ADVANCED";

    final static String ACTION_PREFS_ABOUT = "com.battlelancer.seriesguide.PREFS_ABOUT";

    // Preference keys
    public static final String KEY_TRAKTPWD = "com.battlelancer.seriesguide.traktpwd";

    public static final String KEY_TRAKTUSER = "com.battlelancer.seriesguide.traktuser";

    public static final String KEY_ONLY_FUTURE_EPISODES = "onlyFutureEpisodes";

    public static final String KEY_ONLY_SEASON_EPISODES = "onlySeasonEpisodes";

    public static final String KEY_NUMBERFORMAT = "numberformat";

    public static final String NUMBERFORMAT_DEFAULT = "default";

    public static final String NUMBERFORMAT_ENGLISH = "english";

    public static final String NUMBERFORMAT_ENGLISHLOWER = "englishlower";

    public static final String KEY_OFFSET = "com.battlelancer.seriesguide.timeoffset";

    public static final String KEY_DATABASEIMPORTED = "com.battlelancer.seriesguide.dbimported";

    public static final String KEY_SHOW_SORT_ORDER = "showSorting";

    public static final String KEY_SEASON_SORT_ORDER = "seasonSorting";

    public static final String KEY_EPISODE_SORT_ORDER = "episodeSorting";

    public static final String KEY_SHOWFILTER = "com.battlelancer.seriesguide.showfilter";

    public static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";

    public static final String KEY_UPDATEATLEASTEVERY = "com.battlelancer.seriesguide.updateatleastevery";

    public static final String KEY_VERSION = "oldversioncode";

    public static final String KEY_HIDEIMAGES = "hideimages";

    public static final String KEY_GOOGLEANALYTICS = "enableGAnalytics";

    public static final String KEY_AUTOUPDATE = "com.battlelancer.seriesguide.autoupdate";

    public static final String KEY_ONLYWIFI = "com.battlelancer.seriesguide.autoupdatewlanonly";

    public static final String KEY_LASTUPDATE = "com.battlelancer.seriesguide.lastupdate";

    public static final String KEY_LASTTRAKTUPDATE = "com.battlelancer.seriesguide.lasttraktupdate";

    public static final String KEY_ONLYFAVORITES = "com.battlelancer.seriesguide.onlyfavorites";

    public static final String KEY_NOWATCHED = "com.battlelancer.seriesguide.activity.nowatched";

    public static final String KEY_UPCOMING_LIMIT = "com.battlelancer.seriesguide.upcominglimit";

    public static final String KEY_NOTIFICATIONS_ENABLED = "com.battlelancer.seriesguide.notifications";

    public static final String KEY_NOTIFICATIONS_FAVONLY = "com.battlelancer.seriesguide.notifications.favonly";

    public static final String KEY_NOTIFICATIONS_THRESHOLD = "com.battlelancer.seriesguide.notifications.threshold";

    public static final String KEY_NOTIFICATIONS_LATEST_NOTIFIED = "com.battlelancer.seriesguide.notifications.latestnotified";

    public static final String KEY_VIBRATE = "com.battlelancer.seriesguide.notifications.vibrate";

    public static final String KEY_RINGTONE = "com.battlelancer.seriesguide.notifications.ringtone";

    public static final String KEY_LANGUAGE = "language";

    public static final String KEY_SHAREWITHTRAKT = "com.battlelancer.seriesguide.sharewithtrakt";

    public static final String KEY_SHAREWITHGETGLUE = "com.battlelancer.seriesguide.sharewithgetglue";

    public static final String KEY_THEME = "com.battlelancer.seriesguide.theme";

    public static final String KEY_LASTBACKUP = "com.battlelancer.seriesguide.lastbackup";

    public static final String KEY_FAILED_COUNTER = "com.battlelancer.seriesguide.failedcounter";

    public static final String KEY_ACTIVITYTAB = "com.battlelancer.seriesguide.activitytab";

    public static final String KEY_AUTO_ADD_TRAKT_SHOWS = "com.battlelancer.seriesguide.autoaddtraktshows";

    public static final String KEY_SYNC_UNSEEN_EPISODES = "com.battlelancer.seriesguide.syncunseenepisodes";

    public static final String SUPPORT_MAIL = "support@seriesgui.de";

    private static final String TAG = "Settings";

    private static final String KEY_ABOUT = "aboutPref";

    public static final String KEY_SELECTED_PAGE = "com.battlelancer.seriesguide.selectedpage";

    public static final String KEY_TMDB_BASE_URL = "com.battlelancer.seriesguide.tmdb.baseurl";

    public static final String KEY_TAPE_INTERVAL = "com.battlelancer.seriesguide.tapeinterval";

    public static int THEME = R.style.SeriesGuideTheme;

    private static void fireTrackerEvent(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Click", label, (long) 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_PREFS_BASIC)) {
            addPreferencesFromResource(R.xml.settings_basic);
            setupBasicSettings(this, findPreference(KEY_ONLY_FUTURE_EPISODES),
                    findPreference(KEY_ONLY_SEASON_EPISODES),
                    findPreference(KEY_NOTIFICATIONS_ENABLED),
                    findPreference(KEY_NOTIFICATIONS_FAVONLY), findPreference(KEY_VIBRATE),
                    findPreference(KEY_RINGTONE), findPreference(KEY_LANGUAGE),
                    findPreference(KEY_NOTIFICATIONS_THRESHOLD));
        } else if (action != null && action.equals(ACTION_PREFS_SHARING)) {
            addPreferencesFromResource(R.xml.settings_sharing);
            setupSharingSettings(this, findPreference(KEY_GETGLUE_DISCONNECT));
        } else if (action != null && action.equals(ACTION_PREFS_ADVANCED)) {
            addPreferencesFromResource(R.xml.settings_advanced);
            setupAdvancedSettings(this, findPreference(KEY_THEME), getIntent(),
                    findPreference(KEY_UPCOMING_LIMIT), findPreference(KEY_NUMBERFORMAT),
                    findPreference(KEY_OFFSET), findPreference(KEY_GOOGLEANALYTICS),
                    findPreference(KEY_CLEAR_CACHE));
        } else if (action != null && action.equals(ACTION_PREFS_ABOUT)) {
            addPreferencesFromResource(R.xml.settings_about);
            setupAboutSettings(this, findPreference(KEY_ABOUT));
        } else if (!AndroidUtils.isHoneycombOrHigher()) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.settings_legacy);
        }

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    protected static void setupSharingSettings(Context context, Preference getGluePref) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Disconnect GetGlue
        getGluePref.setEnabled(GetGlue.isAuthenticated(prefs));
        getGluePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Disonnect GetGlue");

                GetGlue.clearCredentials(prefs);
                preference.setEnabled(false);
                return true;
            }
        });
    }

    protected static void setupBasicSettings(final Context context, Preference noAiredPref,
            Preference noSpecialsPref, Preference notificationsPref,
            final Preference notificationsFavOnlyPref, final Preference vibratePref,
            final Preference ringtonePref,
            Preference languagePref, final Preference notificationsThresholdPref) {
        // No aired episodes
        noAiredPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    EasyTracker.getTracker().sendEvent(TAG, "OnlyFutureEpisodes", "Enable",
                            (long) 0);
                } else {
                    EasyTracker.getTracker().sendEvent(TAG, "OnlyFutureEpisodes", "Disable",
                            (long) 0);
                }
                return false;
            }
        });

        // No special episodes
        noSpecialsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    EasyTracker.getTracker().sendEvent(TAG, "OnlySeasonEpisodes", "Enable",
                            (long) 0);
                } else {
                    EasyTracker.getTracker().sendEvent(TAG, "OnlySeasonEpisodes", "Disable",
                            (long) 0);
                }
                return false;
            }
        });

        // Notifications
        // allow supporters to enable notfications
        if (Utils.isSupporterChannel(context)) {
            notificationsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean isChecked = ((CheckBoxPreference) preference).isChecked();
                    if (isChecked) {
                        EasyTracker.getTracker().sendEvent(TAG, "Notifications", "Enable",
                                (long) 0);
                    } else {
                        EasyTracker.getTracker().sendEvent(TAG, "Notifications", "Disable",
                                (long) 0);
                    }

                    notificationsThresholdPref.setEnabled(isChecked);
                    notificationsFavOnlyPref.setEnabled(isChecked);
                    vibratePref.setEnabled(isChecked);
                    ringtonePref.setEnabled(isChecked);

                    Utils.runNotificationService(context);
                    return true;
                }
            });
            notificationsFavOnlyPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    resetAndRunNotificationsService(context);
                    return true;
                }
            });
        } else {
            notificationsPref.setEnabled(false);
            notificationsPref.setSummary(R.string.onlyx);
            notificationsThresholdPref.setEnabled(false);
            notificationsFavOnlyPref.setEnabled(false);
            vibratePref.setEnabled(false);
            ringtonePref.setEnabled(false);
        }

        setListPreferenceSummary((ListPreference) languagePref);
        setListPreferenceSummary((ListPreference) notificationsThresholdPref);
    }

    protected static void setupAdvancedSettings(final Activity activity, Preference themePref,
            final Intent startIntent, Preference upcomingPref, Preference numberFormatPref,
            Preference offsetPref, Preference analyticsPref, Preference clearCachePref) {
        // Theme switcher
        themePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference.getKey().equals(KEY_THEME)) {
                    Utils.updateTheme((String) newValue);

                    // restart to apply new theme
                    NavUtils.navigateUpTo(activity,
                            new Intent(Intent.ACTION_MAIN).setClass(activity, ShowsActivity.class));
                    activity.startActivity(startIntent);
                }
                return true;
            }
        });

        // Clear image cache
        clearCachePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Clear Image Cache");

                ImageProvider.getInstance(activity).clearCache();
                ImageProvider.getInstance(activity).clearExternalStorageCache();
                Toast.makeText(activity, activity.getString(R.string.done), Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
        });

        // GA opt-out
        analyticsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference.getKey().equals(KEY_GOOGLEANALYTICS)) {
                    boolean isEnabled = (Boolean) newValue;
                    GoogleAnalytics.getInstance(activity).setAppOptOut(isEnabled);
                    return true;
                }
                return false;
            }
        });

        // show currently set values for list prefs
        setListPreferenceSummary((ListPreference) themePref);
        setListPreferenceSummary((ListPreference) upcomingPref);

        setListPreferenceSummary((ListPreference) numberFormatPref);
        ListPreference offsetListPref = (ListPreference) offsetPref;
        offsetListPref.setSummary(activity.getString(R.string.pref_offsetsummary,
                offsetListPref.getEntry()));
    }

    protected static void setupAboutSettings(Context context, Preference aboutPref) {
        final String versionFinal = Utils.getVersion(context);

        // About
        aboutPref.setSummary("v" + versionFinal + " (Database v"
                + SeriesGuideDatabase.DATABASE_VERSION + ")");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings, target);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // always navigate back to the home activity
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            NavUtils.navigateUpTo(this,
                    new Intent(Intent.ACTION_MAIN).setClass(this, ShowsActivity.class));
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Performs certain actions on settings changes. <br>
     * <b>WARNING This is for older devices. Newer devices should implement
     * actions in {@link SettingsFragment}s implementation if they require
     * findPreference() to return non-null values.</b>
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_UPCOMING_LIMIT) || key.equals(KEY_LANGUAGE)
                || key.equals(KEY_NUMBERFORMAT) || key.equals(KEY_THEME)
                || key.equals(KEY_NOTIFICATIONS_THRESHOLD)) {
            Preference pref = findPreference(key);
            if (pref != null) {
                setListPreferenceSummary((ListPreference) pref);
            }
        }

        /*
         * This can run here, as it does not depend on findPreference() which
         * would return null when using a SettingsFragment.
         */
        if (key.equals(KEY_LANGUAGE)) {
            // reset last edit date of all episodes so they will get updated
            new Thread(new Runnable() {
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(Episodes.LASTEDIT, 0);
                    getContentResolver().update(Episodes.CONTENT_URI, values, null, null);
                }
            }).start();
        }

        if (key.equals(KEY_OFFSET)) {
            Preference pref = findPreference(key);
            if (pref != null) {
                ListPreference listPref = (ListPreference) pref;
                // Set summary to be the user-description for the selected value
                listPref.setSummary(getString(R.string.pref_offsetsummary, listPref.getEntry()));

                resetAndRunNotificationsService(SeriesGuidePreferences.this);
            }
        }

        if (key.equals(KEY_NOTIFICATIONS_THRESHOLD)) {
            Preference pref = findPreference(key);
            if (pref != null) {
                resetAndRunNotificationsService(SeriesGuidePreferences.this);
            }
        }
    }

    /**
     * Resets and runs the notification service to take care of potential time
     * shifts when e.g. changing the time offset.
     */
    private static void resetAndRunNotificationsService(Context context) {
        NotificationService.resetLastEpisodeAirtime(PreferenceManager
                .getDefaultSharedPreferences(context));
        Utils.runNotificationService(context);
    }

    public static void setListPreferenceSummary(ListPreference listPref) {
        // Set summary to be the user-description for the selected value
        listPref.setSummary(listPref.getEntry());
    }

    @TargetApi(11)
    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("settings");
            if ("basic".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_basic);
                setupBasicSettings(getActivity(), findPreference(KEY_ONLY_FUTURE_EPISODES),
                        findPreference(KEY_ONLY_SEASON_EPISODES),
                        findPreference(KEY_NOTIFICATIONS_ENABLED),
                        findPreference(KEY_NOTIFICATIONS_FAVONLY), findPreference(KEY_VIBRATE),
                        findPreference(KEY_RINGTONE), findPreference(KEY_LANGUAGE),
                        findPreference(KEY_NOTIFICATIONS_THRESHOLD));
            } else if ("sharing".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_sharing);
                setupSharingSettings(getActivity(), findPreference(KEY_GETGLUE_DISCONNECT));
            } else if ("advanced".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_advanced);
                setupAdvancedSettings(getActivity(), findPreference(KEY_THEME), getActivity()
                        .getIntent(), findPreference(KEY_UPCOMING_LIMIT),
                        findPreference(KEY_NUMBERFORMAT), findPreference(KEY_OFFSET),
                        findPreference(KEY_GOOGLEANALYTICS), findPreference(KEY_CLEAR_CACHE));
            } else if ("about".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_about);
                setupAboutSettings(getActivity(), findPreference(KEY_ABOUT));
            }
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
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(KEY_UPCOMING_LIMIT) || key.equals(KEY_LANGUAGE)
                    || key.equals(KEY_NUMBERFORMAT) || key.equals(KEY_THEME)
                    || key.equals(KEY_NOTIFICATIONS_THRESHOLD)) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    setListPreferenceSummary((ListPreference) pref);
                }
            }

            if (key.equals(KEY_OFFSET)) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    ListPreference listPref = (ListPreference) pref;
                    // Set summary to be the user-description for the selected
                    // value
                    listPref.setSummary(getString(R.string.pref_offsetsummary, listPref.getEntry()));

                    resetAndRunNotificationsService(getActivity());
                }
            }

            if (key.equals(KEY_NOTIFICATIONS_THRESHOLD)) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    resetAndRunNotificationsService(getActivity());
                }
            }
        }
    }
}
