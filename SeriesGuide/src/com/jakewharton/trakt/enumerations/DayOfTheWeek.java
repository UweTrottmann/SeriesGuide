package com.jakewharton.trakt.enumerations;

import java.util.HashMap;
import java.util.Map;
import com.jakewharton.trakt.TraktEnumeration;

public enum DayOfTheWeek implements TraktEnumeration {
    Sunday("Sunday"),
    Monday("Monday"),
    Tuesday("Tuesday"),
    Wednesday("Wednesday"),
    Thursday("Thursday"),
    Friday("Friday"),
    Saturday("Saturday");

    private final String value;

    private DayOfTheWeek(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static final Map<String, DayOfTheWeek> STRING_MAPPING = new HashMap<String, DayOfTheWeek>();

    static {
        for (DayOfTheWeek via : DayOfTheWeek.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static DayOfTheWeek fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }
}
