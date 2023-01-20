package com.battlelancer.seriesguide.util

import android.content.Context
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import org.threeten.bp.Clock
import org.threeten.bp.DateTimeException
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeParseException
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Helper tools for converting and formatting date times for shows and episodes.
 */
object TimeTools {

    const val RELEASE_WEEKDAY_UNKNOWN = -1
    const val RELEASE_WEEKDAY_DAILY = 0

    private const val TIMEZONE_ID_PREFIX_AMERICA = "America/"

    const val ISO3166_1_UNITED_STATES = "us"
    const val TIMEZONE_ID_US_EASTERN = "America/New_York"
    const val TIMEZONE_ID_US_EASTERN_DETROIT = "America/Detroit"
    const val TIMEZONE_ID_US_CENTRAL = "America/Chicago"
    const val TIMEZONE_ID_US_MOUNTAIN = "America/Denver"
    const val TIMEZONE_ID_US_ARIZONA = "America/Phoenix"
    const val TIMEZONE_ID_US_PACIFIC = "America/Los_Angeles"

    private const val NETWORK_CBS = "CBS"
    private const val NETWORK_NBC = "NBC"

    @JvmStatic
    fun isBeforeMillis(dateTime: OffsetDateTime, millis: Long): Boolean {
        return dateTime.toInstant().isBefore(Instant.ofEpochMilli(millis))
    }

    @JvmStatic
    fun isAfterMillis(dateTime: OffsetDateTime, millis: Long): Boolean {
        return dateTime.toInstant().isAfter(Instant.ofEpochMilli(millis))
    }

    /**
     * Returns whether the given [Date] is before now.
     *
     * Note: this may seem harsh, but is equal to how "to be released" are calculated for seasons.
     *
     * @see com.battlelancer.seriesguide.shows.overview.UnwatchedUpdateWorker
     */
    fun isReleased(actualRelease: Date): Boolean {
        return actualRelease.before(Date(System.currentTimeMillis()))
    }

    /**
     * Returns the appropriate time zone for the given tzdata zone identifier.
     *
     * If the string is empty or zone unknown, falls back to the devices time zone,
     * or [TIMEZONE_ID_US_EASTERN].
     */
    fun getDateTimeZone(timezone: String?): ZoneId {
        if (timezone != null && timezone.isNotEmpty()) {
            try {
                return ZoneId.of(timezone)
            } catch (ignored: DateTimeException) {
            }
        }
        return safeSystemDefaultZoneId()
    }

    /**
     * The system default time zone, or on failure [TIMEZONE_ID_US_EASTERN].
     *
     * ZoneId.systemDefault may fail if the ID returned by the system does not exist in the time
     * zone data included in the current threetenbp version (time zones get renamed and added).
     * If a zone is reported, check for updates of this library.
     */
    fun safeSystemDefaultZoneId(): ZoneId {
        return try {
            ZoneId.systemDefault()
        } catch (e: Exception) {
            Errors.logAndReport("Failed to get system time zone", e)
            // Use a default zone ID that is very likely to exist.
            ZoneId.of(TIMEZONE_ID_US_EASTERN)
        }
    }

    /**
     * Parses a ISO 8601 time string (e.g. "20:30") and encodes it into an integer with format
     * "hhmm" (e.g. 2030).
     *
     *  If time is invalid returns -1. Performs no extensive formatting check, though.
     */
    fun parseShowReleaseTime(localTime: String?): Int {
        if (localTime == null || localTime.length != 5) {
            return -1
        }

        // extract hour and minute, example: "20:30" => hour = 20, minute = 30
        val hour = Integer.valueOf(localTime.substring(0, 2))
        val minute = Integer.valueOf(localTime.substring(3, 5))

        // return int encoded time, e.g. hhmm (2030)
        return hour * 100 + minute
    }

    /**
     * Converts US week day string to [DayOfWeek.getValue] day.
     *
     *  Returns [RELEASE_WEEKDAY_UNKNOWN] if no conversion is possible or
     *  [RELEASE_WEEKDAY_DAILY] if it is "Daily".
     */
    fun parseShowReleaseWeekDay(day: String?): Int {
        if (day == null || day.isEmpty()) {
            return RELEASE_WEEKDAY_UNKNOWN
        }
        return when (day) {
            "Monday" -> DayOfWeek.MONDAY.value
            "Tuesday" -> DayOfWeek.TUESDAY.value
            "Wednesday" -> DayOfWeek.WEDNESDAY.value
            "Thursday" -> DayOfWeek.THURSDAY.value
            "Friday" -> DayOfWeek.FRIDAY.value
            "Saturday" -> DayOfWeek.SATURDAY.value
            "Sunday" -> DayOfWeek.SUNDAY.value
            "Daily" -> RELEASE_WEEKDAY_DAILY
            else -> RELEASE_WEEKDAY_UNKNOWN
        }
    }

    fun isSameWeekDay(episodeDateTime: Date, showDateTime: Date?, weekDay: Int): Boolean {
        if (weekDay == RELEASE_WEEKDAY_DAILY) {
            return true
        }
        if (showDateTime == null || weekDay == RELEASE_WEEKDAY_UNKNOWN) {
            return false
        }

        val zoneId = safeSystemDefaultZoneId()
        val showInstant = Instant.ofEpochMilli(showDateTime.time)
        val showDayOfWeek = LocalDateTime.ofInstant(showInstant, zoneId).dayOfWeek

        val episodeInstant = Instant.ofEpochMilli(episodeDateTime.time)
        val episodeDayOfWeek = LocalDateTime.ofInstant(episodeInstant, zoneId).dayOfWeek
        return episodeDayOfWeek == showDayOfWeek
    }

    /**
     * Converts a [OffsetDateTime] to an [Instant] and outputs its ISO-8601
     * representation, such as '2013-08-20T15:16:26.355Z'.
     */
    fun parseShowFirstRelease(date: OffsetDateTime?): String {
        return date?.toInstant()?.toString() ?: ""
    }

    /**
     * Calculates the episode release date time as a millisecond instant. Adjusts for time zone
     * effects on release time, e.g. delays between time zones (e.g. in the United States) and DST.
     *
     * @param showTimeZone    See [getDateTimeZone].
     * @param showReleaseTime See [getShowReleaseTime].
     * @return [SgEpisode2.EPISODE_UNKNOWN_RELEASE] if no conversion was possible. Otherwise,
     * any other long value (may be negative!).
     */
    fun parseEpisodeReleaseDate(
        showTimeZone: ZoneId,
        releaseDate: Date?,
        showReleaseTime: LocalTime, showCountry: String?,
        showNetwork: String?, deviceTimeZone: String
    ): Long {
        if (releaseDate == null) {
            return SgEpisode2.EPISODE_UNKNOWN_RELEASE
        }

        // Get local date: tmdb-java parses date string to Date using SimpleDateFormat,
        // which uses the default time zone.
        val instant = Instant.ofEpochMilli(releaseDate.time)
        val localDate = instant.atZone(safeSystemDefaultZoneId()).toLocalDate()

        // set time
        var localDateTime = localDate.atTime(showReleaseTime)

        localDateTime = handleHourPastMidnight(showCountry, showNetwork, localDateTime)

        // get a valid datetime in the show time zone, this auto-forwards time if inside DST gap
        var dateTime = localDateTime.atZone(showTimeZone)

        // handle time zone effects on release time for US shows (only if device is set to US zone)
        if (deviceTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA)) {
            dateTime = applyUnitedStatesCorrections(showCountry, deviceTimeZone, dateTime)
        }

        return dateTime.toInstant().toEpochMilli()
    }

    /**
     * Creates the show release time from a [com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows.RELEASE_TIME]
     * encoded value.
     *
     * If the encoded time passed is not from 0 to 2359 or the encoded minute is larger than 59,
     * a sensible default is returned.
     */
    fun getShowReleaseTime(showReleaseTime: Int): LocalTime {
        if (showReleaseTime in 0..2359) {
            val hour = showReleaseTime / 100
            val minute = showReleaseTime - hour * 100
            if (minute <= 59) {
                return LocalTime.of(hour, minute)
            }
        }

        // if no time is available, use a sensible default
        return LocalTime.of(7, 0)
    }

    /**
     * Calculates the current release date time. Adjusts for time zone effects on release time, e.g.
     * delays between time zones (e.g. in the United States) and DST. Adjusts for user-defined
     * offset.
     *
     * @param releaseTime The [com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows.RELEASE_TIME].
     * @return The date is today or on the next day matching the given week day.
     */
    fun getShowReleaseDateTime(
        context: Context, releaseTime: Int,
        weekDay: Int, timeZone: String?, country: String?,
        network: String?
    ): Date {
        // Determine show time zone.
        val showTimeZone = getDateTimeZone(timeZone)

        val time = getShowReleaseTime(releaseTime)
        var dateTime = getShowReleaseDateTime(
            time, weekDay,
            showTimeZone, country, network, Clock.system(showTimeZone)
        )

        dateTime = applyUserOffset(context, dateTime)

        return Date(dateTime.toInstant().toEpochMilli())
    }

    @VisibleForTesting
    fun getShowReleaseDateTime(
        time: LocalTime, weekDay: Int,
        timeZone: ZoneId, country: String?, network: String?,
        clock: Clock
    ): ZonedDateTime {
        // create current date in show time zone, set local show release time
        var localDateTime = LocalDateTime.of(LocalDate.now(clock), time)

        // adjust day of week so datetime is today or within the next week
        // for daily shows (weekDay == 0) just use the current day
        if (weekDay in 1..7) {
            // joda tries to preserve week
            // so if we want a week day earlier in the week, advance by 7 days first
            if (weekDay < localDateTime.dayOfWeek.value) {
                localDateTime = localDateTime.plusWeeks(1)
            }
            localDateTime = localDateTime.with(ChronoField.DAY_OF_WEEK, weekDay.toLong())
        }

        localDateTime = handleHourPastMidnight(country, network, localDateTime)

        // get a valid datetime in the show time zone, this auto-forwards time if inside DST gap
        var dateTime = localDateTime.atZone(timeZone)

        // handle time zone effects on release time for US shows (only if device is set to US zone)
        val localTimeZone = TimeZone.getDefault().id
        if (localTimeZone.startsWith(TIMEZONE_ID_PREFIX_AMERICA)) {
            dateTime = applyUnitedStatesCorrections(country, localTimeZone, dateTime)
        }

        return dateTime
    }

    /**
     * If the release time is within the hour past midnight (0:00 until 0:59) moves the date one day
     * into the future (currently US shows on some networks only).
     *
     * This is due to late night shows being commonly listed as releasing the day before if
     * they air past midnight (e.g. "Monday night at 0:35" actually is Tuesday 0:35).
     *
     * Note: This should never include streaming services like Netflix where midnight is
     * used correctly.
     *
     * Example: https://www.themoviedb.org/tv/62223-the-late-late-show-with-james-corden
     */
    private fun handleHourPastMidnight(
        country: String?, network: String?,
        localDateTime: LocalDateTime
    ): LocalDateTime {
        if (localDateTime.hour == 0 && ISO3166_1_UNITED_STATES == country &&
            (NETWORK_CBS == network || NETWORK_NBC == network)) {
            return localDateTime.plusDays(1)
        }
        return localDateTime
    }

    /**
     * Parses the ISO-8601, such as '2013-08-20T15:16:26.355Z', or TVDB date format representation
     * of a show first release date and outputs the year string in the user's default locale.
     *
     * @return Returns `null` if the given date time is empty.
     */
    fun getShowReleaseYear(releaseDateTime: String?): String? {
        if (releaseDateTime == null || releaseDateTime.isEmpty()) {
            return null
        }

        val instant: Instant = try {
            Instant.parse(releaseDateTime)
        } catch (ignored: DateTimeParseException) {
            // legacy format, or otherwise invalid
            try {
                // try legacy date only parser
                LocalDate.parse(releaseDateTime).atStartOfDay().toInstant(ZoneOffset.UTC)
            } catch (e: DateTimeParseException) {
                return null
            }
        }

        return SimpleDateFormat("yyyy", Locale.getDefault())
            .format(Date(instant.toEpochMilli()))
    }

    @VisibleForTesting
    fun applyUnitedStatesCorrections(
        country: String?,
        localTimeZone: String,
        dateTime: ZonedDateTime
    ): ZonedDateTime {
        // assumed base time zone for US shows by trakt is America/New_York
        // EST UTC−5:00, EDT UTC−4:00

        // east feed (default): simultaneously in Eastern and Central
        // delayed 1 hour in Mountain
        // delayed three hours in Pacific
        // <==>
        // same local time in Eastern + Pacific (e.g. 20:00)
        // same local time in Central + Mountain (e.g. 19:00)

        // not a US show or no correction necessary (getting east feed)
        if (ISO3166_1_UNITED_STATES != country ||
            localTimeZone == TIMEZONE_ID_US_EASTERN ||
            localTimeZone == TIMEZONE_ID_US_EASTERN_DETROIT ||
            localTimeZone == TIMEZONE_ID_US_CENTRAL) {
            return dateTime
        }

        var offset = 0
        when (localTimeZone) {
            TIMEZONE_ID_US_MOUNTAIN -> {
                // MST UTC−7:00, MDT UTC−6:00
                offset += 1
            }
            TIMEZONE_ID_US_ARIZONA -> {
                // is always UTC-07:00, so like Mountain, but no DST
                val dstInEastern = ZoneId.of(TIMEZONE_ID_US_EASTERN).rules
                    .isDaylightSavings(dateTime.toInstant())
                offset += (if (dstInEastern) 2 else 1)
            }
            TIMEZONE_ID_US_PACIFIC -> {
                // PST UTC−8:00 or PDT UTC−7:00
                offset += 3
            }
        }
        return dateTime.plusHours(offset.toLong())
    }

    /**
     * Returns the text representation of the given country code. If the country is not supported,
     * "unknown" will be returned.
     */
    fun getCountry(context: Context, releaseCountry: String?): String {
        if (releaseCountry == null || releaseCountry.isEmpty()) {
            return context.getString(R.string.unknown)
        }
        val country = Locale("", releaseCountry).getDisplayCountry(Locale.getDefault())
        return if (TextUtils.isEmpty(country)) {
            context.getString(R.string.unknown)
        } else country
    }

    /**
     * Returns the current system time with inverted user-set offsets applied.
     */
    @JvmStatic
    fun getCurrentTime(context: Context): Long {
        val calendar = Calendar.getInstance()
        applyUserOffsetInverted(context, calendar)
        return calendar.timeInMillis
    }

    /**
     * Formats to the week day abbreviation (e.g. "Mon") as defined by the devices locale.
     */
    fun formatToLocalDay(dateTime: Date): String =
        SimpleDateFormat("E", Locale.getDefault()).format(dateTime)

    /**
     * Formats to the week day abbreviation (e.g. "Mon") as defined by the devices locale. If the
     * given weekDay is 0, returns the local version of "Daily".
     */
    fun formatToLocalDayOrDaily(context: Context, dateTime: Date, weekDay: Int): String {
        return if (weekDay == RELEASE_WEEKDAY_DAILY) {
            context.getString(R.string.daily)
        } else formatToLocalDay(dateTime)
    }

    /**
     * Formats to absolute time format (e.g. "08:00 PM") as defined by the devices locale.
     */
    fun formatToLocalTime(context: Context, dateTime: Date): String =
        DateFormat.getTimeFormat(context).format(dateTime)

    /**
     * Formats to relative time in relation to the current system time (e.g. "in 12 min") as defined
     * by the devices locale. If the time difference is lower than a minute, returns the localized
     * equivalent of "now".
     */
    fun formatToLocalRelativeTime(context: Context, dateTime: Date): String {
        val now = System.currentTimeMillis()
        val dateTimeInstant = dateTime.time

        // if we are below the resolution of getRelativeTimeSpanString, return "now"
        return if (abs(now - dateTimeInstant) < DateUtils.MINUTE_IN_MILLIS) {
            context.getString(R.string.now)
        } else DateUtils
            .getRelativeTimeSpanString(
                dateTimeInstant, now, DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL
            ).toString()
    }

    /**
     * Formats to day and relative time in relation to the current system time
     * (e.g. "Mon in 3 days") as defined by the devices locale. If the time is today,
     * returns the local equivalent for "today".
     */
    fun formatToLocalDayAndRelativeTime(context: Context, dateTime: Date): String {
        val dayAndTime = StringBuilder()

        // day abbreviation, e.g. "Mon"
        dayAndTime.append(formatToLocalDay(dateTime))
        dayAndTime.append(" ")

        // relative time to dateTime, "today" or e.g. "3 days ago"
        if (DateUtils.isToday(dateTime.time)) {
            // show 'today' instead of '0 days ago'
            dayAndTime.append(context.getString(R.string.today))
        } else {
            dayAndTime.append(
                DateUtils
                    .getRelativeTimeSpanString(
                        dateTime.time,
                        System.currentTimeMillis(),
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL
                    )
            )
        }
        return dayAndTime.toString()
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
    fun formatToLocalDayAndRelativeWeek(context: Context, thenDate: Date): String {
        if (DateUtils.isToday(thenDate.time)) {
            return context.getString(R.string.released_today)
        }

        // day abbreviation, e.g. "Mon"
        val localDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayAndTime = StringBuilder(localDayFormat.format(thenDate))

        val then = Instant.ofEpochMilli(thenDate.time)
        val thenZoned = ZonedDateTime.ofInstant(then, safeSystemDefaultZoneId())

        // append 'in x wk.' if date is not within a week, for example if today is Thursday:
        // - append for previous Thursday and earlier,
        // - and for next Thursday and later
        val weekDiff = LocalDate.now().until(thenZoned.toLocalDate(), ChronoUnit.WEEKS)
        if (weekDiff != 0L) {
            // use weekDiff to calc "now" for relative time string to be daylight saving safe
            // Android L_MR1 and below do not support 'in x wks.', display date instead
            // so make sure that time is the actual time
            val now = then.minus(weekDiff * 7, ChronoUnit.DAYS)

            dayAndTime.append(" ")
            if (abs(weekDiff) <= 6) {
                dayAndTime.append(
                    DateUtils
                        .getRelativeTimeSpanString(
                            then.toEpochMilli(), now.toEpochMilli(),
                            DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
                        )
                )
            } else {
                // for everything further away from now display date instead
                dayAndTime.append(formatToLocalDateShort(context, thenDate))
            }
        }
        return dayAndTime.toString()
    }

    /**
     * Formats to a date like "October 31", or if the date is not in the current year
     * "October 31, 2010".
     *
     * @see formatToLocalDateShort
     */
    fun formatToLocalDate(context: Context?, dateTime: Date): String =
        DateUtils.formatDateTime(context, dateTime.time, DateUtils.FORMAT_SHOW_DATE)

    /**
     * Formats to a date like "Oct 31", or if the date is not in the current year "Oct 31, 2010".
     *
     * @see formatToLocalDate
     */
    fun formatToLocalDateShort(context: Context, dateTime: Date): String =
        DateUtils.formatDateTime(
            context,
            dateTime.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
        )

    /**
     * Formats to a date, time zone and week day (e.g. "2014/02/04 CET (Mon)") as defined by the
     * devices locale. If the date time is today, uses the local equivalent of "today" instead of a
     * week day.
     */
    fun formatToLocalDateAndDay(context: Context, dateTime: Date): String {
        val date = StringBuilder()

        // date, e.g. "2014/05/31"
        date.append(DateFormat.getDateFormat(context).format(dateTime))
        date.append(" ")

        // device time zone, e.g. "CEST"
        val timeZone = TimeZone.getDefault()
        date.append(
            timeZone.getDisplayName(
                timeZone.inDaylightTime(dateTime),
                TimeZone.SHORT,
                Locale.getDefault()
            )
        )

        // Show 'today' instead of e.g. 'Mon'
        val day: String = if (DateUtils.isToday(dateTime.time)) {
            context.getString(R.string.today)
        } else {
            formatToLocalDay(dateTime)
        }
        return context.getString(R.string.format_date_and_day, date.toString(), day)
    }

    /**
     * Returns a date time equal to the given date time plus the user-defined offset.
     */
    private fun applyUserOffset(context: Context, dateTime: ZonedDateTime): ZonedDateTime {
        val offset = DisplaySettings.getShowsTimeOffset(context)
        if (offset != 0) {
            return dateTime.plusHours(offset.toLong())
        }
        return dateTime
    }

    /**
     * Takes a millisecond date time instant and adds the user-defined offset.
     *
     * Typically required for episode date times stored in the database before formatting them
     * for display.
     */
    @JvmStatic
    fun applyUserOffset(context: Context, releaseInstant: Long): Date {
        // using Android calendar to avoid joda-time lock-up with time zone access
        val dateTime = Calendar.getInstance()
        dateTime.timeInMillis = releaseInstant

        val offset = DisplaySettings.getShowsTimeOffset(context)
        if (offset != 0) {
            dateTime.add(Calendar.HOUR_OF_DAY, offset)
        }
        return dateTime.time
    }

    private fun applyUserOffsetInverted(context: Context, calendar: Calendar) {
        var offset = DisplaySettings.getShowsTimeOffset(context)

        // invert
        offset = -offset

        if (offset != 0) {
            calendar.add(Calendar.HOUR_OF_DAY, offset)
        }
    }

}