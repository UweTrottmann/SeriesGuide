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

import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;

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

    private static final String TIMEZONE_ID_AUSTRALIA = "Australia/Sydney";

    private static final String TIMEZONE_ID_GERMANY = "Europe/Berlin";

    private static final String TIMEZONE_ID_JAPAN = "Asia/Tokyo";

    private static final String TIMEZONE_ID_US_PACIFIC = "America/Los_Angeles";

    private static final String TIMEZONE_ID_UK = "Europe/London";

    private static final SimpleDateFormat TIME_FORMAT_TRAKT = new SimpleDateFormat(
            "h:mmaa", Locale.US);

    static {
        // assume all TVDb times are in a custom time zone
        TimeZone customTimeZone = TimeZone.getTimeZone(TIMEZONE_ID_CUSTOM);
        TIME_FORMAT_TRAKT.setTimeZone(customTimeZone);
    }

    /**
     * Converts a release time from trakt (e.g. "12:00pm") into a millisecond value. The given time
     * is assumed to be in a custom UTC-08:00 time zone.
     *
     * @return -1 if no conversion was possible, a millisecond value storing the time in UTC-08:00
     * otherwise. The date of the millisecond value should be considered as random, only the time
     * matches the input.
     */
    public static long parseTimeToMilliseconds(String traktAirTimeString) {
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

    public static String[] formatShowReleaseTimeAndDay(Context context, long releaseTime,
            String releaseCountry, String releaseDay) {
        // return empty strings if time is missing
        if (releaseTime == -1) {
            return new String[]{
                    "", ""
            };
        }

        // determine release timezone
        String timeZoneId;
        if (TextUtils.isEmpty(releaseCountry)) {
            timeZoneId = TIMEZONE_ID_US_PACIFIC;
        } else {
            switch (releaseCountry) {
                case "United States":
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

        // get release "hours"
        Calendar customTimeCalendar = Calendar
                .getInstance(TimeZone.getTimeZone(TIMEZONE_ID_CUSTOM));
        customTimeCalendar.setTimeInMillis(releaseTime);
        int releaseHourOfDay = customTimeCalendar.get(Calendar.HOUR_OF_DAY);
        int releaseMinute = customTimeCalendar.get(Calendar.MINUTE);

        // set release "hours" on release country calendar
        // has to be done as release time typically stays the same even if DST starts
        Calendar releaseTimeZoneCal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
        releaseTimeZoneCal.set(Calendar.HOUR_OF_DAY, releaseHourOfDay);
        releaseTimeZoneCal.set(Calendar.MINUTE, releaseMinute);
        releaseTimeZoneCal.set(Calendar.SECOND, 0);
        releaseTimeZoneCal.set(Calendar.MILLISECOND, 0);

        // move to correct release day (not for daily shows)
        int releaseDayOfWeek = getDayOfWeek(releaseDay);
        if (releaseDayOfWeek > 0) {
            // make sure we always assume a release date which is today or in the future
            // to get correct local DST information when converting
            int todayDayOfWeek = releaseTimeZoneCal.get(Calendar.DAY_OF_WEEK);
            // get how far release day is ahead of today
            int daysToMoveAhead = (releaseDayOfWeek + 7 - todayDayOfWeek) % 7;
            // move ahead accordingly
            releaseTimeZoneCal.add(Calendar.DAY_OF_MONTH, daysToMoveAhead);
        }

        // TV scheduling madness: times between 12:00AM and 12:59AM are attributed to the hour
        // after the end of the current day
        if (releaseHourOfDay == 0) {
            // move ahead one day (24 hours)
            releaseTimeZoneCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        Date actualRelease = releaseTimeZoneCal.getTime();

        // convert and format to local time
        java.text.DateFormat localTimeFormat = DateFormat.getTimeFormat(context);
        String localReleaseTime = localTimeFormat.format(actualRelease);

        // convert and format to local day
        String localReleaseDay;
        if (releaseDayOfWeek == -1) {
            localReleaseDay = "";
        } else if (releaseDayOfWeek == 0) {
            localReleaseDay = context.getString(R.string.daily);
        } else {
            SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
            localReleaseDay = dayFormat.format(actualRelease);
        }

        return new String[]{localReleaseTime, localReleaseDay};
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

}
