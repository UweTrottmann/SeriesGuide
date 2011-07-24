package com.jakewharton.trakt.enumerations;

import java.util.HashMap;
import java.util.Map;
import com.jakewharton.trakt.TraktEnumeration;

public enum Rating implements TraktEnumeration {
	Love("love"),
	Hate("hate");
	
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
			STRING_MAPPING.put(via.toString(), via);
		}
	}
	
	public static Rating fromValue(String value) {
		return STRING_MAPPING.get(value);
	}
}
