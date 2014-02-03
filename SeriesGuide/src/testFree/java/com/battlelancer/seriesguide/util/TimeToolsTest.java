package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.util.TimeTools;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

// TODO Can't run Robolectric until it supports Android 4.4
//@RunWith(RobolectricTestRunner.class)
public class TimeToolsTest {

    private static final SimpleDateFormat TIME_FORMAT_CUSTOM_TIMEZONE = new SimpleDateFormat(
            "hh:mm aa");

    static {
        TIME_FORMAT_CUSTOM_TIMEZONE.setTimeZone(TimeZone.getTimeZone(TimeTools.TIMEZONE_ID_CUSTOM));
    }

    @Test
    public void test_parseEpisodeReleaseTime() {
        long showReleaseTime = TimeTools.parseTimeToMilliseconds("8:00pm");
        long episodeReleaseTime = TimeTools
                .parseEpisodeReleaseTime("2013-05-31", showReleaseTime, "United States");
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370055600000L);
    }

    @Test
    public void test_parseEpisodeReleaseTime_HourPastMidnight() {
        long showReleaseTime = TimeTools.parseTimeToMilliseconds("12:35am");
        long episodeReleaseTime = TimeTools
                .parseEpisodeReleaseTime("2013-05-31", showReleaseTime, "United States");
        System.out.println(
                "Release time: " + episodeReleaseTime + " " + new Date(episodeReleaseTime));
        assertThat(episodeReleaseTime).isEqualTo(1370072100000L);
    }

    @Test
    public void test_parseTimeToMillisecondsFormats() {
        parseAndCompare("8:00pm", "08:00 PM");
        parseAndCompare("8:00am", "08:00 AM");

        // test some variations to be sure
        parseAndCompare("8:00PM", "08:00 PM");
        parseAndCompare("08:00pm", "08:00 PM");
        parseAndCompare("08:00PM", "08:00 PM");
    }

    private void parseAndCompare(String time, String timeResult) {
        long timeMs = TimeTools.parseTimeToMilliseconds(time);
        String timeString = TIME_FORMAT_CUSTOM_TIMEZONE.format(new Date(timeMs));

        System.out.println(
                time + " is " + timeString + " or " + timeMs + "ms or " + new Date(timeMs));

        assertThat(timeString).isEqualTo(timeResult);
    }

//    @Test
//    public void test_formatShowReleaseTimeAndDay() {
//        long releaseTime = TimeTools.parseTimeToMilliseconds("12:35am");
//        Context context = Robolectric.getShadowApplication().getApplicationContext();
//        String[] timeAndDay = TimeTools
//                .formatShowReleaseTimeAndDay(context, releaseTime, "United States", "Monday");
//        System.out.println("Time: " + timeAndDay[0] + "and day: " + timeAndDay[1]);
//    }

}