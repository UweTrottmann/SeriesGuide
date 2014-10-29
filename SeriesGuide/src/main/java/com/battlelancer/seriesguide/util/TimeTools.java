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
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import java.text.DateFormatSymbols;
import java.text.ParseException;
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
    public static final int RELEASE_DAY_DAILY = 0;

    private static final String TIMEZONE_ID_PREFIX_AMERICA = "America/";

    private static final String AUSTRALIA = "Australia";
    private static final String ISO3166_1_AUSTRALIA = "au";
    private static final String TIMEZONE_ID_AUSTRALIA = "Australia/Sydney";

    private static final String CANADA = "Canada";
    private static final String ISO3166_1_CANADA = "ca";
    private static final String TIMEZONE_ID_CANADA_ATLANTIC = "America/Halifax";
    private static final String TIMEZONE_ID_CANADA_EASTERN = "America/Montreal";
    private static final String TIMEZONE_ID_CANADA_CENTRAL = "America/Winnipeg";
    private static final String TIMEZONE_ID_CANADA_MOUNTAIN = "America/Edmonton";
    private static final String TIMEZONE_ID_CANADA_PACIFIC = "America/Vancouver";

    private static final String FINLAND = "Finland";
    private static final String ISO3166_1_FINLAND = "fi";
    private static final String TIMEZONE_ID_FINLAND = "Europe/Helsinki";

    private static final String GERMANY = "Germany";
    private static final String ISO3166_1_GERMANY = "de";
    private static final String TIMEZONE_ID_GERMANY = "Europe/Berlin";

    private static final String JAPAN = "Japan";
    private static final String ISO3166_1_JAPAN = "jp";
    private static final String TIMEZONE_ID_JAPAN = "Asia/Tokyo";

    private static final String NETHERLANDS = "Netherlands";
    private static final String ISO3166_1_NETHERLANDS = "nl";
    private static final String TIMEZONE_ID_NETHERLANDS = "Europe/Amsterdam";

    private static final String UNITED_KINGDOM = "United Kingdom";
    private static final String ISO3166_1_UNITED_KINGDOM = "gb";
    private static final String TIMEZONE_ID_UK = "Europe/London";

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
    private static final SimpleDateFormat DATE_FORMAT_TVDB = new SimpleDateFormat("yyyy-MM-dd",
            Locale.US);

    static {
        // assume all times are in a custom time zone
        TimeZone customTimeZone = TimeZone.getTimeZone(TIMEZONE_ID_CUSTOM);
        DATE_FORMAT_TVDB.setTimeZone(customTimeZone);
    }

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
        if (weekDay > 0) {
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

    public static String[] formatToShowReleaseTimeAndDay(Context context, long releaseTime,
            String releaseCountry, String releaseDay) {
        // return empty strings if time is missing
        if (releaseTime == -1) {
            return new String[] {
                    "", ""
            };
        }

        int releaseDayOfWeek = getDayOfWeek(releaseDay);

        Calendar calendar = getShowReleaseTime(releaseTime, releaseCountry, releaseDayOfWeek);

        setUserOffset(context, calendar);

        // convert and format to local
        Date actualRelease = calendar.getTime();
        return new String[] {
                formatToLocalReleaseTime(context, actualRelease),
                formatToLocalReleaseDay(context, releaseDayOfWeek, actualRelease)
        };
    }

    /**
     * Returns the Calendar constant (e.g. <code>Calendar.SUNDAY</code>) for a given US English day
     * string (Monday through Sunday) or "Daily".
     *
     * @return Either e.g. <code>Calendar.SUNDAY</code> or 0 if Daily or -1 if no match.
     */
    public static int getDayOfWeek(String day) {
        if (TextUtils.isEmpty(day)) {
            return -1;
        }

        // catch Daily
        if ("Daily".equals(day)) {
            return RELEASE_DAY_DAILY;
        }

        // catch Monday through Sunday
        String[] weekdays = DateFormatSymbols.getInstance(Locale.US).getWeekdays();

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
     * Returns the current system time with inverted user-set offsets applied.
     */
    public static long getCurrentTime(Context context) {
        Calendar calendar = Calendar.getInstance();

        setUserOffsetInverted(context, calendar);

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
     * "in 12 min") defined by the devices locale. If the relative time difference is lower than a
     * minute, returns the localized equivalent of "now".
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
     * Takes a UTC release time and returns the date and time zone (e.g. "2014/02/04 CET") as
     * defined by the devices locale. If the given time is today, the date is prefixed with the
     * local equivalent of "today, ".
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

        // Shows from Canada typically air at the same time across its time zones
        if (CANADA.equals(releaseCountry) || ISO3166_1_CANADA.equals(releaseCountry)) {
            applyCanadaCorrections(calendar);
            return;
        }

        // US shows air at the same LOCAL time across all its time zones (with exceptions)
        if (releaseCountry == null || releaseCountry.length() == 0
                || UNITED_STATES.equals(releaseCountry)
                || ISO3166_1_UNITED_STATES.equals(releaseCountry)) {
            applyUnitedStatesCorrections(calendar);
        }
    }

    /**
     * Reverse some time zone based release time shifts for Canadian time zones.
     */
    private static void applyCanadaCorrections(Calendar calendar) {
        // get device time zone
        final String localTimeZone = TimeZone.getDefault().getID();

        /**
         * Base time zone for Canada is Eastern (see getTimeZoneIdForCountry()).
         *
         * Eastern feed is aired at the same LOCAL time in Pacific.
         * Eastern feed is aired at the same GLOBAL time in Central, Mountain.
         * Eastern feed is aired 1 hour earlier in Atlantic.
         */

        // do nothing when automatic conversion is enough
        if (!localTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA, 0)
                || localTimeZone.equals(TIMEZONE_ID_CANADA_EASTERN)
                || localTimeZone.equals(TIMEZONE_ID_CANADA_CENTRAL)
                || localTimeZone.equals(TIMEZONE_ID_CANADA_MOUNTAIN)) {
            return;
        }

        // need to correct Pacific + Atlantic time zones
        int hourOffset = 0;
        if (localTimeZone.equals(TIMEZONE_ID_CANADA_ATLANTIC)) {
            // 1 hour earlier than Eastern
            hourOffset -= 1;
        } else if (localTimeZone.equals(TIMEZONE_ID_CANADA_PACIFIC)) {
            // same LOCAL time as Eastern
            hourOffset += 3;
        }

        if (hourOffset != 0) {
            calendar.add(Calendar.HOUR_OF_DAY, hourOffset);
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

    private static String getTimeZoneIdForCountry(String releaseCountry) {
        String timeZoneId;
        if (releaseCountry == null || releaseCountry.length() == 0) {
            timeZoneId = TIMEZONE_ID_US_PACIFIC;
        } else {
            switch (releaseCountry) {
                case UNITED_STATES:
                case ISO3166_1_UNITED_STATES:
                    timeZoneId = TIMEZONE_ID_US_PACIFIC;
                    break;
                case CANADA:
                case ISO3166_1_CANADA:
                    // example: https://trakt.tv/show/rookie-blue
                    timeZoneId = TIMEZONE_ID_CANADA_EASTERN;
                    break;
                case UNITED_KINGDOM:
                case ISO3166_1_UNITED_KINGDOM:
                    // example: https://trakt.tv/show/top-gear
                    timeZoneId = TIMEZONE_ID_UK;
                    break;
                case JAPAN:
                case ISO3166_1_JAPAN:
                    // example: https://trakt.tv/show/naruto-shippuuden
                    timeZoneId = TIMEZONE_ID_JAPAN;
                    break;
                case FINLAND:
                case ISO3166_1_FINLAND:
                    // example: https://trakt.tv/show/madventures
                    timeZoneId = TIMEZONE_ID_FINLAND;
                    break;
                case GERMANY:
                case ISO3166_1_GERMANY:
                    // example: https://trakt.tv/show/heuteshow
                    timeZoneId = TIMEZONE_ID_GERMANY;
                    break;
                case NETHERLANDS:
                case ISO3166_1_NETHERLANDS:
                    // example: https://trakt.tv/show/divorce
                    timeZoneId = TIMEZONE_ID_NETHERLANDS;
                    break;
                case AUSTRALIA:
                case ISO3166_1_AUSTRALIA:
                    // example: https://trakt.tv/show/masterchef-australia
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
