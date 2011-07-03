package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

public final class Images implements TraktEntity {
	private static final long serialVersionUID = -4374523954772900340L;
	
	private String poster;
	private String fanart;
	private String headshot;
	
	public String getPoster() {
		return this.poster;
	}
	public String getFanart() {
		return this.fanart;
	}
	public String getHeadshot() {
		return this.headshot;
	}
}