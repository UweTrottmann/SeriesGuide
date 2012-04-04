
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SeriesGuidePreferences extends SherlockPreferenceActivity {

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

    public static final String KEY_SHOWSSORTORDER = "showSorting";

    public static final String KEY_SHOWFILTER = "com.battlelancer.seriesguide.showfilter";

    public static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";

    public static final String KEY_UPDATEATLEASTEVERY = "com.battlelancer.seriesguide.updateatleastevery";

    public static final String KEY_VERSION = "oldversioncode";

    public static final String KEY_HIDEIMAGES = "hideimages";

    public static final String KEY_GOOGLEANALYTICS = "enableGAnalytics";

    public static final String KEY_AUTOUPDATE = "com.battlelancer.seriesguide.autoupdate";

    public static final String KEY_AUTOUPDATEWLANONLY = "com.battlelancer.seriesguide.autoupdatewlanonly";

    public static final String KEY_LASTUPDATE = "com.battlelancer.seriesguide.lastupdate";

    public static final String KEY_LASTTRAKTUPDATE = "com.battlelancer.seriesguide.lasttraktupdate";

    public static final String KEY_LAST_USED_SHARE_METHOD = "com.battlelancer.seriesguide.lastusedsharemethod";

    public static final String KEY_ONLYFAVORITES = "com.battlelancer.seriesguide.onlyfavorites";

    public static final String KEY_UPCOMING_LIMIT = "com.battlelancer.seriesguide.upcominglimit";

    public static final String KEY_NOTIFICATIONS_ENABLED = "com.battlelancer.seriesguide.notifications";

    public static final String KEY_LANGUAGE = "language";

    public static final String KEY_SHAREWITHTRAKT = "com.battlelancer.seriesguide.sharewithtrakt";

    public static final String KEY_SHAREWITHGETGLUE = "com.battlelancer.seriesguide.sharewithgetglue";

    public static final String SUPPORT_MAIL = "support@seriesgui.de";

    public static final String HELP_URL = "http://seriesgui.de/help";

    protected static final int ABOUT_DIALOG = 0;

    private static final String TRANSLATIONS_URL = "http://crowdin.net/project/seriesguide-translations/invite";

    private static final String PAYPAL_DONATE_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=VVBLMQBSBU74L";

    private static final String TAG = "SeriesGuidePreferences";

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(this).trackEvent(TAG, "Click", label, 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SeriesGuidePreferences activity = this;
        addPreferencesFromResource(R.layout.preferences);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        final String versionFinal = Utils.getVersion(this);

        // About
        Preference aboutPref = (Preference) findPreference("aboutPref");
        aboutPref.setSummary("v" + versionFinal + " (dbver " + SeriesGuideDatabase.DATABASE_VERSION
                + ")");
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("About dialog");

                showDialog(ABOUT_DIALOG);
                return true;
            }
        });

        // Clear image cache
        Preference clearCachePref = (Preference) findPreference("clearCache");
        clearCachePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Clear Image Cache");

                ImageCache.getInstance(activity).clear();
                ImageCache.getInstance(activity).clearExternal();
                Toast.makeText(getApplicationContext(), getString(R.string.done),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // Backup & Restore
        Preference backupPref = (Preference) findPreference("backup");
        backupPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(activity, BackupDeleteActivity.class));
                return true;
            }
        });

        // No aired episodes
        Preference futureepisodes = (Preference) findPreference(KEY_ONLY_FUTURE_EPISODES);
        futureepisodes.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                            "OnlyFutureEpisodes", "Enable", 0);
                } else {
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                            "OnlyFutureEpisodes", "Disable", 0);
                }
                return false;
            }
        });

        // No special episodes
        Preference seasonEpisodes = (Preference) findPreference(KEY_ONLY_SEASON_EPISODES);
        seasonEpisodes.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                            "OnlySeasonEpisodes", "Enable", 0);
                } else {
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                            "OnlySeasonEpisodes", "Disable", 0);
                }
                return false;
            }
        });

        // run notification service to take care of potential time shifts when
        // changing the time offset
        findPreference(KEY_OFFSET).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference.getKey().equals(KEY_OFFSET)) {
                    Utils.runNotificationService(SeriesGuidePreferences.this);
                }
                return true;
            }
        });

        // Disconnect GetGlue
        Preference getgluePref = (Preference) findPreference("clearGetGlueCredentials");
        getgluePref.setEnabled(GetGlue.isAuthenticated(prefs));
        getgluePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Disonnect GetGlue");

                GetGlue.clearCredentials(prefs);
                preference.setEnabled(false);
                return true;
            }
        });

        // trakt.tv
        findPreference("com.battlelancer.seriesguide.trakt").setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(SeriesGuidePreferences.this,
                                TraktSyncActivity.class));
                        return true;
                    }
                });

        // Help translate
        Preference helpTranslate = (Preference) findPreference("com.battlelancer.seriesguide.helpTranslate");
        helpTranslate.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Help translate");

                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TRANSLATIONS_URL));
                startActivity(myIntent);
                return true;
            }
        });

        // Donate
        findPreference("com.battlelancer.seriesguide.donate").setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    public boolean onPreferenceClick(Preference preference) {
                        // track event
                        fireTrackerEvent("Donate");

                        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
                                .parse(PAYPAL_DONATE_URL));
                        startActivity(myIntent);
                        return true;
                    }
                });

        // Notifications
        Preference notificationsPref = findPreference(KEY_NOTIFICATIONS_ENABLED);
        switch (Utils.getChannel(this)) {
            case STABLE: {
                notificationsPref.setEnabled(false);
                notificationsPref.setSummary(R.string.onlyx);
                break;
            }
            default: {
                notificationsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (((CheckBoxPreference) preference).isChecked()) {
                            AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                                    "Notifications", "Enable", 0);
                        } else {
                            AnalyticsUtils.getInstance(activity).trackEvent("Settings",
                                    "Notifications", "Disable", 0);
                        }

                        Utils.runNotificationService(SeriesGuidePreferences.this);
                        return true;
                    }
                });
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.getInstance(this).trackPageView("/Settings");
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ABOUT_DIALOG: {
                final TextView message = new TextView(this);
                message.setAutoLinkMask(Linkify.ALL);
                message.setText(getString(R.string.about_message));
                message.setPadding(10, 5, 10, 5);
                message.setTextSize(16);
                final ScrollView aboutScroll = new ScrollView(this);
                aboutScroll.addView(message);
                return new AlertDialog.Builder(this).setTitle(getString(R.string.about))
                        .setCancelable(true).setIcon(R.drawable.icon)
                        .setPositiveButton(getString(android.R.string.ok), null)
                        .setView(aboutScroll).create();
            }
        }
        return null;
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
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
