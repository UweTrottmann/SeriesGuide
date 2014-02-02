package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;

import org.junit.Test;

import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

public class TimeToolsTest {

    private static final SimpleDateFormat TIME_FORMAT_PST = new SimpleDateFormat("hh:mm aa");

    private static final SimpleDateFormat DATE_TIME_FORMAT_PST = new SimpleDateFormat(
            "yyyy-MM-dd hh:mm aa");

    static {
        TIME_FORMAT_PST.setTimeZone(TimeZone.getTimeZone(TimeTools.TIMEZONE_ID_PST));
    }

    @Test
    public void test_parseTimeToMillisecondsFormats() {
        parseAndCompare("8:00PM", "08:00 PM");
        parseAndCompare("08:00PM", "08:00 PM");
        parseAndCompare("8:00 PM", "08:00 PM");
        parseAndCompare("08:00 PM", "08:00 PM");
        parseAndCompare("8 PM", "08:00 PM");
        parseAndCompare("8:00", "08:00 AM");
        parseAndCompare("20:00", "08:00 PM");
    }

    private void parseAndCompare(String time, String timeResult) {
        long timeMs = TimeTools.parseTimeToMilliseconds(time);
        String timeString = TIME_FORMAT_PST.format(new Date(timeMs));

        System.out.println(
                time + " is " + timeString + " or " + timeMs + "ms or " + new Date(timeMs));

        assertThat(timeString).isEqualTo(timeResult);
    }

    @Test
    public void test_parseTimeToMillisecondsMidnight() {
        long timeAfterLastMidnight = TimeTools.parseTimeToMilliseconds("1:00AM");
        long timeBeforeMidnight = TimeTools.parseTimeToMilliseconds("11:55PM");
        long timeMidnight = TimeTools.parseTimeToMilliseconds("12:00AM");

        // ensure 12:00 AM - 12:59 AM is 24 hours later than it should be by law
        Date afterLastMidnight = new Date(timeAfterLastMidnight);
        Date beforeMidnight = new Date(timeBeforeMidnight);
        Date midnight = new Date(timeMidnight);

        System.out.println("After LAST midnight:     "
                + DATE_TIME_FORMAT_PST.format(afterLastMidnight)
                + " (" + timeAfterLastMidnight + ")");
        System.out.println("Before CURRENT midnight: "
                + DATE_TIME_FORMAT_PST.format(beforeMidnight)
                + " (" + timeBeforeMidnight + ")");
        System.out.println("CURRENT midnight:        "
                + DATE_TIME_FORMAT_PST.format(midnight)
                + " (" + timeMidnight + ")");

        assertThat(midnight).isAfter(afterLastMidnight);
        assertThat(midnight).isAfter(beforeMidnight);
    }

}