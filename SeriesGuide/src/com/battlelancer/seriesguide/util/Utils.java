
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.thetvdbapi.ImageCache;
import com.jakewharton.trakt.ServiceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
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

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static ServiceManager sServiceManagerWithAuthInstance;

    private static ServiceManager sServiceManagerInstance;

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
     * Return an array with absolute time [0], day [1] and relative time [2] of
     * the given millisecond time. Respects user offsets and 'Use my time zone'
     * setting.
     * 
     * @param airtime
     * @param context
     * @return
     */
    public static String[] formatToTimeAndDay(long airtime, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Calendar cal = getAirtimeCalendar(airtime, prefs);

        final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
        final SimpleDateFormat dayFormat = new SimpleDateFormat("E");
        Date airDate = cal.getTime();
        String day = dayFormat.format(airDate);
        String absoluteTime = timeFormat.format(airDate);

        String relativeTime = DateUtils
                .getRelativeTimeSpanString(cal.getTimeInMillis(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();

        return new String[] {
                absoluteTime, day, relativeTime
        };
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

    public static boolean isHoneycombOrHigher() {
        // Can use static final constants like HONEYCOMB, declared in later
        // versions
        // of the OS since they are inlined at compile time. This is guaranteed
        // behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isFroyoOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
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

    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
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
            if (mShowId != null) {
                // update single show
                DBUtils.updateLatestEpisode(mContext, mShowId);
            } else {
                // update all shows
                final Cursor shows = mContext.getContentResolver().query(Shows.CONTENT_URI,
                        new String[] {
                            Shows._ID
                        }, null, null, null);
                while (shows.moveToNext()) {
                    String id = shows.getString(0);
                    DBUtils.updateLatestEpisode(mContext, id);
                }
                shows.close();
            }

            // Adapter gets notified by ContentProvider
        }
    }

    /**
     * Get the trakt-java ServiceManger with user credentials and our API key
     * set.
     * 
     * @param context
     * @param refreshCredentials Set this flag to refresh the user credentials.
     * @return
     * @throws Exception When decrypting the password failed.
     */
    public static synchronized ServiceManager getServiceManagerWithAuth(Context context,
            boolean refreshCredentials) throws Exception {
        if (sServiceManagerWithAuthInstance == null) {
            sServiceManagerWithAuthInstance = new ServiceManager();
            sServiceManagerWithAuthInstance.setApiKey(context.getResources().getString(
                    R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);

            refreshCredentials = true;
        }

        if (refreshCredentials) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                    .getApplicationContext());

            final String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
            String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, "");
            password = SimpleCrypto.decrypt(password, context);

            sServiceManagerWithAuthInstance.setAuthentication(username, password);
        }

        return sServiceManagerWithAuthInstance;
    }

    /**
     * Get a trakt-java ServiceManager with just our API key set. NO user auth
     * data.
     * 
     * @param context
     * @return
     */
    public static synchronized ServiceManager getServiceManager(Context context) {
        if (sServiceManagerInstance == null) {
            sServiceManagerInstance = new ServiceManager();
            sServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);
        }

        return sServiceManagerInstance;
    }

    public static String getTraktUsername(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());

        return prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
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
     * Put the TVDb season string in, get a full 'Season X' or 'Special
     * Episodes' string out.
     * 
     * @param context
     * @param season
     * @return
     */
    public static String getSeasonString(Context context, String season) {
        if (season.equals("0") || season.length() == 0) {
            season = context.getString(R.string.specialseason);
        } else {
            season = context.getString(R.string.season) + " " + season;
        }
        return season;
    }

    /**
     * If {@code isBusy} is {@code true}, then the image is only loaded if it is
     * in memory. In every other case a place-holder is shown.
     * 
     * @param poster
     * @param path
     * @param isBusy
     * @param context
     */
    public static void setPosterBitmap(ImageView poster, String path, boolean isBusy,
            Context context) {
        Bitmap bitmap = null;
        if (path.length() != 0) {
            bitmap = ImageCache.getInstance(context).getThumb(path, isBusy);
        }

        if (bitmap != null) {
            poster.setImageBitmap(bitmap);
            poster.setTag(null);
        } else {
            // set placeholder
            poster.setImageResource(R.drawable.show_generic);
            // Non-null tag means the view still needs to load it's data
            poster.setTag(path);
        }
    }

    /**
     * Run the notification service to display and (re)schedule upcoming episode
     * alarms.
     * 
     * @param context
     */
    public static void runNotificationService(Context context) {
        Intent i = new Intent(context, NotificationService.class);
        context.startService(i);
    }

    public static String toSHA1(byte[] convertme) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] b = md.digest(convertme);

            String result = "";
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }

            return result;
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "Could not get SHA-1 message digest instance", e);
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

    public static void setValueOrPlaceholder(View view, final String value) {
        TextView field = (TextView) view;
        if (value == null || value.length() == 0) {
            field.setText(R.string.episode_unkownairdate);
        } else {
            field.setText(value);
        }
    }

}
