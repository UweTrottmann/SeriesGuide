package com.battlelancer.seriesguide.test;

import com.battlelancer.seriesguide.util.TimeTools;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeToolsTest extends TestCase {

    public static Test suite() {
        return new TestSuite(TimeToolsTest.class);
    }

    private static final SimpleDateFormat TIME_FORMAT_CUSTOM_TIMEZONE = new SimpleDateFormat(
            "hh:mm aa");

    static {
        TIME_FORMAT_CUSTOM_TIMEZONE.setTimeZone(TimeZone.getTimeZone(TimeTools.TIMEZONE_ID_CUSTOM));
    }

    public void test_parseEpisodeReleaseTime() {
        long showReleaseTime = new DateTime(2000, 1, 1, 20, 0, DateTimeZone.UTC).getMillis();
        long episodeReleaseTime = TimeTools
                .parseEpisodeReleaseTime("2013-05-31", showReleaseTime, "us");
        usTimeZoneWarning();
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370055600000L);
    }

    public void test_parseEpisodeReleaseTime_Country() {
        long showReleaseTime = new DateTime(2000, 1, 1, 20, 0, DateTimeZone.UTC).getMillis();
        long episodeReleaseTime = TimeTools
                .parseEpisodeReleaseTime("2013-05-31", showReleaseTime, "de");
        usTimeZoneWarning();
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370023200000L);
    }

    public void test_parseEpisodeReleaseTime_HourPastMidnight() {
        long showReleaseTime = new DateTime(2000, 1, 1, 0, 35, DateTimeZone.UTC).getMillis();
        long episodeReleaseTime = TimeTools
                .parseEpisodeReleaseTime("2013-05-31", showReleaseTime, "us");
        usTimeZoneWarning();
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370072100000L);
    }

    private void usTimeZoneWarning() {
        System.out.println("WARNING: This test WILL fail on any US time zone not US Pacific");
    }

}