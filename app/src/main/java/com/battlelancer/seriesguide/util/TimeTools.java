package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.threeten.bp.Clock;
import org.threeten.bp.DateTimeException;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.ChronoUnit;

/**
 * Helper tools for converting and formatting date times for shows and episodes.
 */
public class TimeTools {

    public static final int RELEASE_WEEKDAY_UNKNOWN = -1;
    public static final int RELEASE_WEEKDAY_DAILY = 0;

    private static final String TIMEZONE_ID_PREFIX_AMERICA = "America/";

    public static final String ISO3166_1_UNITED_STATES = "us";
    public static final String TIMEZONE_ID_US_EASTERN = "America/New_York";
    public static final String TIMEZONE_ID_US_EASTERN_DETROIT = "America/Detroit";
    public static final String TIMEZONE_ID_US_CENTRAL = "America/Chicago";
    public static final String TIMEZONE_ID_US_MOUNTAIN = "America/Denver";
    public static final String TIMEZONE_ID_US_ARIZONA = "America/Phoenix";
    public static final String TIMEZONE_ID_US_PACIFIC = "America/Los_Angeles";

    private static final String NETWORK_CBS = "CBS";
    private static final String NETWORK_NBC = "NBC";

    public static boolean isBeforeMillis(OffsetDateTime dateTime, long millis) {
        return dateTime.toInstant().isBefore(Instant.ofEpochMilli(millis));
    }

    public static boolean isAfterMillis(OffsetDateTime dateTime, long millis) {
        return dateTime.toInstant().isAfter(Instant.ofEpochMilli(millis));
    }

    /**
     * Returns whether the given {@link Date} is before now.
     *
     * Note: this may seem harsh, but is equal to how to be released are calculated for seasons.
     *
     * @see com.battlelancer.seriesguide.ui.overview.UnwatchedUpdateWorker
     */
    public static boolean isReleased(Date actualRelease) {
        return actualRelease.before(new Date(System.currentTimeMillis()));
    }

    /**
     * Returns the appropriate time zone for the given tzdata zone identifier.
     *
     * <p> Falls back to "America/New_York" if timezone string is empty or unknown.
     */
    public static ZoneId getDateTimeZone(@Nullable String timezone) {
        if (timezone != null && timezone.length() != 0) {
            try {
                return ZoneId.of(timezone);
            } catch (DateTimeException ignored) {
            }
        }

        return ZoneId.of(TIMEZONE_ID_US_EASTERN);
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
     * Converts US week day string to {@link DayOfWeek#getValue()} day.
     *
     * <p> Returns {@link #RELEASE_WEEKDAY_UNKNOWN} if no conversion is possible or {@link
     * #RELEASE_WEEKDAY_DAILY} if it is "Daily".
     */
    public static int parseShowReleaseWeekDay(String day) {
        if (day == null || day.length() == 0) {
            return -1;
        }

        // catch Monday through Sunday
        switch (day) {
            case "Monday":
                return DayOfWeek.MONDAY.getValue();
            case "Tuesday":
                return DayOfWeek.TUESDAY.getValue();
            case "Wednesday":
                return DayOfWeek.WEDNESDAY.getValue();
            case "Thursday":
                return DayOfWeek.THURSDAY.getValue();
            case "Friday":
                return DayOfWeek.FRIDAY.getValue();
            case "Saturday":
                return DayOfWeek.SATURDAY.getValue();
            case "Sunday":
                return DayOfWeek.SUNDAY.getValue();
            case "Daily":
                return RELEASE_WEEKDAY_DAILY;
        }

        // no match
        return RELEASE_WEEKDAY_UNKNOWN;
    }

    public static boolean isSameWeekDay(Date episodeDateTime, @Nullable Date showDateTime,
            int weekDay) {
        if (weekDay == RELEASE_WEEKDAY_DAILY) {
            return true;
        }
        if (showDateTime == null || weekDay == RELEASE_WEEKDAY_UNKNOWN) {
            return false;
        }

        Instant showInstant = Instant.ofEpochMilli(showDateTime.getTime());
        DayOfWeek showDayOfWeek = LocalDateTime.ofInstant(showInstant, ZoneId.systemDefault())
                .getDayOfWeek();

        Instant episodeInstant = Instant.ofEpochMilli(episodeDateTime.getTime());
        DayOfWeek episodeDayOfWeek = LocalDateTime.ofInstant(episodeInstant, ZoneId.systemDefault())
                .getDayOfWeek();
        return episodeDayOfWeek == showDayOfWeek;
    }

    /**
     * Converts a {@link OffsetDateTime} to an {@link Instant} and outputs its ISO-8601
     * representation, such as '2013-08-20T15:16:26.355Z'.
     */
    public static String parseShowFirstRelease(@Nullable OffsetDateTime date) {
        return date == null ? "" : date.toInstant().toString();
    }

    /**
     * Calculates the episode release date time as a millisecond instant. Adjusts for time zone
     * effects on release time, e.g. delays between time zones (e.g. in the United States) and DST.
     *
     * @param showTimeZone See {@link #getDateTimeZone(String)}.
     * @param showReleaseTime See {@link #getShowReleaseTime(int)}.
     * @return -1 if no conversion was possible. Otherwise, any other long value (may be negative!).
     */
    public static long parseEpisodeReleaseDate(@NonNull ZoneId showTimeZone,
            @Nullable Date releaseDate,
            @NonNull LocalTime showReleaseTime, @Nullable String showCountry,
            @Nullable String showNetwork, @NonNull String deviceTimeZone) {
        if (releaseDate == null) {
            return Constants.EPISODE_UNKNOWN_RELEASE;
        }

        // Get local date: tmdb-java parses date string to Date using SimpleDateFormat,
        // which uses the default time zone.
        Instant instant = Instant.ofEpochMilli(releaseDate.getTime());
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

        // set time
        LocalDateTime localDateTime = localDate.atTime(showReleaseTime);

        localDateTime = handleHourPastMidnight(showCountry, showNetwork, localDateTime);

        // get a valid datetime in the show time zone, this auto-forwards time if inside DST gap
        ZonedDateTime dateTime = localDateTime.atZone(showTimeZone);

        // handle time zone effects on release time for US shows (only if device is set to US zone)
        if (deviceTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA)) {
            dateTime = applyUnitedStatesCorrections(showCountry, deviceTimeZone, dateTime);
        }

        return dateTime.toInstant().toEpochMilli();
    }

    /**
     * Creates the show release time from a {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows#RELEASE_TIME}
     * encoded value.
     *
     * <p> If the encoded time passed is not from 0 to 2359 or the encoded minute is larger than 59,
     * a sensible default is returned.
     */
    public static LocalTime getShowReleaseTime(int showReleaseTime) {
        if (showReleaseTime >= 0 && showReleaseTime <= 2359) {
            int hour = showReleaseTime / 100;
            int minute = showReleaseTime - (hour * 100);
            if (minute <= 59) {
                return LocalTime.of(hour, minute);
            }
        }

        // if no time is available, use a sensible default
        return LocalTime.of(7, 0);
    }

    /**
     * Calculates the current release date time. Adjusts for time zone effects on release time, e.g.
     * delays between time zones (e.g. in the United States) and DST. Adjusts for user-defined
     * offset.
     *
     * @param releaseTime The {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows#RELEASE_TIME}.
     * @return The date is today or on the next day matching the given week day.
     */
    public static Date getShowReleaseDateTime(@NonNull Context context, int releaseTime,
            int weekDay, @Nullable String timeZone, @Nullable String country,
            @Nullable String network) {
        // determine show time zone (falls back to America/New_York)
        ZoneId showTimeZone = getDateTimeZone(timeZone);

        LocalTime time = TimeTools.getShowReleaseTime(releaseTime);
        ZonedDateTime dateTime = getShowReleaseDateTime(time, weekDay,
                showTimeZone, country, network, Clock.system(showTimeZone));

        dateTime = applyUserOffset(context, dateTime);

        return new Date(dateTime.toInstant().toEpochMilli());
    }

    @VisibleForTesting
    @NonNull
    public static ZonedDateTime getShowReleaseDateTime(@NonNull LocalTime time, int weekDay,
            @NonNull ZoneId timeZone, @Nullable String country, @Nullable String network,
            @NonNull Clock clock) {
        // create current date in show time zone, set local show release time
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.now(clock), time);

        // adjust day of week so datetime is today or within the next week
        // for daily shows (weekDay == 0) just use the current day
        if (weekDay >= 1 && weekDay <= 7) {
            // joda tries to preserve week
            // so if we want a week day earlier in the week, advance by 7 days first
            if (weekDay < localDateTime.getDayOfWeek().getValue()) {
                localDateTime = localDateTime.plusWeeks(1);
            }
            localDateTime = localDateTime.with(ChronoField.DAY_OF_WEEK, weekDay);
        }

        localDateTime = handleHourPastMidnight(country, network, localDateTime);

        // get a valid datetime in the show time zone, this auto-forwards time if inside DST gap
        ZonedDateTime dateTime = localDateTime.atZone(timeZone);

        // handle time zone effects on release time for US shows (only if device is set to US zone)
        String localTimeZone = TimeZone.getDefault().getID();
        if (localTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA)) {
            dateTime = applyUnitedStatesCorrections(country, localTimeZone, dateTime);
        }

        return dateTime;
    }

    /**
     * If the release time is within the hour past midnight (0:00 until 0:59) moves the date one day
     * into the future (currently US shows on some networks only).
     *
     * <p> This is due to late night shows being commonly listed as releasing the day before if
     * they air past midnight (e.g. "Monday night at 0:35" actually is Tuesday 0:35).
     *
     * <p> Note: This should never include streaming services like Netflix where midnight is
     * used correctly.
     *
     * <p>Example: https://www.themoviedb.org/tv/62223-the-late-late-show-with-james-corden
     */
    private static LocalDateTime handleHourPastMidnight(@Nullable String country,
            @Nullable String network, LocalDateTime localDateTime) {
        if (localDateTime.getHour() == 0 && ISO3166_1_UNITED_STATES.equals(country)
                && (NETWORK_CBS.equals(network)
                || NETWORK_NBC.equals(network))
        ) {
            return localDateTime.plusDays(1);
        }
        return localDateTime;
    }

    /**
     * Parses the ISO-8601, such as '2013-08-20T15:16:26.355Z', or TVDB date format representation
     * of a show first release date and outputs the year string in the user's default
     * locale.
     *
     * @return Returns {@code null} if the given date time is empty.
     */
    public static String getShowReleaseYear(@Nullable String releaseDateTime) {
        if (releaseDateTime == null || releaseDateTime.length() == 0) {
            return null;
        }

        Instant instant;

        try {
            instant = Instant.parse(releaseDateTime);
        } catch (DateTimeParseException ignored) {
            // legacy format, or otherwise invalid
            try {
                // try legacy date only parser
                instant = LocalDate.parse(releaseDateTime).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        return new SimpleDateFormat("yyyy", Locale.getDefault())
                .format(new Date(instant.toEpochMilli()));
    }

    @VisibleForTesting
    public static ZonedDateTime applyUnitedStatesCorrections(@Nullable String country,
            @NonNull String localTimeZone, @NonNull ZonedDateTime dateTime) {
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
            boolean dstInEastern = ZoneId.of(TIMEZONE_ID_US_EASTERN).getRules()
                    .isDaylightSavings(dateTime.toInstant());
            if (dstInEastern) {
                offset += 2;
            } else {
                offset += 1;
            }
        } else if (localTimeZone.equals(TIMEZONE_ID_US_PACIFIC)) {
            // PST UTC−8:00 or PDT UTC−7:00
            offset += 3;
        }

        dateTime = dateTime.plusHours(offset);

        return dateTime;
    }

    /**
     * Returns the text representation of the given country code. If the country is not supported,
     * "unknown" will be returned.
     */
    public static String getCountry(Context context, String releaseCountry) {
        if (releaseCountry == null || releaseCountry.length() == 0) {
            return context.getString(R.string.unknown);
        }
        String country = new Locale("", releaseCountry).getDisplayCountry(Locale.getDefault());
        if (TextUtils.isEmpty(country)) {
            return context.getString(R.string.unknown);
        }
        return country;
    }

    /**
     * Returns the current system time with inverted user-set offsets applied.
     */
    public static long getCurrentTime(Context context) {
        Calendar calendar = Calendar.getInstance();

        applyUserOffsetInverted(context, calendar);

        return calendar.getTimeInMillis();
    }

    /**
     * Formats to the week day abbreviation (e.g. "Mon") as defined by the devices locale.
     */
    public static String formatToLocalDay(@NonNull Date dateTime) {
        SimpleDateFormat localDayFormat = new SimpleDateFormat("E", Locale.getDefault());
        return localDayFormat.format(dateTime);
    }

    /**
     * Formats to the week day abbreviation (e.g. "Mon") as defined by the devices locale. If the
     * given weekDay is 0, returns the local version of "Daily".
     */
    public static String formatToLocalDayOrDaily(Context context, Date dateTime, int weekDay) {
        if (weekDay == RELEASE_WEEKDAY_DAILY) {
            return context.getString(R.string.daily);
        }
        return formatToLocalDay(dateTime);
    }

    /**
     * Formats to absolute time format (e.g. "08:00 PM") as defined by the devices locale.
     */
    public static String formatToLocalTime(@NonNull Context context, @NonNull Date dateTime) {
        java.text.DateFormat localTimeFormat = DateFormat.getTimeFormat(context);
        return localTimeFormat.format(dateTime);
    }

    /**
     * Formats to relative time in relation to the current system time (e.g. "in 12 min") as defined
     * by the devices locale. If the time difference is lower than a minute, returns the localized
     * equivalent of "now".
     */
    public static String formatToLocalRelativeTime(Context context, Date dateTime) {
        long now = System.currentTimeMillis();
        long dateTimeInstant = dateTime.getTime();

        // if we are below the resolution of getRelativeTimeSpanString, return "now"
        if (Math.abs(now - dateTimeInstant) < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.now);
        }

        return DateUtils
                .getRelativeTimeSpanString(dateTimeInstant, now, DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    /**
     * Formats to day and relative time in relation to the current system time (e.g. "Mon in 3
     * days") as defined by the devices locale. If the time is today, returns the local equivalent
     * for "today".
     */
    public static String formatToLocalDayAndRelativeTime(@NonNull Context context,
            @NonNull Date dateTime) {
        StringBuilder dayAndTime = new StringBuilder();

        // day abbreviation, e.g. "Mon"
        dayAndTime.append(formatToLocalDay(dateTime));
        dayAndTime.append(" ");

        // relative time to dateTime, "today" or e.g. "3 days ago"
        if (DateUtils.isToday(dateTime.getTime())) {
            // show 'today' instead of '0 days ago'
            dayAndTime.append(context.getString(R.string.today));
        } else {
            dayAndTime.append(DateUtils
                    .getRelativeTimeSpanString(
                            dateTime.getTime(),
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL));
        }

        return dayAndTime.toString();
    }

    /**
     * Formats to day and relative week in relation to the current system time (e.g. "Mon in 3
     * weeks") as defined by the devices locale.
     * - If the time is today, returns local variant of 'Released today'.
     * - If the time is within the next or previous 6 days, just returns the day.
     * - If the time is more than 6 weeks away, returns the day with a short date.
     *
     * Note: on Android L_MR1 and below, shows date instead as 'in x weeks' is not supported.
     */
    public static String formatToLocalDayAndRelativeWeek(Context context, Date thenDate) {
        if (DateUtils.isToday(thenDate.getTime())) {
            return context.getString(R.string.released_today);
        }

        // day abbreviation, e.g. "Mon"
        SimpleDateFormat localDayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        StringBuilder dayAndTime = new StringBuilder(localDayFormat.format(thenDate));

        Instant then = Instant.ofEpochMilli(thenDate.getTime());
        ZonedDateTime thenZoned = ZonedDateTime.ofInstant(then, ZoneId.systemDefault());

        // append 'in x wk.' if date is not within a week, for example if today is Thursday:
        // - append for previous Thursday and earlier,
        // - and for next Thursday and later
        long weekDiff = LocalDate.now().until(thenZoned.toLocalDate(), ChronoUnit.WEEKS);
        if (weekDiff != 0) {
            // use weekDiff to calc "now" for relative time string to be daylight saving safe
            // Android L_MR1 and below do not support 'in x wks.', display date instead
            // so make sure that time is the actual time
            Instant now = then.minus(weekDiff * 7, ChronoUnit.DAYS);

            dayAndTime.append(" ");
            if (Math.abs(weekDiff) <= 6) {
                dayAndTime.append(DateUtils
                        .getRelativeTimeSpanString(then.toEpochMilli(), now.toEpochMilli(),
                                DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
            } else {
                // for everything further away from now display date instead
                dayAndTime.append(formatToLocalDateShort(context, thenDate));
            }
        }

        return dayAndTime.toString();
    }

    /**
     * Formats to a date like "October 31", or if the date is not in the current year "October 31,
     * 2010".
     *
     * @see #formatToLocalDateShort(Context, Date)
     */
    public static String formatToLocalDate(Context context, Date dateTime) {
        return DateUtils.formatDateTime(context, dateTime.getTime(), DateUtils.FORMAT_SHOW_DATE);
    }

    /**
     * Formats to a date like "Oct 31", or if the date is not in the current year "Oct 31, 2010".
     *
     * @see #formatToLocalDate(Context, Date)
     */
    public static String formatToLocalDateShort(@NonNull Context context, @NonNull Date dateTime) {
        return DateUtils.formatDateTime(context, dateTime.getTime(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
    }

    /**
     * Formats to a date, time zone and week day (e.g. "2014/02/04 CET (Mon)") as defined by the
     * devices locale. If the date time is today, uses the local equivalent of "today" instead of a
     * week day.
     */
    public static String formatToLocalDateAndDay(Context context, Date dateTime) {
        StringBuilder date = new StringBuilder();

        // date, e.g. "2014/05/31"
        date.append(DateFormat.getDateFormat(context).format(dateTime));
        date.append(" ");

        // device time zone, e.g. "CEST"
        TimeZone timeZone = TimeZone.getDefault();
        date.append(timeZone.getDisplayName(timeZone.inDaylightTime(dateTime), TimeZone.SHORT,
                Locale.getDefault()));

        //
        // Show 'today' instead of e.g. 'Mon'
        String day;
        if (DateUtils.isToday(dateTime.getTime())) {
            day = context.getString(R.string.today);
        } else {
            day = formatToLocalDay(dateTime);
        }

        return context.getString(R.string.format_date_and_day, date.toString(), day);
    }

    /**
     * Returns a date time equal to the given date time plus the user-defined offset.
     */
    private static ZonedDateTime applyUserOffset(Context context, ZonedDateTime dateTime) {
        int offset = DisplaySettings.getShowsTimeOffset(context);
        if (offset != 0) {
            dateTime = dateTime.plusHours(offset);
        }
        return dateTime;
    }

    /**
     * Takes a millisecond date time instant and adds the user-defined offset.
     *
     * <p> Typically required for episode date times stored in the database before formatting them
     * for display.
     */
    public static Date applyUserOffset(Context context, long releaseInstant) {
        // using Android calendar to avoid joda-time lock-up with time zone access
        Calendar dateTime = Calendar.getInstance();
        dateTime.setTimeInMillis(releaseInstant);

        int offset = DisplaySettings.getShowsTimeOffset(context);
        if (offset != 0) {
            dateTime.add(Calendar.HOUR_OF_DAY, offset);
        }
        return dateTime.getTime();
    }

    private static void applyUserOffsetInverted(Context context, Calendar calendar) {
        int offset = DisplaySettings.getShowsTimeOffset(context);

        // invert
        offset = -offset;

        if (offset != 0) {
            calendar.add(Calendar.HOUR_OF_DAY, offset);
        }
    }

    /**
     * Used instead of {@link #applyUserOffset(android.content.Context, long)} if episode time can
     * not be offset, so need to manipulate time instead.
     */
    public static long applyUserOffsetInverted(Context context, long instant) {
        Calendar dateTime = Calendar.getInstance();
        dateTime.setTimeInMillis(instant);

        applyUserOffsetInverted(context, dateTime);

        return dateTime.getTimeInMillis();
    }
}
