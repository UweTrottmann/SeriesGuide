package com.battlelancer.seriesguide.test;

import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeToolsTest extends TestCase {

    public static final String AMERICA_NEW_YORK = "America/New_York";
    public static final String AMERICA_LOS_ANGELES = "America/Los_Angeles";
    public static final String EUROPE_BERLIN = "Europe/Berlin";
    public static final String GERMANY = "de";
    public static final String UNITED_STATES = "us";

    public static Test suite() {
        return new TestSuite(TimeToolsTest.class);
    }

    public void test_parseEpisodeReleaseTime() {
        // ensure a US show has its local release time correctly converted to UTC time
        // (we can be sure that in May there is always DST in effect in America/New_York
        // so this test will likely not break if DST rules change)
        DateTimeZone showTimeZone = DateTimeZone.forID(AMERICA_NEW_YORK);
        String deviceTimeZone = AMERICA_LOS_ANGELES;
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(
                showTimeZone,
                "2013-05-31",
                new LocalTime(20, 0), // 20:00
                UNITED_STATES,
                deviceTimeZone);
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370055600000L);
    }

    public void test_parseEpisodeReleaseTime_Country() {
        // ensure a German show has its local release time correctly converted to UTC time
        // (we can be sure that in May there is always DST in effect in Europe/Berlin
        // so this test will likely not break if DST rules change)
        DateTimeZone showTimeZone = DateTimeZone.forID(EUROPE_BERLIN);
        String deviceTimeZone = AMERICA_LOS_ANGELES;
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(
                showTimeZone,
                "2013-05-31",
                new LocalTime(20, 0), // 20:00
                GERMANY,
                deviceTimeZone);
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370023200000L);
    }

    public void test_parseEpisodeReleaseTime_HourPastMidnight() {
        // ensure episodes releasing in the hour past midnight are moved to the next day
        // e.g. if 00:35, the episode date is typically (wrongly) that of the previous day
        // this is common for late night shows, e.g. "Monday night" is technically "early Tuesday"
        DateTimeZone showTimeZone = DateTimeZone.forID(AMERICA_NEW_YORK);
        String deviceTimeZone = AMERICA_LOS_ANGELES;
        long episodeReleaseTime = TimeTools.parseEpisodeReleaseDate(
                showTimeZone,
                "2013-05-31",
                new LocalTime(0, 35), // 00:35
                UNITED_STATES,
                deviceTimeZone);
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370072100000L);
    }
}