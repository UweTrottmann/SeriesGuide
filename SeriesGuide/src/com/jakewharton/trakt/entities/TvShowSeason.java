package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

public class TvShowSeason implements TraktEntity {
	private static final long serialVersionUID = -1283154821327471366L;

	private Integer season;
	private Integer episodes;
	private String url;
	private Images images;
	
	public Integer getSeason() {
		return this.season;
	}
	public Integer getEpisodes() {
		return this.episodes;
	}
	public String getUrl() {
		return this.url;
	}
	public Images getImages() {
		return this.images;
	}
}
