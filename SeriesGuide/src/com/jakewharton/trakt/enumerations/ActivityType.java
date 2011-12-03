package com.jakewharton.trakt.enumerations;

import java.util.HashMap;
import java.util.Map;
import com.jakewharton.trakt.TraktEnumeration;

public enum ActivityType implements TraktEnumeration {
    All("all"),
    Episode("episode"),
    Show("show"),
    Movie("movie"),
    List("list");

    private final String value;

    private ActivityType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static final Map<String, ActivityType> STRING_MAPPING = new HashMap<String, ActivityType>();

    static {
        for (ActivityType via : ActivityType.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static ActivityType fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }
}
