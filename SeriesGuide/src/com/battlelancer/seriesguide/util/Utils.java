
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    /**
     * Returns the Calendar constant (e.g. <code>Calendar.SUNDAY</code>) for a
     * given weekday string of the US locale. If no match is found
     * <code>Calendar.SUNDAY</code> will be returned.
     * 
     * @param day
     * @return
     */
    private static int getDayOfWeek(String day) {
        DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
        String[] weekdays = dfs.getWeekdays();

        for (int i = 1; i < weekdays.length; i++) {
            if (day.equalsIgnoreCase(weekdays[i])) {
                return i;
            }
        }
        return Calendar.SUNDAY;
    }

    public static String getDayShortcode(String day) {
        return getDayShortcode(getDayOfWeek(day));
    }

    /**
     * Returns the short version of the date symbol for the given weekday (e.g.
     * <code>Calendar.SUNDAY</code>) in the user default locale.
     * 
     * @param day
     * @return
     */
    public static String getDayShortcode(int day) {
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] weekdays = dfs.getShortWeekdays();
        return weekdays[day];
    }

    /**
     * Parses a TVDB date string and airtime in milliseconds to a relative
     * timestring and day shortcode. An array is returned which holds the
     * timestring and the day shortcode (in this order).
     * 
     * @param tvdbDateString
     * @param airtime
     * @param ctx
     * @return String array holding timestring and shortcode.
     */
    public static String parseDateToLocalRelative(String tvdbDateString, long airtime, Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean useUserTimeZone = prefs.getBoolean(SeriesGuidePreferences.KEY_USE_MY_TIMEZONE,
                false);
        try {
            Calendar cal = getLocalCalendar(tvdbDateString, airtime, useUserTimeZone, ctx);
            Calendar now = Calendar.getInstance();
            String day = "";

            // Use midnight to compare if not airing today
            if (!DateUtils.isToday(cal.getTimeInMillis())) {
                int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
                DateFormatSymbols dfs = new DateFormatSymbols();
                day = " (" + dfs.getShortWeekdays()[dayofweek] + ")";

                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                now.set(Calendar.HOUR_OF_DAY, 0);
                now.set(Calendar.MINUTE, 0);
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 0);
            }

            return DateUtils.getRelativeTimeSpanString(cal.getTimeInMillis(),
                    now.getTimeInMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)
                    .toString()
                    + day;
        } catch (ParseException e) {
            // shouldn't happen, if so there are errors in theTVDB
            e.printStackTrace();
        }
        return "";
    }

    public static String parseDateToLocal(String tvdbDateString, long airtime, Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        try {

            Calendar local = getLocalCalendar(tvdbDateString, airtime,
                    prefs.getBoolean(SeriesGuidePreferences.KEY_USE_MY_TIMEZONE, false), ctx);
            String timezone = local.getTimeZone().getDisplayName(true, TimeZone.SHORT);

            String today = "";
            if (DateUtils.isToday(local.getTimeInMillis())) {
                today = ctx.getString(R.string.today) + ", ";
            }

            return today + DateFormat.getDateFormat(ctx).format(local.getTime()) + " " + timezone;
        } catch (ParseException e) {
            // shouldn't happen, if so there are errors in theTVDB
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Returns a local calendar with the air time and date set. If
     * useUserTimeZone is true time and date are converted from Pacific Time to
     * the users time zone.
     * 
     * @param tvdbDateString
     * @param airtime
     * @param useUserTimeZone
     * @param context
     * @return
     * @throws ParseException
     */
    public static Calendar getLocalCalendar(String tvdbDateString, long airtime,
            boolean useUserTimeZone, Context context) throws ParseException {

        Calendar cal;
        if (useUserTimeZone) {
            cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        } else {
            // use local calendar, e.g. for US based users where airtime is the
            // same across time zones
            cal = Calendar.getInstance();
        }

        // set airday
        Date date = Constants.theTVDBDateFormat.parse(tvdbDateString);
        cal.set(Calendar.DATE, date.getDate());
        cal.set(Calendar.MONTH, date.getMonth());
        cal.set(Calendar.YEAR, date.getYear() + 1900);

        // set airtime
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        time.setTimeInMillis(airtime);
        cal.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
        cal.set(Calendar.SECOND, 0);

        // add user-set hour offset
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int offset = Integer.valueOf(prefs.getString(SeriesGuidePreferences.KEY_OFFSET, "0"));
        cal.add(Calendar.HOUR_OF_DAY, offset);

        // check for US Central and automagically subtract one hour
        if (TimeZone.getDefault().getRawOffset() == TimeZone.getTimeZone("US/Central")
                .getRawOffset()) {
            cal.add(Calendar.HOUR_OF_DAY, -1);
        }

        if (useUserTimeZone) {
            // convert to local timezone
            Calendar local = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
            local.setTime(cal.getTime());
            return local;
        } else {
            return cal;
        }
    }

    public static final SimpleDateFormat thetvdbTimeFormatAMPM = new SimpleDateFormat("h:mm aa",
            Locale.US);

    public static final SimpleDateFormat thetvdbTimeFormatAMPMalt = new SimpleDateFormat("h:mmaa",
            Locale.US);

    public static final SimpleDateFormat thetvdbTimeFormatAMPMshort = new SimpleDateFormat("h aa",
            Locale.US);

    public static final SimpleDateFormat thetvdbTimeFormatNormal = new SimpleDateFormat("H:mm",
            Locale.US);

    public static long parseTimeToMilliseconds(String tvdbTimeString) {
        Date time = null;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));

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
            cal.set(Calendar.HOUR_OF_DAY, time.getHours());
            cal.set(Calendar.MINUTE, time.getMinutes());
            cal.set(Calendar.SECOND, 0);
            return cal.getTimeInMillis();
        } else {
            return -1;
        }
    }

    public static String[] parseMillisecondsToTime(long milliseconds, String dayofweek,
            Context context) {
        // return empty strings if time is missing (maybe return at least day?)
        if (context == null || milliseconds == -1) {
            return new String[] {
                    "", ""
            };
        }

        final TimeZone pacificTimeZone = TimeZone.getTimeZone("America/Los_Angeles");
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean useUserTimeZone = prefs.getBoolean(
                SeriesGuidePreferences.KEY_USE_MY_TIMEZONE, false);

        // set calendar time and day (if available) on Pacific cal
        final Calendar cal = Calendar.getInstance(pacificTimeZone);
        cal.setTimeInMillis(milliseconds);
        if (dayofweek != null) {
            cal.set(Calendar.DAY_OF_WEEK, getDayOfWeek(dayofweek));
        }

        // add user-set hour offset
        int offset = Integer.valueOf(prefs.getString(SeriesGuidePreferences.KEY_OFFSET, "0"));
        if (offset != 0) {
            cal.add(Calendar.HOUR_OF_DAY, offset);
        }
        // check for US Central and automagically subtract one hour
        if (TimeZone.getDefault().getRawOffset() == TimeZone.getTimeZone("US/Central")
                .getRawOffset()) {
            cal.add(Calendar.HOUR_OF_DAY, -1);
        }

        // create time string
        final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
        final SimpleDateFormat dayFormat = new SimpleDateFormat("E");
        String day = "";
        final Date date = cal.getTime();

        if (useUserTimeZone) {
            timeFormat.setTimeZone(TimeZone.getDefault());
            dayFormat.setTimeZone(TimeZone.getDefault());
        } else {
            timeFormat.setTimeZone(pacificTimeZone);
            dayFormat.setTimeZone(pacificTimeZone);
        }

        if (dayofweek != null) {
            day = dayFormat.format(date);
        }

        return new String[] {
                timeFormat.format(date), day
        };
    }

    /**
     * Returns a string in format "1x01 title" or "S1E01 title" dependent on a
     * user preference.
     */
    public static String getNextEpisodeString(SharedPreferences prefs, String season,
            String episode, String title) {
        season = getEpisodeNumber(prefs, season, episode);
        season += " " + title;
        return season;
    }

    /**
     * Returns the episode number formatted according to the users preference
     * (e.g. '1x01', 'S01E01', ...).
     */
    public static String getEpisodeNumber(SharedPreferences prefs, String season, String episode) {
        String format = prefs.getString(SeriesGuidePreferences.KEY_NUMBERFORMAT,
                SeriesGuidePreferences.NUMBERFORMAT_DEFAULT);
        if (format.equals(SeriesGuidePreferences.NUMBERFORMAT_DEFAULT)) {
            // 1x01 format
            season += "x";
        } else {
            // S01E01 format
            // make season number always two chars long
            if (season.length() == 1) {
                season = "0" + season;
            }
            if (format.equals(SeriesGuidePreferences.NUMBERFORMAT_ENGLISHLOWER))
                season = "s" + season + "e";
            else
                season = "S" + season + "E";
        }

        // make episode number always two chars long
        if (episode.length() == 1) {
            season += "0";
        }

        season += episode;
        return season;
    }

    /**
     * Splits the string and reassembles it, separating the items with commas.
     * The given object is returned with the new string.
     * 
     * @param tvdbstring
     * @return
     */
    public static String splitAndKitTVDBStrings(String tvdbstring) {
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
    public static Constants.EpisodeSorting getEpisodeSorting(Context context) {
        String[] epsortingData = context.getResources().getStringArray(R.array.epsortingData);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String currentPref = prefs.getString("episodeSorting", epsortingData[1]);

        Constants.EpisodeSorting sorting;
        if (currentPref.equals(epsortingData[0])) {
            sorting = Constants.EpisodeSorting.LATEST_FIRST;
        } else if (currentPref.equals(epsortingData[1])) {
            sorting = Constants.EpisodeSorting.OLDEST_FIRST;
        } else if (currentPref.equals(epsortingData[2])) {
            sorting = Constants.EpisodeSorting.UNWATCHED_FIRST;
        } else if (currentPref.equals(epsortingData[3])) {
            sorting = Constants.EpisodeSorting.ALPHABETICAL_ASC;
        } else if (currentPref.equals(epsortingData[4])) {
            sorting = Constants.EpisodeSorting.ALPHABETICAL_DESC;
        } else if (currentPref.equals(epsortingData[5])) {
            sorting = Constants.EpisodeSorting.DVDLATEST_FIRST;
        } else {
            sorting = Constants.EpisodeSorting.DVDOLDEST_FIRST;
        }

        return sorting;
    }

    public static boolean isHoneycomb() {
        // Can use static final constants like HONEYCOMB, declared in later
        // versions
        // of the OS since they are inlined at compile time. This is guaranteed
        // behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isExtStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            return activeNetworkInfo.isConnected();
        }
        return false;
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo != null) {
            return wifiNetworkInfo.isConnected();
        }
        return false;
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

}
