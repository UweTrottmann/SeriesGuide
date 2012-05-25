package com.jakewharton.trakt.enumerations;

import java.util.HashMap;
import java.util.Map;
import com.jakewharton.trakt.TraktEnumeration;

public enum Rating implements TraktEnumeration {
    WeakSauce("1"),
    Terrible("2"),
    Bad("3"),
    Poor("4"),
    Meh("5"),
    Fair("6"),
    Good("7"),
    Great("8"),
    Superb("9"),
    TotallyNinja("10"),
    Unrate("0");

    private final String value;

    private Rating(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static final Map<String, Rating> STRING_MAPPING = new HashMap<String, Rating>();

    static {
        for (Rating via : Rating.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static Rating fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }
}
