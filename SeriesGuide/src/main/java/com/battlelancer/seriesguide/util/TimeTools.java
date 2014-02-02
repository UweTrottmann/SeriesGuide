/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helps with converting timestamps used by TVDb and other services.
 */
public class TimeTools {

    public static final String TIMEZONE_ID_PST = "GMT-08:00";

    private static final SimpleDateFormat TIME_FORMAT_TVDB_AMPM = new SimpleDateFormat("h:mm aa",
            Locale.US);

    private static final SimpleDateFormat TIME_FORMAT_TVDB_AMPM_NOSPACE = new SimpleDateFormat(
            "h:mmaa", Locale.US);

    private static final SimpleDateFormat TIME_FORMAT_TVDB_AMPM_HOUR_ONLY = new SimpleDateFormat(
            "h aa", Locale.US);

    private static final SimpleDateFormat TIME_FORMAT_TVDB_24 = new SimpleDateFormat("H:mm",
            Locale.US);

    static {
        // assume all TVDb time strings are in PST
        TIME_FORMAT_TVDB_AMPM.setTimeZone(TimeZone.getTimeZone(TIMEZONE_ID_PST));
        TIME_FORMAT_TVDB_AMPM_NOSPACE.setTimeZone(TimeZone.getTimeZone(TIMEZONE_ID_PST));
        TIME_FORMAT_TVDB_AMPM_HOUR_ONLY.setTimeZone(TimeZone.getTimeZone(TIMEZONE_ID_PST));
        TIME_FORMAT_TVDB_24.setTimeZone(TimeZone.getTimeZone(TIMEZONE_ID_PST));
    }

    /**
     * Parse a TVDb air time to a UTC ms value. The air time from TVDb is assumed to be in Pacific
     * Standard Time (always without daylight saving).
     *
     * @return -1 if no conversion was possible, a UTC millisecond value otherwise.
     */
    public static long parseTimeToMilliseconds(String tvdbTimeString) {
        // try parsing with different formats, starting with the most likely
        Date time = null;
        if (tvdbTimeString != null && tvdbTimeString.length() != 0) {
            try {
                time = TIME_FORMAT_TVDB_AMPM.parse(tvdbTimeString);
            } catch (ParseException e) {
                try {
                    time = TIME_FORMAT_TVDB_AMPM_NOSPACE.parse(tvdbTimeString);
                } catch (ParseException e1) {
                    try {
                        time = TIME_FORMAT_TVDB_AMPM_HOUR_ONLY.parse(tvdbTimeString);
                    } catch (ParseException e2) {
                        try {
                            time = TIME_FORMAT_TVDB_24.parse(tvdbTimeString);
                        } catch (ParseException e3) {
                            // string may be wrongly formatted
                            time = null;
                        }
                    }
                }
            }
        }

        if (time != null) {
            return time.getTime();
        } else {
            // times resolution is at most in minutes, so -1 (ms) can never exist
            return -1;
        }
    }
}
