package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

public final class Movie extends MediaBase implements TraktEntity {
	private static final long serialVersionUID = -1543214252495012419L;

	private String tmdbId;
	private Integer plays;
	private Boolean inCollection;
	
	public String getTmdbId() {
		return this.tmdbId;
	}
	public Integer getPlays() {
		return this.plays;
	}
	public Boolean getInCollection() {
		return this.inCollection;
	}
}
