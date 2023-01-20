package com.battlelancer.seriesguide.util

import com.battlelancer.seriesguide.util.TimeTools.applyUnitedStatesCorrections
import com.battlelancer.seriesguide.util.TimeTools.getDateTimeZone
import com.battlelancer.seriesguide.util.TimeTools.getShowReleaseDateTime
import com.battlelancer.seriesguide.util.TimeTools.getShowReleaseYear
import com.battlelancer.seriesguide.util.TimeTools.parseEpisodeReleaseDate
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.Clock
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.Date

/**
 * Note: ensure to test on Android as local JVM handles time and dates differently.
 */
class TimeToolsTest {

    @Test
    fun test_getShowReleaseYear() {
        // new ISO 8601 format
        val year1 = getShowReleaseYear("2017-01-31T15:16:26.355Z")
        // legacy TVDB ISO date format
        val year2 = getShowReleaseYear("2017-01-31")
        assertThat(year1).isEqualTo("2017")
        assertThat(year1).isEqualTo(year2)
    }

    private fun getDayAsDate(year: Int, month: Int, dayOfMonth: Int): Date {
        // tmdb-java parses date string to Date using SimpleDateFormat,
        // which uses the default time zone.
        val instant = LocalDate.of(year, month, dayOfMonth)
            .atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
        return Date(instant.toEpochMilli())
    }

    @Test
    fun test_parseEpisodeReleaseTime() {
        // ensure a US show has its local release time correctly converted to UTC time
        // (we can be sure that in May there is always DST in effect in America/New_York
        // so this test will likely not break if DST rules change)
        val showTimeZone = ZoneId.of(AMERICA_NEW_YORK)
        val episodeReleaseTime = parseEpisodeReleaseDate(
            showTimeZone,
            getDayAsDate(2013, 5, 31),
            LocalTime.of(20, 0),  // 20:00
            UNITED_STATES,
            null,
            AMERICA_LOS_ANGELES
        )
        println(
            "Release time: " + episodeReleaseTime + " " + Date(episodeReleaseTime)
        )
        assertThat(episodeReleaseTime).isEqualTo(1370055600000L)
    }

    @Test
    fun test_parseEpisodeReleaseTime_Country() {
        // ensure a German show has its local release time correctly converted to UTC time
        // (we can be sure that in May there is always DST in effect in Europe/Berlin
        // so this test will likely not break if DST rules change)
        val showTimeZone = ZoneId.of(EUROPE_BERLIN)
        val episodeReleaseTime = parseEpisodeReleaseDate(
            showTimeZone,
            getDayAsDate(2013, 5, 31),
            LocalTime.of(20, 0),  // 20:00
            GERMANY,
            null,
            AMERICA_LOS_ANGELES
        )
        println(
            "Release time: " + episodeReleaseTime + " " + Date(episodeReleaseTime)
        )
        assertThat(episodeReleaseTime).isEqualTo(1370023200000L)
    }

    @Test
    fun test_parseEpisodeReleaseTime_HourPastMidnight() {
        // ensure episodes releasing in the hour past midnight are moved to the next day
        // e.g. if 00:35, the episode date is typically (wrongly) that of the previous day
        // this is common for late night shows, e.g. "Monday night" is technically "early Tuesday"
        // ONLY for specific networks
        val showTimeZone = ZoneId.of(AMERICA_NEW_YORK)
        val episodeReleaseTime = parseEpisodeReleaseDate(
            showTimeZone,
            getDayAsDate(2013, 5, 31),
            LocalTime.of(0, 35),  // 00:35
            UNITED_STATES,
            "CBS",
            AMERICA_LOS_ANGELES
        )
        println(
            "Release time: " + episodeReleaseTime + " " + Date(episodeReleaseTime)
        )
        assertThat(episodeReleaseTime).isEqualTo(1370072100000L)
    }

    @Test
    fun test_parseEpisodeReleaseTime_NoHourPastMidnight() {
        // ensure episodes releasing in the hour past midnight are NOT moved to the next day
        // if it is a Netflix show
        val showTimeZone = ZoneId.of(AMERICA_NEW_YORK)
        val episodeReleaseTime = parseEpisodeReleaseDate(
            showTimeZone,
            getDayAsDate(2013, 6, 1),  // +one day here
            LocalTime.of(0, 35),  // 00:35
            UNITED_STATES,
            "Netflix",
            AMERICA_LOS_ANGELES
        )
        println(
            "Release time: " + episodeReleaseTime + " " + Date(episodeReleaseTime)
        )
        assertThat(episodeReleaseTime).isEqualTo(1370072100000L)
    }

    @Test
    fun test_getShowReleaseDateTime_dstGap() {
        // using begin of daylight saving time in Europe/Berlin on 2017-03-26
        // clock moves forward at 2:00 by 1 hour
        val zoneIdBerlin = ZoneId.of(EUROPE_BERLIN)
        val instantDayOfDstStart = ZonedDateTime.of(
            LocalDateTime.of(2017, 3, 26, 1, 0),
            zoneIdBerlin
        ).toInstant()
        val fixedClock = Clock.fixed(instantDayOfDstStart, zoneIdBerlin)

        // put show release exactly inside daylight saving gap (02:00-03:00)
        val dateTime = getShowReleaseDateTime(
            LocalTime.of(2, 30), DayOfWeek.SUNDAY.value,
            zoneIdBerlin, GERMANY, "Some Network",
            fixedClock
        )

        // time should be "fixed" by moving an hour forward
        assertThat(dateTime.toLocalTime()).isEqualTo(LocalTime.of(3, 30))
    }

    @Test
    fun test_applyUnitedStatesCorrections() {
        // assume a US show releasing in Eastern time at 20:00
        val localTimeOf2000 = LocalTime.of(20, 0)
        val zoneIdUsEastern = ZoneId.of(TimeTools.TIMEZONE_ID_US_EASTERN)
        val showDateTime = ZonedDateTime.of(
            LocalDate.of(2017, 1, 31), localTimeOf2000,
            zoneIdUsEastern
        )

        // Same local time as US Eastern
        applyAndAssertFor(showDateTime, localTimeOf2000, TimeTools.TIMEZONE_ID_US_EASTERN)
        applyAndAssertFor(showDateTime, localTimeOf2000, TimeTools.TIMEZONE_ID_US_EASTERN_DETROIT)
        applyAndAssertFor(showDateTime, localTimeOf2000, TimeTools.TIMEZONE_ID_US_PACIFIC)

        // One hour earlier local time than US Eastern
        val localTimeOf1900 = LocalTime.of(19, 0)
        applyAndAssertFor(showDateTime, localTimeOf1900, TimeTools.TIMEZONE_ID_US_CENTRAL)
        applyAndAssertFor(showDateTime, localTimeOf1900, TimeTools.TIMEZONE_ID_US_MOUNTAIN)

        // Same during winter...
        applyAndAssertFor(showDateTime, localTimeOf1900, TimeTools.TIMEZONE_ID_US_ARIZONA)
        // ...but observes no daylight saving
        val dateDuringDaylightSaving = LocalDate.of(2017, 5, 31)
        val showTimeInDst = ZonedDateTime.of(
            dateDuringDaylightSaving, localTimeOf2000,
            zoneIdUsEastern
        )
        applyAndAssertFor(showTimeInDst, localTimeOf1900, TimeTools.TIMEZONE_ID_US_ARIZONA)
    }

    @Test
    fun test_applyUnitedStatesCorrections_nonUs() {
        // assume a non-US show releasing at 20:00 local Berlin time (UTC+01:00)
        val localTimeOf2000 = LocalTime.of(20, 0)
        val showDateTime = ZonedDateTime.of(
            LocalDate.of(2017, 1, 31), localTimeOf2000,
            ZoneId.of(EUROPE_BERLIN)
        )

        // difference to US Central (UTC-06:00): -7 hours
        applyAndAssertFor(showDateTime, LocalTime.of(13, 0), TimeTools.TIMEZONE_ID_US_CENTRAL)
    }

    private fun applyAndAssertFor(
        showDateTime: ZonedDateTime, localTimeExpected: LocalTime,
        timeZone: String
    ) {
        val showTimeCorrected = applyUnitedStatesCorrections(
            TimeTools.ISO3166_1_UNITED_STATES, timeZone, showDateTime
        )
        Truth.assertWithMessage("Check %s", timeZone)
            .that(showTimeCorrected.withZoneSameInstant(ZoneId.of(timeZone)).toLocalTime())
            .isEqualTo(localTimeExpected)
    }

    @Test
    fun test_getDateTimeZone() {
        val defaultTimeZone = getDateTimeZone(null)
        Truth.assertWithMessage("Emulators by default set to GMT")
            .that(defaultTimeZone.toString()).isEqualTo("GMT")
        // Test some common time zones to make sure threetenbp works.
        assertThat(getDateTimeZone("Europe/Berlin").toString())
            .isEqualTo("Europe/Berlin")
        assertThat(getDateTimeZone(TimeTools.TIMEZONE_ID_US_EASTERN).toString())
            .isEqualTo(TimeTools.TIMEZONE_ID_US_EASTERN)
    }

    companion object {
        const val AMERICA_NEW_YORK = "America/New_York"
        const val AMERICA_LOS_ANGELES = "America/Los_Angeles"
        const val EUROPE_BERLIN = "Europe/Berlin"
        const val GERMANY = "de"
        const val UNITED_STATES = "us"
    }
}