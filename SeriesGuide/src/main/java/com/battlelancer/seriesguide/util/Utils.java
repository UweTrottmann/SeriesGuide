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

package com.battlelancer.seriesguide.util;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.service.OnAlarmReceiver;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.BuildConfig;
import com.uwetrottmann.seriesguide.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    private static final String TAG = "Utils";

    private static final String TIMEZONE_AMERICA_PREFIX = "America/";

    private static final String TIMEZONE_ALWAYS_PST = "GMT-08:00";

    private static final String TIMEZONE_US_ARIZONA = "America/Phoenix";

    private static final String TIMEZONE_US_EASTERN = "America/New_York";

    private static final Object TIMEZONE_US_EASTERN_DETROIT = "America/Detroit";

    private static final String TIMEZONE_US_CENTRAL = "America/Chicago";

    private static final String TIMEZONE_US_PACIFIC = "America/Los_Angeles";

    private static final String TIMEZONE_US_MOUNTAIN = "America/Denver";

    /**
     * Return date string of the given time, prefixed with the actual day of the week (e.g. 'Mon, ')
     * or 'today, ' if applicable.
     */
    public static String formatToDate(long airtime, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Calendar cal = getAirtimeCalendar(airtime, prefs);

        TimeZone localTimeZone = cal.getTimeZone();
        Date date = cal.getTime();
        String timezone = localTimeZone.getDisplayName(localTimeZone.inDaylightTime(date),
                TimeZone.SHORT);

        String dateString = DateFormat.getDateFormat(context).format(date) + " " + timezone;
        // add today prefix if applicable
        if (DateUtils.isToday(cal.getTimeInMillis())) {
            dateString = context.getString(R.string.today) + ", " + dateString;
        } else {
            final SimpleDateFormat dayFormat = new SimpleDateFormat("E");
            dateString = dayFormat.format(date) + ", " + dateString;
        }

        return dateString;
    }

    /**
     * Create a calendar set to the given airtime, time is adjusted according to 'Use my time zone',
     * 'Time Offset' settings and user time zone.
     */
    public static Calendar getAirtimeCalendar(long airtime, final SharedPreferences prefs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(airtime);

        setOffsets(prefs, cal, airtime);

        return cal;
    }

    /**
     * Add user set manual offset and auto-offset for US time zones.
     */
    private static void setOffsets(SharedPreferences prefs, Calendar cal, long airtime) {
        boolean pacificInDaylight = TimeZone.getTimeZone(TIMEZONE_US_PACIFIC).inDaylightTime(
                new Date(airtime));

        // get user-set hour offset
        int offset = Integer.valueOf(prefs.getString(SeriesGuidePreferences.KEY_OFFSET, "0"));
        final String tzId = TimeZone.getDefault().getID();

        if (tzId.startsWith(TIMEZONE_AMERICA_PREFIX, 0)) {
            if (tzId.equals(TIMEZONE_US_MOUNTAIN)) {
                offset -= 1;
            } else if (tzId.equals(TIMEZONE_US_CENTRAL)) {
                // for US Central subtract one hour more
                // shows always air an hour earlier
                offset -= 3;
            } else if (tzId.equals(TIMEZONE_US_EASTERN) || tzId
                    .equals(TIMEZONE_US_EASTERN_DETROIT)) {
                offset -= 3;
            } else if (tzId.equals(TIMEZONE_US_ARIZONA)) {
                // Arizona has no daylight saving, correct for that
                // airtime might not be correct, yet, but the best we can do for
                // now
                if (!pacificInDaylight) {
                    offset -= 1;
                }
            }
        }

        // we store all time values in GMT+08:00 (always Pacific Standard Time)
        // correct that if Pacific is in daylight savings
        if (pacificInDaylight) {
            offset -= 1;
        }

        if (offset != 0) {
            cal.add(Calendar.HOUR_OF_DAY, offset);
        }
    }

    public static long buildEpisodeAirtime(String tvdbDateString, long airtime) {
        TimeZone pacific = TimeZone.getTimeZone(TIMEZONE_ALWAYS_PST);
        SimpleDateFormat tvdbDateFormat = Constants.theTVDBDateFormat;
        tvdbDateFormat.setTimeZone(pacific);

        try {

            Date day = tvdbDateFormat.parse(tvdbDateString);

            Calendar dayCal = Calendar.getInstance(pacific);
            dayCal.setTime(day);

            // set an airtime if we have one (may not be the case for ended
            // shows)
            if (airtime != -1) {
                Calendar timeCal = Calendar.getInstance(pacific);
                timeCal.setTimeInMillis(airtime);

                dayCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                dayCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                dayCal.set(Calendar.SECOND, 0);
                dayCal.set(Calendar.MILLISECOND, 0);
            }

            return dayCal.getTimeInMillis();

        } catch (ParseException e) {
            // we just return -1 then
            return -1;
        }
    }

    /**
     * Returns a string in format "1x01 title" or "S1E01 title" dependent on a user preference.
     */
    public static String getNextEpisodeString(Context context, int season, int episode,
            String title) {
        String result = getEpisodeNumber(context, season, episode);
        result += " " + title;
        return result;
    }

    /**
     * Returns the episode number formatted according to the users preference (e.g. '1x01',
     * 'S01E01', ...).
     */
    public static String getEpisodeNumber(Context context, int season, int episode) {
        String format = DisplaySettings.getNumberFormat(context);
        String result = String.valueOf(season);
        if (DisplaySettings.NUMBERFORMAT_DEFAULT.equals(format)) {
            // 1x01 format
            result += "x";
        } else {
            // S01E01 format
            // make season number always two chars long
            if (season < 10) {
                result = "0" + result;
            }
            if (DisplaySettings.NUMBERFORMAT_ENGLISHLOWER.equals(format)) {
                result = "s" + result + "e";
            } else {
                result = "S" + result + "E";
            }
        }

        if (episode != -1) {
            // make episode number always two chars long
            if (episode < 10) {
                result += "0";
            }

            result += episode;
        }
        return result;
    }

    /**
     * Splits the string and reassembles it, separating the items with commas. The given object is
     * returned with the new string.
     */
    public static String splitAndKitTVDBStrings(String tvdbstring) {
        if (tvdbstring == null) {
            tvdbstring = "";
        }
        String[] splitted = tvdbstring.split("\\|");
        tvdbstring = "";
        for (String item : splitted) {
            if (tvdbstring.length() != 0) {
                tvdbstring += ", ";
            }
            tvdbstring += item;
        }
        return tvdbstring;
    }

    /**
     * Update the latest episode fields for all existing shows.
     */
    public static void updateLatestEpisodes(Context context) {
        Thread t = new UpdateLatestEpisodeThread(context);
        t.start();
    }

    /**
     * Update the latest episode field for a specific show.
     */
    public static void updateLatestEpisode(Context context, int showTvdbId) {
        Thread t = new UpdateLatestEpisodeThread(context, showTvdbId);
        t.start();
    }

    public static class UpdateLatestEpisodeThread extends Thread {

        private Context mContext;

        private int mShowTvdbId;

        public UpdateLatestEpisodeThread(Context context) {
            mContext = context;
            this.setName("UpdateLatestEpisode");
        }

        public UpdateLatestEpisodeThread(Context context, int showTvdbId) {
            this(context);
            mShowTvdbId = showTvdbId;
        }

        public void run() {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final boolean isNoReleasedEpisodes = DisplaySettings.isNoReleasedEpisodes(mContext);
            final boolean isNoSpecials = DisplaySettings.isHidingSpecials(mContext);

            if (mShowTvdbId > 0) {
                // update single show
                DBUtils.updateLatestEpisode(mContext, mShowTvdbId, isNoReleasedEpisodes,
                        isNoSpecials);
            } else {
                // update all shows
                final Cursor shows = mContext.getContentResolver().query(Shows.CONTENT_URI,
                        new String[]{
                                Shows._ID
                        }, null, null, null);
                if (shows != null) {
                    while (shows.moveToNext()) {
                        int showTvdbId = shows.getInt(0);
                        DBUtils.updateLatestEpisode(mContext, showTvdbId, isNoReleasedEpisodes,
                                isNoSpecials);
                    }
                    shows.close();
                }
            }

            // Show adapter gets notified by ContentProvider
            // Lists adapter needs to be notified manually
            mContext.getContentResolver().notifyChange(ListItems.CONTENT_WITH_DETAILS_URI, null);
        }
    }

    public static String getVersion(Context context) {
        String version;
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA).versionName;
        } catch (NameNotFoundException e) {
            version = "UnknownVersion";
        }
        return version;
    }

    /**
     * Run the notification service to display and (re)schedule upcoming episode alarms.
     */
    public static void runNotificationService(Context context) {
        Intent i = new Intent(context, NotificationService.class);
        context.startService(i);
    }

    /**
     * Run the notification service delayed by a minute to display and (re)schedule upcoming episode
     * alarms.
     */
    public static void runNotificationServiceDelayed(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1
                * DateUtils.MINUTE_IN_MILLIS, pi);
    }

    public static String toSHA1(Context context, String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageBytes = message.getBytes("UTF-8");
            byte[] digest = md.digest(messageBytes);

            String result = "";
            for (int i = 0; i < digest.length; i++) {
                result += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
            }

            return result;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Utils.trackExceptionAndLog(context, TAG, e);
        }
        return null;
    }

    /**
     * Returns whether a regular check with the Google Play app is necessary to determine access to
     * X features (e.g. the subscription is still valid).
     */
    public static boolean requiresPurchaseCheck(Context context) {
        // dev builds and the SeriesGuide X key app are not handled through the
        // Play store
        return !(BuildConfig.DEBUG || hasUnlockKeyInstalled(context));
    }

    /**
     * Returns whether this user should currently get access to X features.
     */
    public static boolean hasAccessToX(Context context) {
        // dev builds, SeriesGuide X installed or a valid purchase unlock X
        // features
        return !requiresPurchaseCheck(context) || AdvancedSettings.isSubscribedToX(context);
    }

    /**
     * Returns true if the user has the legacy SeriesGuide X version installed, signed with the same
     * key as we are.
     */
    public static boolean hasUnlockKeyInstalled(Context context) {
        try {
            // Get our signing key
            PackageManager manager = context.getPackageManager();
            PackageInfo appInfoSeriesGuide = manager
                    .getPackageInfo(
                            context.getApplicationContext().getPackageName(),
                            PackageManager.GET_SIGNATURES);

            // Try to find the X signing key
            PackageInfo appInfoSeriesGuideX = manager
                    .getPackageInfo(
                            "com.battlelancer.seriesguide.x",
                            PackageManager.GET_SIGNATURES);

            final String ourKey = appInfoSeriesGuide.signatures[0].toCharsString();
            final String xKey = appInfoSeriesGuideX.signatures[0].toCharsString();
            return ourKey.equals(xKey);
        } catch (NameNotFoundException e) {
            // Expected exception that occurs if the package is not present.
        }

        return false;
    }

    public static void setValueOrPlaceholder(View view, final String value) {
        TextView field = (TextView) view;
        if (value == null || value.length() == 0) {
            field.setText(R.string.unknown);
        } else {
            field.setText(value);
        }
    }

    public static void setLabelValueOrHide(View label, TextView text, final String value) {
        if (TextUtils.isEmpty(value)) {
            label.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
        } else {
            label.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
            text.setText(value);
        }
    }

    public static void setLabelValueOrHide(View label, TextView text, double value) {
        if (value > 0.0) {
            label.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
            text.setText(String.valueOf(value));
        } else {
            label.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
        }
    }

    @TargetApi(16)
    @SuppressWarnings("deprecation")
    public static void setPosterBackground(ImageView background, String posterPath,
            Context context) {
        if (AndroidUtils.isJellyBeanOrHigher()) {
            background.setImageAlpha(30);
        } else {
            background.setAlpha(30);
        }
        ImageProvider.getInstance(context).loadImage(background, posterPath, false);
    }

    /**
     * Sets the global app theme variable. Applied by all activities once they are created.
     */
    public static synchronized void updateTheme(String themeIndex) {
        int theme = Integer.valueOf(themeIndex);
        switch (theme) {
            case 1:
                SeriesGuidePreferences.THEME = R.style.AndroidTheme;
                break;
            case 2:
                SeriesGuidePreferences.THEME = R.style.SeriesGuideThemeLight;
                break;
            default:
                SeriesGuidePreferences.THEME = R.style.SeriesGuideTheme;
                break;
        }
    }

    /**
     * Tracks an exception using the Google Analytics {@link EasyTracker}.
     */
    public static void trackException(Context context, String tag, Exception e) {
        EasyTracker.getInstance(context).send(
                MapBuilder.createException(tag + ": " + e.getMessage(), false).build()
        );
    }

    /**
     * Tracks an exception using the Google Analytics {@link EasyTracker} and the local log.
     */
    public static void trackExceptionAndLog(Context context, String tag, Exception e) {
        trackException(context, tag, e);
        Log.w(tag, e);
    }

    public static void trackView(Context context, String screenName) {
        EasyTracker tracker = EasyTracker.getInstance(context);
        tracker.set(Fields.SCREEN_NAME, screenName);
        tracker.send(MapBuilder.createAppView().build());
        tracker.set(Fields.SCREEN_NAME, null);
    }

    public static void trackCustomEvent(Context context, String tag, String category,
            String label) {
        EasyTracker.getInstance(context).send(
                MapBuilder.createEvent(tag, category, label, null).build()
        );
    }

    public static void trackAction(Context context, String tag, String label) {
        EasyTracker.getInstance(context).send(
                MapBuilder.createEvent(tag, "Action Item", label, null).build()
        );
    }

    public static void trackContextMenu(Context context, String tag, String label) {
        EasyTracker.getInstance(context).send(
                MapBuilder.createEvent(tag, "Context Item", label, null).build()
        );
    }

    public static void trackClick(Context context, String tag, String label) {
        EasyTracker.getInstance(context).send(
                MapBuilder.createEvent(tag, "Click", label, null).build()
        );
    }

    /**
     * Returns true if there is an active connection which is approved by the user for large data
     * downloads (e.g. images).
     *
     * @param showOfflineToast If true, displays a toast asking the user to connect to a network.
     */
    public static boolean isAllowedLargeDataConnection(Context context, boolean showOfflineToast) {
        boolean isConnected;
        boolean largeDataOverWifiOnly = UpdateSettings.isLargeDataOverWifiOnly(context);

        // check connection state
        if (largeDataOverWifiOnly) {
            isConnected = AndroidUtils.isWifiConnected(context);
        } else {
            isConnected = AndroidUtils.isNetworkConnected(context);
        }

        // display optional offline toast
        if (showOfflineToast && !isConnected) {
            Toast.makeText(context,
                    largeDataOverWifiOnly ? R.string.offline_no_wifi : R.string.offline,
                    Toast.LENGTH_LONG).show();
        }

        return isConnected;
    }

    /**
     * Returns true if a network connection exists.
     *
     * @param showOfflineToast If true, displays a toast asking the user to connect to a network.
     */
    public static boolean isConnected(Context context, boolean showOfflineToast) {
        boolean isConnected = AndroidUtils.isNetworkConnected(context);

        // display optional offline toast
        if (!isConnected && showOfflineToast) {
            Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show();
        }

        return isConnected;
    }

    /**
     * Launches {@link BillingActivity} and notifies that something is only available with the X
     * subscription.
     */
    public static void advertiseSubscription(Context context) {
        Toast.makeText(context, R.string.onlyx, Toast.LENGTH_SHORT).show();
        context.startActivity(new Intent(context, BillingActivity.class));
    }

    /**
     * Calls {@link Context#startActivity(Intent)} with the given <b>implicit</b> {@link Intent}
     * after making sure there is an {@link Activity} to handle it. Can show an error toast, if not.
     * <br> <br> This may happen if e.g. the web browser has been disabled through restricted
     * profiles.
     *
     * @return Whether there was an {@link Activity} to handle the given {@link Intent}.
     */
    public static boolean tryStartActivity(Context context, Intent intent, boolean displayError) {
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else if (displayError) {
            Toast.makeText(context, R.string.app_not_available, Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Resolves the given attribute to the resource id for the given theme.
     */
    public static int resolveAttributeToResourceId(Resources.Theme theme, int attributeResId) {
        TypedValue outValue = new TypedValue();
        theme.resolveAttribute(attributeResId, outValue, true);
        return outValue.resourceId;
    }

    /**
     * Tries to launch a web browser loading the given URL. Sets a flag to exit the browser if
     * coming back to the app.
     */
    public static void launchWebsite(Context context, String url, String logTag, String logItem) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // try to launch web browser
        Utils.tryStartActivity(context, intent, true);

        Utils.trackAction(context, logTag, logItem);
    }

}
