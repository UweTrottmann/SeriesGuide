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

package com.battlelancer.seriesguide.util;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.Constants.EpisodeSorting;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.service.OnAlarmReceiver;
import com.battlelancer.seriesguide.settings.ActivitySettings;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    private static final String TIMEZONE_AMERICA_PREFIX = "America/";

    private static final String TAG = "Utils";

    private static final String TIMEZONE_ALWAYS_PST = "GMT-08:00";

    private static final String TIMEZONE_US_ARIZONA = "America/Phoenix";

    private static final String TIMEZONE_US_EASTERN = "America/New_York";

    private static final Object TIMEZONE_US_EASTERN_DETROIT = "America/Detroit";

    private static final String TIMEZONE_US_CENTRAL = "America/Chicago";

    private static final String TIMEZONE_US_PACIFIC = "America/Los_Angeles";

    private static final String TIMEZONE_US_MOUNTAIN = "America/Denver";

    public static final SimpleDateFormat thetvdbTimeFormatAMPM = new SimpleDateFormat("h:mm aa",
            Locale.US);

    public static final SimpleDateFormat thetvdbTimeFormatAMPMalt = new SimpleDateFormat("h:mmaa",
            Locale.US);

    public static final SimpleDateFormat thetvdbTimeFormatAMPMshort = new SimpleDateFormat("h aa",
            Locale.US);

    public static final SimpleDateFormat thetvdbTimeFormatNormal = new SimpleDateFormat("H:mm",
            Locale.US);

    /**
     * Parse a shows TVDb air time value to a ms value in Pacific Standard Time
     * (always without daylight saving).
     * 
     * @param tvdbTimeString
     * @return
     */
    public static long parseTimeToMilliseconds(String tvdbTimeString) {
        Date time = null;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ALWAYS_PST));

        // try parsing with three different formats, most of the time the first
        // should match
        if (tvdbTimeString.length() != 0) {
            try {
                time = thetvdbTimeFormatAMPM.parse(tvdbTimeString);
            } catch (ParseException e) {
                try {
                    time = thetvdbTimeFormatAMPMalt.parse(tvdbTimeString);
                } catch (ParseException e1) {
                    try {
                        time = thetvdbTimeFormatAMPMshort.parse(tvdbTimeString);
                    } catch (ParseException e2) {
                        try {
                            time = thetvdbTimeFormatNormal.parse(tvdbTimeString);
                        } catch (ParseException e3) {
                            // string may be wrongly formatted
                            time = null;
                        }
                    }
                }
            }
        }

        if (time != null) {
            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(time);
            cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } else {
            return -1;
        }
    }

    /**
     * Parse a shows airtime ms value to an actual time. If given a TVDb day
     * string the day will get determined, too, all respecting user settings
     * like time zone and time offset.
     * 
     * @param milliseconds
     * @param dayofweek
     * @param context
     * @return
     */
    public static String[] parseMillisecondsToTime(long milliseconds, String dayofweek,
            Context context) {
        // return empty strings if time is missing
        if (context == null || milliseconds == -1) {
            return new String[] {
                    "", ""
            };
        }

        // set calendar time and day on always PST calendar
        // this is a workaround so we can convert the air day to a another time
        // zone without actually having a date
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ALWAYS_PST));
        final int year = cal.get(Calendar.YEAR);
        final int month = cal.get(Calendar.MONTH);
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTimeInMillis(milliseconds);
        // set the date back to today
        cal.set(year, month, day);

        // determine the shows common air day (Mo through Sun or daily)
        int dayIndex = -1;
        if (dayofweek != null) {
            dayIndex = getDayOfWeek(dayofweek);
            if (dayIndex > 0) {
                int today = cal.get(Calendar.DAY_OF_WEEK);
                // make sure we always assume a day which is today or later
                if (dayIndex - today < 0) {
                    // we have a day before today
                    cal.add(Calendar.DAY_OF_WEEK, (7 - today) + dayIndex);
                } else {
                    // we have today or in the future
                    cal.set(Calendar.DAY_OF_WEEK, dayIndex);
                }
            }
        }

        // convert time to local, including the day
        final Calendar localCal = Calendar.getInstance();
        localCal.setTimeInMillis(cal.getTimeInMillis());

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        setOffsets(prefs, localCal, milliseconds);

        // create time string
        final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
        final SimpleDateFormat dayFormat = new SimpleDateFormat("E");
        final Date date = localCal.getTime();

        timeFormat.setTimeZone(TimeZone.getDefault());
        dayFormat.setTimeZone(TimeZone.getDefault());

        String daystring = "";
        if (dayIndex == 0) {
            daystring = context.getString(R.string.daily);
        } else if (dayIndex != -1) {
            daystring = dayFormat.format(date);
        }

        return new String[] {
                timeFormat.format(date), daystring
        };
    }

    /**
     * Returns the Calendar constant (e.g. <code>Calendar.SUNDAY</code>) for a
     * given TVDb airday string (Monday through Sunday and Daily). If no match
     * is found -1 will be returned.
     * 
     * @param TVDb day string
     * @return
     */
    private static int getDayOfWeek(String day) {
        // catch Daily
        if (day.equalsIgnoreCase("Daily")) {
            return 0;
        }

        // catch Monday through Sunday
        DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
        String[] weekdays = dfs.getWeekdays();

        for (int i = 1; i < weekdays.length; i++) {
            if (day.equalsIgnoreCase(weekdays[i])) {
                return i;
            }
        }

        // no match
        return -1;
    }

    /**
     * Returns an array with absolute time [0], day [1] and relative time [2] of
     * the given millisecond time. Respects user offsets and 'Use my time zone'
     * setting.
     */
    public static String[] formatToTimeAndDay(long airtime, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Calendar cal = getAirtimeCalendar(airtime, prefs);
        Date airDate = cal.getTime();

        // absolute time
        final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
        String absoluteTime = timeFormat.format(airDate);

        // day string
        final SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
        String day = dayFormat.format(airDate);

        // relative time
        String relativeTime = DateUtils
                .getRelativeTimeSpanString(cal.getTimeInMillis(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();

        return new String[] {
                absoluteTime, day, relativeTime
        };
    }

    /**
     * Returns a string like 'Mon in 3 days', the day followed by how far it is
     * away in relative time.<br>
     * Does <b>not</b> respect user offsets or 'Use my time zone' setting. The
     * time to be passed is expected to be already corrected for that.
     */
    public static String formatToDayAndTimeWithoutOffsets(Context context, long airtime) {
        StringBuilder timeAndDay = new StringBuilder();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(airtime);

        final SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
        timeAndDay.append(dayFormat.format(cal.getTime()));

        timeAndDay.append(" ");

        // Show 'today' instead of '0 days ago'
        if (DateUtils.isToday(cal.getTimeInMillis())) {
            timeAndDay.append(context.getString(R.string.today));
        } else {
            timeAndDay.append(DateUtils
                    .getRelativeTimeSpanString(
                            cal.getTimeInMillis(),
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL));
        }

        return timeAndDay.toString();
    }

    /**
     * Return date string of the given time, prefixed with the actual day of the
     * week (e.g. 'Mon, ') or 'today, ' if applicable.
     * 
     * @param airtime
     * @param context
     * @return
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
     * Create a calendar set to the given airtime, time is adjusted according to
     * 'Use my time zone', 'Time Offset' settings and user time zone.
     * 
     * @param airtime
     * @param prefs
     * @return
     */
    public static Calendar getAirtimeCalendar(long airtime, final SharedPreferences prefs) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(airtime);

        setOffsets(prefs, cal, airtime);

        return cal;
    }

    /**
     * Add user set manual offset and auto-offset for US time zones.
     * 
     * @param prefs
     * @param cal
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
            } else if (tzId.equals(TIMEZONE_US_EASTERN) || tzId.equals(TIMEZONE_US_EASTERN_DETROIT)) {
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

    /**
     * To correctly display and calculate upcoming episodes we need to modify
     * the current time to be later/earlier. Also respecting user-set offsets.
     * 
     * @param prefs
     * @return
     */
    public static long getFakeCurrentTime(SharedPreferences prefs) {
        return convertToFakeTime(System.currentTimeMillis(), prefs, true);
    }

    /**
     * Modify a time to be earlier/later respecting user-set offsets and
     * automatic offsets based on time zone.
     * 
     * @param prefs
     * @param isCurrentTime
     * @return
     */
    public static long convertToFakeTime(long time, SharedPreferences prefs, boolean isCurrentTime) {
        boolean pacificInDaylight = TimeZone.getTimeZone(TIMEZONE_US_PACIFIC).inDaylightTime(
                new Date(time));

        int offset = Integer.valueOf(prefs.getString(SeriesGuidePreferences.KEY_OFFSET, "0"));
        final String tzId = TimeZone.getDefault().getID();

        if (tzId.startsWith(TIMEZONE_AMERICA_PREFIX, 0)) {
            if (tzId.equals(TIMEZONE_US_MOUNTAIN)) {
                // Mountain Time
                offset -= 1;
            } else if (tzId.equals(TIMEZONE_US_CENTRAL)) {
                // for US Central subtract one hour more
                // shows always air an hour earlier
                offset -= 3;
            } else if (tzId.equals(TIMEZONE_US_EASTERN) || tzId.equals(TIMEZONE_US_EASTERN_DETROIT)) {
                // Eastern Time
                offset -= 3;
            } else if (tzId.equals(TIMEZONE_US_ARIZONA)) {
                // Arizona has no daylight saving, correct for that
                if (!pacificInDaylight) {
                    offset -= 1;
                }
            }
        }

        if (pacificInDaylight) {
            offset -= 1;
        }

        if (offset != 0) {
            if (isCurrentTime) {
                // invert offset if we modify the current time
                time -= (offset * DateUtils.HOUR_IN_MILLIS);
            } else {
                // add offset if we modify an episodes air time
                time += (offset * DateUtils.HOUR_IN_MILLIS);
            }
        }

        return time;
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

            long episodeAirtime = dayCal.getTimeInMillis();
            return episodeAirtime;

        } catch (ParseException e) {
            // we just return -1 then
            return -1;
        }
    }

    /**
     * Returns a string in format "1x01 title" or "S1E01 title" dependent on a
     * user preference.
     */
    public static String getNextEpisodeString(SharedPreferences prefs, int season, int episode,
            String title) {
        String result = getEpisodeNumber(prefs, season, episode);
        result += " " + title;
        return result;
    }

    /**
     * Returns the episode number formatted according to the users preference
     * (e.g. '1x01', 'S01E01', ...).
     */
    public static String getEpisodeNumber(SharedPreferences prefs, int season,
            int episode) {
        String format = prefs.getString(SeriesGuidePreferences.KEY_NUMBERFORMAT,
                SeriesGuidePreferences.NUMBERFORMAT_DEFAULT);
        String result = String.valueOf(season);
        if (format.equals(SeriesGuidePreferences.NUMBERFORMAT_DEFAULT)) {
            // 1x01 format
            result += "x";
        } else {
            // S01E01 format
            // make season number always two chars long
            if (season < 10) {
                result = "0" + result;
            }
            if (format.equals(SeriesGuidePreferences.NUMBERFORMAT_ENGLISHLOWER)) {
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
     * Splits the string and reassembles it, separating the items with commas.
     * The given object is returned with the new string.
     * 
     * @param tvdbstring
     * @return
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
     * Get the currently set episode sorting from settings.
     * 
     * @param context
     * @return a EpisodeSorting enum set to the current sorting
     */
    public static EpisodeSorting getEpisodeSorting(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentPref = prefs.getString(SeriesGuidePreferences.KEY_EPISODE_SORT_ORDER,
                EpisodeSorting.OLDEST_FIRST.value());

        EpisodeSorting sorting = EpisodeSorting.fromValue(currentPref);
        if (sorting == null) {
            return EpisodeSorting.OLDEST_FIRST;
        } else {
            return EpisodeSorting.fromValue(currentPref);
        }
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
    public static void updateLatestEpisode(Context context, String showId) {
        Thread t = new UpdateLatestEpisodeThread(context, showId);
        t.start();
    }

    public static class UpdateLatestEpisodeThread extends Thread {
        private Context mContext;

        private String mShowId;

        public UpdateLatestEpisodeThread(Context context) {
            mContext = context;
            this.setName("UpdateLatestEpisode");
        }

        public UpdateLatestEpisodeThread(Context context, String showId) {
            this(context);
            mShowId = showId;
        }

        public void run() {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final boolean isOnlyFutureEpisodes = prefs.getBoolean(
                    SeriesGuidePreferences.KEY_ONLY_FUTURE_EPISODES, false);
            final boolean isNoSpecials = ActivitySettings.isHidingSpecials(mContext);

            if (mShowId != null) {
                // update single show
                DBUtils.updateLatestEpisode(mContext, mShowId, isOnlyFutureEpisodes, isNoSpecials,
                        prefs);
            } else {
                // update all shows
                final Cursor shows = mContext.getContentResolver().query(Shows.CONTENT_URI,
                        new String[] {
                            Shows._ID
                        }, null, null, null);
                if (shows != null) {
                    while (shows.moveToNext()) {
                        String showId = shows.getString(0);
                        DBUtils.updateLatestEpisode(mContext, showId, isOnlyFutureEpisodes,
                                isNoSpecials, prefs);
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
     * Put the TVDb season number in, get a full 'Season X' or 'Special
     * Episodes' string out.
     * 
     * @param context
     * @param seasonNumber
     * @return
     */
    public static String getSeasonString(Context context, int seasonNumber) {
        if (seasonNumber == 0) {
            return context.getString(R.string.specialseason);
        } else {
            return context.getString(R.string.season_number, seasonNumber);
        }
    }

    /**
     * Run the notification service to display and (re)schedule upcoming episode
     * alarms.
     */
    public static void runNotificationService(Context context) {
        Intent i = new Intent(context, NotificationService.class);
        context.startService(i);
    }

    /**
     * Run the notification service delayed by a minute to display and
     * (re)schedule upcoming episode alarms.
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
        } catch (NoSuchAlgorithmException e) {
            Utils.trackExceptionAndLog(context, TAG, e);
        } catch (UnsupportedEncodingException e) {
            Utils.trackExceptionAndLog(context, TAG, e);
        }
        return null;
    }

    public enum SGChannel {
        STABLE("com.battlelancer.seriesguide"), BETA("com.battlelancer.seriesguide.beta"), X(
                "com.battlelancer.seriesguide.x");

        String packageName;

        private SGChannel(String packageName) {
            this.packageName = packageName;
        }
    }

    public static SGChannel getChannel(Context context) {
        String thisPackageName = context.getApplicationContext().getPackageName();
        if (thisPackageName.equals(SGChannel.BETA.packageName)) {
            return SGChannel.BETA;
        }
        if (thisPackageName.equals(SGChannel.X.packageName)) {
            return SGChannel.X;
        }
        return SGChannel.STABLE;
    }

    /**
     * Returns whether a regular check with the Google Play app is necessary to
     * determine access to X features (e.g. the subscription is still valid).
     */
    public static boolean requiresPurchaseCheck(Context context) {
        // dev builds and the SeriesGuide X key app are not handled through the
        // Play store
        if (getChannel(context) != SGChannel.STABLE || hasUnlockKeyInstalled(context)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns whether this user should currently get access to X features.
     */
    public static boolean hasAccessToX(Context context) {
        // dev builds, SeriesGuide X installed or a valid purchase unlock X
        // features
        if (!requiresPurchaseCheck(context) || AdvancedSettings.isSubscribedToX(context)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the user has the legacy SeriesGuide X version installed,
     * signed with the same key as we are.
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
    public static void setPosterBackground(ImageView background, String posterPath, Context context) {
        if (AndroidUtils.isJellyBeanOrHigher()) {
            background.setImageAlpha(30);
        } else {
            background.setAlpha(30);
        }
        ImageProvider.getInstance(context).loadImage(background, posterPath, false);
    }

    /**
     * Sets the global app theme variable. Applied by all activities once they
     * are created.
     * 
     * @param themeIndex
     */
    public static synchronized void updateTheme(String themeIndex) {
        int theme = Integer.valueOf((String) themeIndex);
        switch (theme) {
            case 1:
                SeriesGuidePreferences.THEME = R.style.ICSBaseTheme;
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
        EasyTracker.getTracker().sendException(tag + ": " + e.getMessage(), false);
    }

    /**
     * Tracks an exception using the Google Analytics {@link EasyTracker} and
     * the local log.
     */
    public static void trackExceptionAndLog(Context context, String tag, Exception e) {
        trackException(context, tag, e);
        Log.w(tag, e);
    }

    /**
     * Returns true if we are on a user-permitted and connected internet
     * connection.
     * 
     * @param context
     * @return
     */
    public static boolean isAllowedConnection(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean isWlanOnly = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLYWIFI, false);

        if (isWlanOnly) {
            return AndroidUtils.isWifiConnected(context);
        } else {
            return AndroidUtils.isNetworkConnected(context);
        }
    }

    /**
     * Creates the tag of a {@link ViewPager} fragment.
     * 
     * @param viewId of the {@link ViewPager}
     * @param id of the fragment, often the position
     */
    public static String makeViewPagerFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }

    /**
     * Launches {@link BillingActivity} and notifies that something is only
     * available with the X subscription.
     */
    public static void advertiseSubscription(Context context) {
        Toast.makeText(context, R.string.onlyx, Toast.LENGTH_SHORT).show();
        TaskStackBuilder
                .create(context)
                .addNextIntent(new Intent(context, SeriesGuidePreferences.class))
                .addNextIntent(new Intent(context, BillingActivity.class))
                .startActivities();
    }

}
