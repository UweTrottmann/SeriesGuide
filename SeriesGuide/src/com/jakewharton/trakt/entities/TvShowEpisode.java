package com.jakewharton.trakt.entities;

import java.util.Date;
import com.jakewharton.trakt.TraktEntity;

public final class TvShowEpisode implements TraktEntity {
	private static final long serialVersionUID = -1550739539663499211L;
	
	private Integer season;
	private Integer number;
	private String title;
	private String overview;
	private String url;
	private Date firstAired;
	private Date inserted;
	private Integer plays;
	
	public Integer getSeason() {
		return this.season;
	}
	public Integer getNumber() {
		return this.number;
	}
	public String getTitle() {
		return this.title;
	}
	public String getOverview() {
		return this.overview;
	}
	public String getUrl() {
		return this.url;
	}
	public Date getFirstAired() {
		return this.firstAired;
	}
	public Date getInserted() {
		return this.inserted;
	}
	public Integer getPlays() {
		return this.plays;
	}
}
