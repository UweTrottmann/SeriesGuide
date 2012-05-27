
package com.jakewharton.trakt.enumerations;

import com.jakewharton.trakt.TraktEnumeration;

import java.util.HashMap;
import java.util.Map;

public enum ExtendedParam implements TraktEnumeration {
    Extended("extended"), Min("min");

    private final String value;

    private ExtendedParam(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static final Map<String, ExtendedParam> STRING_MAPPING = new HashMap<String, ExtendedParam>();

    static {
        for (ExtendedParam via : ExtendedParam.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static ExtendedParam fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }
}
