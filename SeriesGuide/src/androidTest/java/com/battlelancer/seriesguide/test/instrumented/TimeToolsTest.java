package com.battlelancer.seriesguide.test.instrumented;

import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;
import org.junit.Test;
import org.threeten.bp.Clock;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Note: this test has a mirrored local unit test!
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
        assertThat(year1).isEqualToIgnoringCase("2017");
        assertThat(year1).isEqualToIgnoringCase(year2);
    }

    @Test
    public void test_parseEpisodeReleaseTime() {
        // ensure a US show has its local release time correctly converted to UTC time
        // (we can be sure that in May there is always DST in effect in America/New_York
        // so this test will likely not break if DST rules change)
        ZoneId showTimeZone = ZoneId.of(AMERICA_NEW_YORK);
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(null,
                showTimeZone,
                "2013-05-31",
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
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(null,
                showTimeZone,
                "2013-05-31",
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
        ZoneId showTimeZone = ZoneId.of(AMERICA_NEW_YORK);
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(null,
                showTimeZone,
                "2013-05-31",
                LocalTime.of(0, 35), // 00:35
                UNITED_STATES,
                null,
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
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(null,
                showTimeZone,
                "2013-06-01", // +one day here
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
}