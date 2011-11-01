package com.jakewharton.trakt.enumerations;

import java.util.HashMap;
import java.util.Map;
import com.jakewharton.trakt.TraktEnumeration;

public enum Gender implements TraktEnumeration {
	Male("male"),
	Female("female");
	
	private final String value;
	
	private Gender(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return this.value;
	}
	
	private static final Map<String, Gender> STRING_MAPPING = new HashMap<String, Gender>();

	static {
		for (Gender via : Gender.values()) {
			STRING_MAPPING.put(via.toString().toUpperCase(), via);
		}
	}
	
	public static Gender fromValue(String value) {
		return STRING_MAPPING.get(value.toUpperCase());
	}
}
