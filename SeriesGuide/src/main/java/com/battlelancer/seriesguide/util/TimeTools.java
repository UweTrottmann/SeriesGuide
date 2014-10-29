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

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Helps with converting timestamps used by TVDb and other services.
 */
public class TimeTools {

    public static final String TIMEZONE_ID_CUSTOM = "GMT-08:00";
    public static final int RELEASE_WEEKDAY_DAILY = 0;

    private static final String TIMEZONE_ID_PREFIX_AMERICA = "America/";

    private static final String AUSTRALIA = "Australia";
    private static final String ISO3166_1_AUSTRALIA = "au";

    private static final String CANADA = "Canada";
    private static final String ISO3166_1_CANADA = "ca";

    private static final String FINLAND = "Finland";
    private static final String ISO3166_1_FINLAND = "fi";

    private static final String GERMANY = "Germany";
    private static final String ISO3166_1_GERMANY = "de";

    private static final String JAPAN = "Japan";
    private static final String ISO3166_1_JAPAN = "jp";

    private static final String NETHERLANDS = "Netherlands";
    private static final String ISO3166_1_NETHERLANDS = "nl";

    private static final String UNITED_KINGDOM = "United Kingdom";
    private static final String ISO3166_1_UNITED_KINGDOM = "gb";

    private static final String UNITED_STATES = "United States";
    private static final String ISO3166_1_UNITED_STATES = "us";
    private static final String TIMEZONE_ID_US_EASTERN = "America/New_York";
    private static final Object TIMEZONE_ID_US_EASTERN_DETROIT = "America/Detroit";
    private static final String TIMEZONE_ID_US_CENTRAL = "America/Chicago";
    private static final String TIMEZONE_ID_US_MOUNTAIN = "America/Denver";
    private static final String TIMEZONE_ID_US_ARIZONA = "America/Phoenix";
    private static final String TIMEZONE_ID_US_PACIFIC = "America/Los_Angeles";

    private static final DateTimeFormatter DATE_TIME_FORMATTER_UTC
            = ISODateTimeFormat.dateTime().withZoneUTC();

    /**
     * Parses a ISO 8601 time string (e.g. "20:30") and encodes it into an integer with format
     * "hhmm" (e.g. 2030).
     *
     * <p> If time is invalid returns -1. Performs no extensive formatting check, though.
     */
    public static int parseShowReleaseTime(@Nullable String localTime) {
        if (localTime == null || localTime.length() != 5) {
            return -1;
        }

        // extract hour and minute, example: "20:30" => hour = 20, minute = 30
        int hour = Integer.valueOf(localTime.substring(0, 2));
        int minute = Integer.valueOf(localTime.substring(3, 5));

        // return int encoded time, e.g. hhmm (2030)
        return hour * 100 + minute;
    }

    /**
     * Parses a {@link DateTime} to its ISO datetime string representation (in UTC).
     */
    public static String parseShowFirstRelease(@Nullable DateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER_UTC.print(dateTime);
    }

    /**
     * Calculates the episode release time as a millisecond instant. Adjusts for time zone effects
     * on release time, e.g. delays between time zones (e.g. in the United States) and DST.
     *
     * @return -1 if no conversion was possible. Otherwise, any other long value (may be negative!).
     */
    public static long parseEpisodeReleaseTime(@Nonnull DateTimeFormatter formatter,
            @Nullable String releaseDate, int showReleaseTime, @Nullable String country,
            @Nonnull String deviceTimeZone) {
        if (releaseDate == null || releaseDate.length() == 0) {
            return -1;
        }

        // get date
        DateTime dateTime = formatter.parseDateTime(releaseDate);

        // set time if available
        if (showReleaseTime != -1) {
            int hour = showReleaseTime / 100;
            int minute = showReleaseTime - (hour * 100);
            dateTime = dateTime.withTime(hour, minute, 0, 0);
        }

        // handle time zone effects on release time for US shows (only if device is set to US zone)
        if (deviceTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA)) {
            dateTime = applyUnitedStatesCorrections(country, deviceTimeZone, dateTime);
        }

        return dateTime.getMillis();
    }

    /**
     * Returns the appropriate time zone for the given tzdata zone identifier.
     *
     * <p> Falls back to "America/New_York" if timezone string is empty.
     */
    private static DateTimeZone getDateTimeZone(@Nullable String timezone) {
        return (timezone == null || timezone.length() == 0) ?
                DateTimeZone.forID(TIMEZONE_ID_US_EASTERN) : DateTimeZone.forID(timezone);
    }

    /**
     * Create a date formatter for TheTVDB. Cache and re-use with {@link #parseEpisodeReleaseTime}.
     */
    public static DateTimeFormatter getTvdbDateFormatter(@Nullable String timeZone) {
        return ISODateTimeFormat.date().withZone(getDateTimeZone(timeZone));
    }

    /**
     * Calculates the current release time as a millisecond instant. Adjusts for time zone effects
     * on release time, e.g. delays between time zones (e.g. in the United States) and DST.
     *
     * @return -1 if no conversion was possible. The date is today or on the next day matching the
     * given week day.
     */
    public static long getShowReleaseTime(int time, int weekDay, @Nullable String timezone,
            @Nullable String country) {
        // no time, no fun. also catch old ms format.
        if (time == -1 || time > 2359) {
            return -1;
        }

        // extract hour and minute
        int hour = time / 100;
        int minute = time - (hour * 100);

        // determine time zone (fall back to America/New_York)
        DateTimeZone timeZone = getDateTimeZone(timezone);

        // get current datetime
        DateTime dateTime = new DateTime().withZone(timeZone).withTime(hour, minute, 0, 0);

        // adjust day of week so datetime is today or within the next week
        if (weekDay >= 1 && weekDay <= 7) {
            // joda tries to preserve week
            // so if we want a week day earlier in the week, advance by 7 days first
            if (weekDay < dateTime.getDayOfWeek()) {
                dateTime = dateTime.plusWeeks(1);
            }
            dateTime = dateTime.withDayOfWeek(weekDay);
        }

        // handle time zone effects on release time for US shows (only if device is set to US zone)
        String localTimeZone = TimeZone.getDefault().getID();
        if (localTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA)) {
            dateTime = applyUnitedStatesCorrections(country, localTimeZone, dateTime);
        }

        return dateTime.getMillis();
    }

    /**
     * Calculate the year string of a show's first release in the user's default locale from an ISO
     * date time string.
     *
     * @return Returns {@code null} if the given date time is empty.
     */
    public static String getShowReleaseYear(@Nullable String releaseDateTime) {
        if (releaseDateTime == null || releaseDateTime.length() == 0) {
            return null;
        }
        DateTime dateTime = DATE_TIME_FORMATTER_UTC.parseDateTime(releaseDateTime);
        return new SimpleDateFormat("yyyy", Locale.getDefault()).format(dateTime.toDate());
    }

    private static DateTime applyUnitedStatesCorrections(@Nullable String country,
            @Nonnull String localTimeZone, @Nonnull DateTime dateTime) {
        // assumed base time zone for US shows by trakt is America/New_York
        // EST UTC−5:00, EDT UTC−4:00

        // east feed (default): simultaneously in Eastern and Central
        // delayed 1 hour in Mountain
        // delayed three hours in Pacific
        // <==>
        // same local time in Eastern + Pacific (e.g. 20:00)
        // same local time in Central + Mountain (e.g. 19:00)

        // not a US show or no correction necessary (getting east feed)
        if (!ISO3166_1_UNITED_STATES.equals(country)
                || localTimeZone.equals(TIMEZONE_ID_US_EASTERN)
                || localTimeZone.equals(TIMEZONE_ID_US_EASTERN_DETROIT)
                || localTimeZone.equals(TIMEZONE_ID_US_CENTRAL)) {
            return dateTime;
        }

        int offset = 0;
        if (localTimeZone.equals(TIMEZONE_ID_US_MOUNTAIN)) {
            // MST UTC−7:00, MDT UTC−6:00
            offset += 1;
        } else if (localTimeZone.equals(TIMEZONE_ID_US_ARIZONA)) {
            // is always UTC-07:00, so like Mountain, but no DST
            boolean noDstInEastern = DateTimeZone.forID(TIMEZONE_ID_US_EASTERN)
                    .isStandardOffset(dateTime.getMillis());
            if (noDstInEastern) {
                offset += 1;
            } else {
                offset += 2;
            }
        } else if (localTimeZone.equals(TIMEZONE_ID_US_PACIFIC)) {
            // PST UTC−8:00 or PDT UTC−7:00
            offset += 3;
        }

        dateTime = dateTime.plusHours(offset);

        return dateTime;
    }

    /**
     * Converts US week day string to {@link org.joda.time.DateTimeConstants} day.
     *
     * <p> Returns -1 if no conversion is possible.
     */
    public static int parseDayOfWeek(String day) {
        if (day == null || day.length() == 0) {
            return -1;
        }

        // catch Monday through Sunday
        switch (day) {
            case "Monday":
                return DateTimeConstants.MONDAY;
            case "Tuesday":
                return DateTimeConstants.TUESDAY;
            case "Wednesday":
                return DateTimeConstants.WEDNESDAY;
            case "Thursday":
                return DateTimeConstants.THURSDAY;
            case "Friday":
                return DateTimeConstants.FRIDAY;
            case "Saturday":
                return DateTimeConstants.SATURDAY;
            case "Sunday":
                return DateTimeConstants.SUNDAY;
        }

        // no match
        return -1;
    }

    /**
     * Takes the UTC millisecond instant of an episode release time (see {@link
     * #parseEpisodeReleaseTime}) and adds user-set offsets.
     */
    public static Date getEpisodeReleaseTime(Context context, long releaseTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(releaseTime);

        setUserOffset(context, calendar);

        return calendar.getTime();
    }

    /**
     * Returns the current system time with inverted user-set offsets applied.
     */
    public static long getCurrentTime(Context context) {
        Calendar calendar = Calendar.getInstance();

        setUserOffsetInverted(context, calendar);

        return calendar.getTimeInMillis();
    }

    /**
     * Formats to the week day abbreviation (e.g. "Mon") as defined by the devices local.
     */
    public static String formatToLocalReleaseDay(Date actualRelease) {
        SimpleDateFormat localDayFormat = new SimpleDateFormat("E", Locale.getDefault());
        return localDayFormat.format(actualRelease);
    }

    /**
     * Formats to absolute time format (e.g. "08:00 PM") as defined by the devices locale.
     */
    public static String formatToLocalReleaseTime(Context context, Date actualRelease) {
        java.text.DateFormat localTimeFormat = DateFormat.getTimeFormat(context);
        return localTimeFormat.format(actualRelease);
    }

    /**
     * Formats to relative time in relation to the current system time (e.g. "in 12 min") as defined
     * by the devices locale. If the time difference is lower than a minute, returns the localized
     * equivalent of "now".
     */
    public static String formatToRelativeLocalReleaseTime(Context context, Date actualRelease) {
        long now = System.currentTimeMillis();
        long releaseTime = actualRelease.getTime();

        // if we are below the resolution of getRelativeTimeSpanString, return "now"
        if (Math.abs(now - releaseTime) < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.now);
        }

        return DateUtils
                .getRelativeTimeSpanString(releaseTime, now, DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    /**
     * Formats to day and relative time in relation to the current system time (e.g. "Mon in 3
     * days") as defined by the devices locale. If the time is today, returns the local equivalent
     * for "today".
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
     * Formats to a date and time zone (e.g. "2014/02/04 CET") as defined by the devices locale. If
     * the time is today, the date is prefixed with the local equivalent of "today, ".
     */
    public static String formatToDate(Context context, Date actualRelease) {
        StringBuilder date = new StringBuilder();

        // date, e.g. "2014/05/31"
        date.append(DateFormat.getDateFormat(context).format(actualRelease));

        date.append(" ");

        // time zone, e.g. "CEST"
        TimeZone timeZone = TimeZone.getDefault();
        date.append(timeZone.getDisplayName(timeZone.inDaylightTime(actualRelease), TimeZone.SHORT,
                Locale.getDefault()));

        // Show 'today' instead of e.g. 'Mon'
        String day;
        if (DateUtils.isToday(actualRelease.getTime())) {
            day = context.getString(R.string.today);
        } else {
            day = formatToLocalReleaseDay(actualRelease);
        }

        return context.getString(R.string.release_date_and_day, date.toString(), day);
    }

    /**
     * Returns the text representation of the given country code. If the country is not supported,
     * "unknown" will be returned.
     */
    public static String getCountry(Context context, String releaseCountry) {
        if (releaseCountry == null || releaseCountry.length() == 0) {
            return context.getString(R.string.unknown);
        }
        switch (releaseCountry) {
            case ISO3166_1_AUSTRALIA:
                return AUSTRALIA;
            case ISO3166_1_CANADA:
                return CANADA;
            case ISO3166_1_JAPAN:
                return JAPAN;
            case ISO3166_1_FINLAND:
                return FINLAND;
            case ISO3166_1_GERMANY:
                return GERMANY;
            case ISO3166_1_NETHERLANDS:
                return NETHERLANDS;
            case ISO3166_1_UNITED_KINGDOM:
                return UNITED_KINGDOM;
            case ISO3166_1_UNITED_STATES:
                return UNITED_STATES;
            default:
                return context.getString(R.string.unknown);
        }
    }

    private static void setUserOffset(Context context, Calendar calendar) {
        int offset = getUserOffset(context);

        if (offset != 0) {
            calendar.add(Calendar.HOUR_OF_DAY, offset);
        }
    }

    private static void setUserOffsetInverted(Context context, Calendar calendar) {
        int offset = getUserOffset(context);

        // invert
        offset = -offset;

        if (offset != 0) {
            calendar.add(Calendar.HOUR_OF_DAY, offset);
        }
    }

    private static int getUserOffset(Context context) {
        try {
            return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(SeriesGuidePreferences.KEY_OFFSET, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
