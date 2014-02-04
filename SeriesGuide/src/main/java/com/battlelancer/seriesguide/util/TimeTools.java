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

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helps with converting timestamps used by TVDb and other services.
 */
public class TimeTools {

    public static final String TIMEZONE_ID_CUSTOM = "GMT-08:00";

    private static final String TIMEZONE_ID_PREFIX_AMERICA = "America/";

    private static final String TIMEZONE_ID_AUSTRALIA = "Australia/Sydney";

    private static final String TIMEZONE_ID_GERMANY = "Europe/Berlin";

    private static final String TIMEZONE_ID_JAPAN = "Asia/Tokyo";

    private static final String TIMEZONE_ID_US_EASTERN = "America/New_York";

    private static final Object TIMEZONE_ID_US_EASTERN_DETROIT = "America/Detroit";

    private static final String TIMEZONE_ID_US_CENTRAL = "America/Chicago";

    private static final String TIMEZONE_ID_US_MOUNTAIN = "America/Denver";

    private static final String TIMEZONE_ID_US_ARIZONA = "America/Phoenix";

    private static final String TIMEZONE_ID_US_PACIFIC = "America/Los_Angeles";

    private static final String TIMEZONE_ID_UK = "Europe/London";

    private static final SimpleDateFormat TIME_FORMAT_TRAKT = new SimpleDateFormat(
            "h:mmaa", Locale.US);

    private static final SimpleDateFormat DATE_FORMAT_TVDB = new SimpleDateFormat("yyyy-MM-dd",
            Locale.US);

    static {
        // assume all times are in a custom time zone
        TimeZone customTimeZone = TimeZone.getTimeZone(TIMEZONE_ID_CUSTOM);
        TIME_FORMAT_TRAKT.setTimeZone(customTimeZone);
        DATE_FORMAT_TVDB.setTimeZone(customTimeZone);
    }

    public static final String UNITED_STATES = "United States";

    /**
     * Converts a release time from trakt (e.g. "12:00pm") into a millisecond value. The given time
     * is assumed to be in a custom UTC-08:00 time zone.
     *
     * @return -1 if no conversion was possible, a millisecond value storing the time in UTC-08:00
     * otherwise. The date of the millisecond value should be considered as random, only the time
     * matches the input.
     */
    public static long parseShowReleaseTime(String traktAirTimeString) {
        // try parsing with different formats, starting with the most likely
        Date time = null;
        if (traktAirTimeString != null && traktAirTimeString.length() != 0) {
            try {
                time = TIME_FORMAT_TRAKT.parse(traktAirTimeString);
            } catch (ParseException e) {
                // string may be wrongly formatted
                time = null;
            }
        }

        if (time != null) {
            return time.getTime();
        } else {
            // times resolution is at most in minutes, so -1 (ms) can never exist
            return -1;
        }
    }

    public static long parseEpisodeReleaseTime(String releaseDateEpisode, long releaseTimeShow,
            String releaseCountry) {
        // create calendar, set to custom time zone
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ID_CUSTOM));

        // extract day, month and year
        Date releaseDate;
        try {
            releaseDate = DATE_FORMAT_TVDB.parse(releaseDateEpisode);
        } catch (ParseException e) {
            releaseDate = null;
        }
        if (releaseDate == null) {
            return -1;
        }
        calendar.setTime(releaseDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // extract hour and minute
        int hour;
        int minute;
        if (releaseTimeShow != -1) {
            calendar.setTimeInMillis(releaseTimeShow);
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minute = calendar.get(Calendar.MINUTE);
        } else {
            // no exact time? default to 5 in the morning
            hour = 5;
            minute = 0;
        }

        // set calendar to release time zone
        String timeZoneId = getTimeZoneIdForCountry(releaseCountry);
        calendar.setTimeZone(TimeZone.getTimeZone(timeZoneId));

        // set parsed date and time
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // US shows air at the same LOCAL time across all its time zones (with exceptions)
        // this depends on the current device time zone, so if it changes to/from a US time zone
        // updating all episode time stamps is necessary
        // as current episodes are updated regularly this should not be an issue
        applyCustomCorrections(calendar, hour, releaseCountry);

        return calendar.getTimeInMillis();
    }

    public static String[] formatToShowReleaseTimeAndDay(Context context, long releaseTime,
            String releaseCountry, String releaseDay) {
        // return empty strings if time is missing
        if (releaseTime == -1) {
            return new String[]{
                    "", ""
            };
        }

        int releaseDayOfWeek = getDayOfWeek(releaseDay);

        Calendar calendar = getShowReleaseTime(releaseTime, releaseCountry, releaseDayOfWeek);

        setUserOffset(context, calendar);

        // convert and format to local
        Date actualRelease = calendar.getTime();
        return new String[]{
                formatToLocalReleaseTime(context, actualRelease),
                formatToLocalReleaseDay(context, releaseDayOfWeek, actualRelease)
        };
    }

    /**
     * Returns the Calendar constant (e.g. <code>Calendar.SUNDAY</code>) for a given US English day
     * string (Monday through Sunday) and Daily.
     *
     * @param day Either e.g. <code>Calendar.SUNDAY</code> or 0 if Daily or -1 if no match.
     */
    private static int getDayOfWeek(String day) {
        if (TextUtils.isEmpty(day)) {
            return -1;
        }

        // catch Daily
        if ("Daily".equals(day)) {
            return 0;
        }

        // catch Monday through Sunday
        DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
        String[] weekdays = dfs.getWeekdays();

        for (int i = 1; i < weekdays.length; i++) {
            if (day.equals(weekdays[i])) {
                return i;
            }
        }

        // no match
        return -1;
    }

    private static Calendar getShowReleaseTime(long releaseTime, String releaseCountry,
            int releaseDayOfWeek) {
        // create calendar, set to custom time zone
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ID_CUSTOM));

        // get release "hours"
        calendar.setTimeInMillis(releaseTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // set calendar to release time zone
        String timeZoneId = getTimeZoneIdForCountry(releaseCountry);
        calendar.setTimeZone(TimeZone.getTimeZone(timeZoneId));

        // set to today
        calendar.setTimeInMillis(System.currentTimeMillis());

        // set release "hours" on release country calendar
        // has to be done as release time typically stays the same even if DST starts
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // move to correct release day (not for daily shows)
        if (releaseDayOfWeek > 0) {
            // make sure we always assume a release date which is today or in the future
            // to get correct local DST information when converting
            int todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            // get how far release day is ahead of today
            int daysToMoveAhead = (releaseDayOfWeek + 7 - todayDayOfWeek) % 7;
            // move ahead accordingly
            calendar.add(Calendar.DAY_OF_MONTH, daysToMoveAhead);
        }

        // apply some TV specific corrections
        applyCustomCorrections(calendar, hour, releaseCountry);

        return calendar;
    }

    /**
     * Takes the UTC time in ms of an episode release (see {@link #parseEpisodeReleaseTime(String,
     * long, String)}) and adds user-set offsets.
     */
    public static Date getEpisodeReleaseTime(Context context, long releaseTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(releaseTime);

        setUserOffset(context, calendar);

        return calendar.getTime();
    }

    /**
     * Returns the current system time with user-set offsets applied.
     */
    public static long getCurrentTime(Context context) {
        Calendar calendar = Calendar.getInstance();

        setUserOffset(context, calendar);

        return calendar.getTimeInMillis();
    }

    private static String formatToLocalReleaseDay(Context context, int releaseDayOfWeek,
            Date actualRelease) {
        String localReleaseDay;
        if (releaseDayOfWeek == -1) {
            localReleaseDay = "";
        } else if (releaseDayOfWeek == 0) {
            localReleaseDay = context.getString(R.string.daily);
        } else {
            localReleaseDay = formatToLocalReleaseDay(actualRelease);
        }
        return localReleaseDay;
    }

    /**
     * Takes a UTC release time and returns the week day abbreviation (e.g. "Mon") defined by the
     * devices locale.
     */
    public static String formatToLocalReleaseDay(Date actualRelease) {
        SimpleDateFormat localDayFormat = new SimpleDateFormat("E", Locale.getDefault());
        return localDayFormat.format(actualRelease);
    }

    /**
     * Takes a UTC release time and converts it to the absolute time format (e.g. "08:00 PM")
     * defined by the devices locale.
     */
    public static String formatToLocalReleaseTime(Context context, Date actualRelease) {
        java.text.DateFormat localTimeFormat = DateFormat.getTimeFormat(context);
        return localTimeFormat.format(actualRelease);
    }

    /**
     * Takes a UTC release time and returns the relative time until the current system time (e.g.
     * "in 12 min") defined by the devices locale.
     */
    public static String formatToRelativeLocalReleaseTime(Date actualRelease) {
        return DateUtils
                .getRelativeTimeSpanString(actualRelease.getTime(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    /**
     * Takes a UTC release time and returns the day as well as relative time until the current
     * system time (e.g. "Mon in 3 days") as defined by the devices locale. If the time is today,
     * only the local equivalent for "today" will be returned.
     */
    public static String formatToDayAndRelativeTime(Context context, Date actualRelease) {
        StringBuilder timeAndDay = new StringBuilder();

        timeAndDay.append(formatToLocalReleaseDay(actualRelease));

        timeAndDay.append(" ");

        // Show 'today' instead of '0 days ago'
        if (DateUtils.isToday(actualRelease.getTime())) {
            timeAndDay.append(context.getString(R.string.today));
        } else {
            timeAndDay.append(DateUtils
                    .getRelativeTimeSpanString(
                            actualRelease.getTime(),
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL));
        }

        return timeAndDay.toString();
    }

    /**
     * Corrects the "hour past midnight" and for US shows the release time if the device is set to a
     * US time zone.
     */
    private static void applyCustomCorrections(Calendar calendar, int releaseHourOfDay,
            String releaseCountry) {
        // TV scheduling madness: times between 12:00AM (midnight) and 12:59AM are attributed
        // to the hour after the end of the current day
        // example: Late Night with Jimmy Fallon
        if (releaseHourOfDay == 0) {
            // move ahead one day (24 hours)
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // US shows air at the same LOCAL time across all its time zones (with exceptions)
        if (releaseCountry == null || releaseCountry.length() == 0
                || UNITED_STATES.equals(releaseCountry)) {
            applyUnitedStatesCorrections(calendar);
        }
    }

    /**
     * If the device is set to a US time zone, adjusts the time based on the assumption that all US
     * shows air at the same LOCAL time across US time zones (also handles exceptions for e.g. US
     * Central time).<br/> <b>Do only call this for TV shows released in the US!</b>
     */
    private static void applyUnitedStatesCorrections(Calendar calendar) {
        // get device time zone
        final String localTimeZone = TimeZone.getDefault().getID();

        // no-op if device is set to US Pacific or non-US time zone
        if (!localTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA, 0)
                || localTimeZone.equals(TIMEZONE_ID_US_PACIFIC)) {
            return;
        }

        // by default US shows are either in PST UTC−8:00 or PDT UTC−7:00
        // see #getTimeZoneIdForCountry()

        int offset = 0;
        if (localTimeZone.equals(TIMEZONE_ID_US_MOUNTAIN)) {
            // MST UTC−7:00, MDT UTC−6:00
            offset -= 1;
        } else if (localTimeZone.equals(TIMEZONE_ID_US_ARIZONA)) {
            // is always UTC-07:00, so like Mountain, but no DST
            boolean pacificInDaylight = calendar.getTimeZone().inDaylightTime(calendar.getTime());
            if (!pacificInDaylight) {
                offset -= 1;
            }
        } else if (localTimeZone.equals(TIMEZONE_ID_US_CENTRAL)) {
            // CST UTC−6:00, CDT UTC−5:00
            // shows typically release an hour earlier, so subtract one hour more
            offset -= (2 + 1);
        } else if (localTimeZone.equals(TIMEZONE_ID_US_EASTERN) || localTimeZone
                .equals(TIMEZONE_ID_US_EASTERN_DETROIT)) {
            // EST UTC−5:00, EDT UTC−4:00
            offset -= 3;
        }

        // TODO all applicable US time zones enter/leave DST at the same LOCAL time
        // correct for the short period where eastern zones already enabled/disabled DST,
        // but the given calendar is still in PST/PDT
        // boolean isInDaylight = TimeZone.getTimeZone(localTimeZone).inDaylightTime();

        if (offset != 0) {
            calendar.add(Calendar.HOUR_OF_DAY, offset);
        }
    }

    private static String getTimeZoneIdForCountry(String releaseCountry) {
        String timeZoneId;
        if (releaseCountry == null || releaseCountry.length() == 0) {
            timeZoneId = TIMEZONE_ID_US_PACIFIC;
        } else {
            switch (releaseCountry) {
                case UNITED_STATES:
                    timeZoneId = TIMEZONE_ID_US_PACIFIC;
                    break;
                case "United Kingdom":
                    timeZoneId = TIMEZONE_ID_UK;
                    break;
                case "Japan":
                    timeZoneId = TIMEZONE_ID_JAPAN;
                    break;
                case "Germany":
                    timeZoneId = TIMEZONE_ID_GERMANY;
                    break;
                case "Australia":
                    timeZoneId = TIMEZONE_ID_AUSTRALIA;
                    break;
                default:
                    timeZoneId = TIMEZONE_ID_US_PACIFIC;
                    break;
            }
        }
        return timeZoneId;
    }

    private static void setUserOffset(Context context, Calendar calendar) {
        String offsetString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SeriesGuidePreferences.KEY_OFFSET, "0");
        int offset = Integer.valueOf(offsetString);

        if (offset != 0) {
            calendar.add(Calendar.HOUR_OF_DAY, offset);
        }
    }

}
