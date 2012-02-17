package com.jakewharton.trakt.enumerations;

import com.jakewharton.trakt.TraktEnumeration;

import java.util.HashMap;
import java.util.Map;

public enum ActivityAction implements TraktEnumeration {
    All("all"),
    Watching("watching"),
    Scrobble("scrobble"),
    Checkin("checkin"),
    Seen("seen"),
    Collection("collection"),
    Rating("rating"),
    Watchlist("watchlist"),
    Shout("shout"),
    Created("created"),
    ItemAdded("item_added");

    private final String value;

    private ActivityAction(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static final Map<String, ActivityAction> STRING_MAPPING = new HashMap<String, ActivityAction>();

    static {
        for (ActivityAction via : ActivityAction.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static ActivityAction fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }
}
