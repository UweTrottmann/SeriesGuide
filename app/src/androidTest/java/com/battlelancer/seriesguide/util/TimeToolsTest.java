package com.battlelancer.seriesguide.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.Date;
import org.junit.Test;
import org.threeten.bp.Clock;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

/**
 * Note: ensure to test on Android as local JVM handles time and dates differently.
 */
public class TimeToolsTest {

    public static final String AMERICA_NEW_YORK = "America/New_York";
    public static final String AMERICA_LOS_ANGELES = "America/Los_Angeles";
    public static final String EUROPE_BERLIN = "Europe/Berlin";
    public static final String GERMANY = "de";
    public static final String UNITED_STATES = "us";

    @Test
    public void test_getShowReleaseYear() {
        // new ISO 8601 format
        String year1 = TimeTools.getShowReleaseYear("2017-01-31T15:16:26.355Z");
        // legacy TVDB ISO date format
        String year2 = TimeTools.getShowReleaseYear("2017-01-31");
        assertThat(year1).isEqualTo("2017");
        assertThat(year1).isEqualTo(year2);
    }

    private Date getDayAsDate(int year, int month, int dayOfMonth) {
        // tmdb-java parses date string to Date using SimpleDateFormat,
        // which uses the default time zone.
        Instant instant = LocalDate.of(year, month, dayOfMonth)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant();
        return new Date(instant.toEpochMilli());
    }

    @Test
    public void test_parseEpisodeReleaseTime() {
        // ensure a US show has its local release time correctly converted to UTC time
        // (we can be sure that in May there is always DST in effect in America/New_York
        // so this test will likely not break if DST rules change)
        ZoneId showTimeZone = ZoneId.of(AMERICA_NEW_YORK);
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(
                showTimeZone,
                getDayAsDate(2013, 5, 31),
                LocalTime.of(20, 0), // 20:00
                UNITED_STATES,
                null,
                AMERICA_LOS_ANGELES);
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370055600000L);
    }

    @Test
    public void test_parseEpisodeReleaseTime_Country() {
        // ensure a German show has its local release time correctly converted to UTC time
        // (we can be sure that in May there is always DST in effect in Europe/Berlin
        // so this test will likely not break if DST rules change)
        ZoneId showTimeZone = ZoneId.of(EUROPE_BERLIN);
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(
                showTimeZone,
                getDayAsDate(2013, 5, 31),
                LocalTime.of(20, 0), // 20:00
                GERMANY,
                null,
                AMERICA_LOS_ANGELES);
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370023200000L);
    }

    @Test
    public void test_parseEpisodeReleaseTime_HourPastMidnight() {
        // ensure episodes releasing in the hour past midnight are moved to the next day
        // e.g. if 00:35, the episode date is typically (wrongly) that of the previous day
        // this is common for late night shows, e.g. "Monday night" is technically "early Tuesday"
        // ONLY for specific networks
        ZoneId showTimeZone = ZoneId.of(AMERICA_NEW_YORK);
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(
                showTimeZone,
                getDayAsDate(2013, 5, 31),
                LocalTime.of(0, 35), // 00:35
                UNITED_STATES,
                "CBS",
                AMERICA_LOS_ANGELES);
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370072100000L);
    }

    @Test
    public void test_parseEpisodeReleaseTime_NoHourPastMidnight() {
        // ensure episodes releasing in the hour past midnight are NOT moved to the next day
        // if it is a Netflix show
        ZoneId showTimeZone = ZoneId.of(AMERICA_NEW_YORK);
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(
                showTimeZone,
                getDayAsDate(2013, 6, 1), // +one day here
                LocalTime.of(0, 35), // 00:35
                UNITED_STATES,
                "Netflix",
                AMERICA_LOS_ANGELES);
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370072100000L);
    }

    @Test
    public void test_getShowReleaseDateTime_dstGap() {
        // using begin of daylight saving time in Europe/Berlin on 2017-03-26
        // clock moves forward at 2:00 by 1 hour
        ZoneId zoneIdBerlin = ZoneId.of(EUROPE_BERLIN);
        Instant instantDayOfDstStart = ZonedDateTime.of(LocalDateTime.of(2017, 3, 26, 1, 0),
                zoneIdBerlin).toInstant();
        Clock fixedClock = Clock.fixed(instantDayOfDstStart, zoneIdBerlin);

        // put show release exactly inside daylight saving gap (02:00-03:00)
        ZonedDateTime dateTime = TimeTools.getShowReleaseDateTime(
                LocalTime.of(2, 30), DayOfWeek.SUNDAY.getValue(),
                zoneIdBerlin, GERMANY, "Some Network",
                fixedClock);

        // time should be "fixed" by moving an hour forward
        assertThat(dateTime.toLocalTime()).isEqualTo(LocalTime.of(3, 30));
    }

    @Test
    public void test_applyUnitedStatesCorrections() {
        // assume a US show releasing in Eastern time at 20:00
        LocalTime localTimeOf2000 = LocalTime.of(20, 0);
        ZoneId zoneIdUsEastern = ZoneId.of(TimeTools.TIMEZONE_ID_US_EASTERN);
        ZonedDateTime showDateTime = ZonedDateTime.of(LocalDate.of(2017, 1, 31), localTimeOf2000,
                zoneIdUsEastern);

        // Same local time as US Eastern
        applyAndAssertFor(showDateTime, localTimeOf2000, TimeTools.TIMEZONE_ID_US_EASTERN);
        applyAndAssertFor(showDateTime, localTimeOf2000, TimeTools.TIMEZONE_ID_US_EASTERN_DETROIT);
        applyAndAssertFor(showDateTime, localTimeOf2000, TimeTools.TIMEZONE_ID_US_PACIFIC);

        // One hour earlier local time than US Eastern
        LocalTime localTimeOf1900 = LocalTime.of(19, 0);
        applyAndAssertFor(showDateTime, localTimeOf1900, TimeTools.TIMEZONE_ID_US_CENTRAL);
        applyAndAssertFor(showDateTime, localTimeOf1900, TimeTools.TIMEZONE_ID_US_MOUNTAIN);

        // Same during winter...
        applyAndAssertFor(showDateTime, localTimeOf1900, TimeTools.TIMEZONE_ID_US_ARIZONA);
        // ...but observes no daylight saving
        LocalDate dateDuringDaylightSaving = LocalDate.of(2017, 5, 31);
        ZonedDateTime showTimeInDst = ZonedDateTime.of(dateDuringDaylightSaving, localTimeOf2000,
                zoneIdUsEastern);
        applyAndAssertFor(showTimeInDst, localTimeOf1900, TimeTools.TIMEZONE_ID_US_ARIZONA);
    }

    @Test
    public void test_applyUnitedStatesCorrections_nonUs() {
        // assume a non-US show releasing at 20:00 local Berlin time (UTC+01:00)
        LocalTime localTimeOf2000 = LocalTime.of(20, 0);
        ZonedDateTime showDateTime = ZonedDateTime.of(LocalDate.of(2017, 1, 31), localTimeOf2000,
                ZoneId.of(EUROPE_BERLIN));

        // difference to US Central (UTC-06:00): -7 hours
        applyAndAssertFor(showDateTime, LocalTime.of(13, 0), TimeTools.TIMEZONE_ID_US_CENTRAL);
    }

    private void applyAndAssertFor(ZonedDateTime showDateTime, LocalTime localTimeExpected,
            String timeZone) {
        ZonedDateTime showTimeCorrected = TimeTools.applyUnitedStatesCorrections(
                TimeTools.ISO3166_1_UNITED_STATES, timeZone, showDateTime);
        assertWithMessage("Check %s", timeZone)
                .that(showTimeCorrected.withZoneSameInstant(ZoneId.of(timeZone)).toLocalTime())
                .isEqualTo(localTimeExpected);
    }
}