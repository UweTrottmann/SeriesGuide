
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.util.ActivityHelper;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.ShareUtils.TraktCredentialsDialogFragment;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.SherlockPreferenceActivity;
import android.support.v4.view.MenuItem;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SeriesGuidePreferences extends SherlockPreferenceActivity {

    public static final String KEY_TRAKTPWD = "com.battlelancer.seriesguide.traktpwd";

    public static final String KEY_TRAKTUSER = "com.battlelancer.seriesguide.traktuser";

    public static final String KEY_USE_MY_TIMEZONE = "com.battlelancer.seriesguide.usemytimezone";

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

    protected static final int ABOUT_DIALOG = 0;

    private static final String TRANSLATIONS_URL = "http://crowdin.net/project/seriesguide-translations/invite";

    private static final String HELP_URL = "http://seriesguide.uwetrottmann.com/help";

    private static final String PAYPAL_DONATE_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=VVBLMQBSBU74L";

    private static final String SUPPORT_MAIL = "seriesguide@battlelancer.com";

    private final ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);

    private static final String TAG = "SeriesGuidePreferences";

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(this).trackEvent(TAG, "Click", label, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SeriesGuidePreferences activity = this;
        addPreferencesFromResource(R.layout.preferences);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_META_DATA).versionName;
        } catch (NameNotFoundException e) {
            version = "UnknownVersion";
        }
        final String versionFinal = version;

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

        // Help
        Preference helpPref = (Preference) findPreference("helpPref");
        helpPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent("Help");

                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_URL));
                startActivity(myIntent);
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

        // Use my timezone
        CheckBoxPreference useMyTimezone = (CheckBoxPreference) findPreference("com.battlelancer.seriesguide.usemytimezone");
        useMyTimezone.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings", "Use my time zone",
                            "Enable", 0);
                } else {
                    AnalyticsUtils.getInstance(activity).trackEvent("Settings", "Use my time zone",
                            "Disable", 0);
                }
                return false;
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

        // Trakt.tv credentials
        Preference traktCred = findPreference("com.battlelancer.seriesguide.traktcredentials");
        traktCred.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                // show the trakt credentials dialog
                TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                        .newInstance();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                newFragment.show(ft, "traktcredentialsdialog");
                return true;
            }
        });

        // Trakt.tv experimental sync
        findPreference("com.battlelancer.seriesguide.traktsync").setOnPreferenceClickListener(
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

        // Sending feedback
        Preference feedback = (Preference) findPreference("com.battlelancer.seriesguide.feedback");
        feedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);

                intent.setType("plain/text");

                intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
                    SUPPORT_MAIL
                });

                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "SeriesGuide " + versionFinal
                        + " Feedback");

                intent.putExtra(android.content.Intent.EXTRA_TEXT, "");

                startActivity(Intent.createChooser(intent, "Send mail..."));
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
        return mActivityHelper.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mActivityHelper.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
