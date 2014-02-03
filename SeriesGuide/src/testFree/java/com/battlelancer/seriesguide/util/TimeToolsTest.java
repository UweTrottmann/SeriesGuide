package com.battlelancer.seriesguide.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

public class TimeToolsTest {

    private static final SimpleDateFormat TIME_FORMAT_CUSTOM_TIMEZONE = new SimpleDateFormat(
            "hh:mm aa");

    static {
        TIME_FORMAT_CUSTOM_TIMEZONE.setTimeZone(TimeZone.getTimeZone(TimeTools.TIMEZONE_ID_CUSTOM));
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

}