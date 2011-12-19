package com.jakewharton.trakt.enumerations;

import java.util.HashMap;
import java.util.Map;
import com.jakewharton.trakt.TraktEnumeration;

public enum ListItemType implements TraktEnumeration {
    Movie("movie"),
    TvShow("show"),
    TvShowSeason("season"),
    TvShowEpisode("episode");

    private final String value;

    private ListItemType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static final Map<String, ListItemType> STRING_MAPPING = new HashMap<String, ListItemType>();

    static {
        for (ListItemType via : ListItemType.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static ListItemType fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }
}
