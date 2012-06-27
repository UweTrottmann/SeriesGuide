package com.jakewharton.trakt.enumerations;

import java.util.HashMap;
import java.util.Map;
import com.jakewharton.trakt.TraktEnumeration;

public enum RatingType implements TraktEnumeration {
    Episode("episode"),
    Movie("movie"),
    Show("show");

    private final String value;

    private RatingType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static final Map<String, RatingType> STRING_MAPPING = new HashMap<String, RatingType>();

    static {
        for (RatingType via : RatingType.values()) {
            STRING_MAPPING.put(via.toString(), via);
        }
    }

    public static RatingType fromValue(String value) {
        return STRING_MAPPING.get(value);
    }
}
