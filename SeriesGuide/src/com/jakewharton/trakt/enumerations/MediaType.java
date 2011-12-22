package com.jakewharton.trakt.enumerations;

import com.jakewharton.trakt.TraktEnumeration;

import java.util.HashMap;
import java.util.Map;

public enum MediaType implements TraktEnumeration {
    Movie("movie"),
    TvShow("episode");

    private final String value;

    private MediaType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static final Map<String, MediaType> STRING_MAPPING = new HashMap<String, MediaType>();

    static {
        for (MediaType via : MediaType.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static MediaType fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }
}
