package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

import java.util.Date;
import java.util.List;

public final class TvShow extends MediaBase implements TraktEntity {
	private static final long serialVersionUID = 862473930551420996L;

	private Date firstAired;
	private String country;
	private String overview;
	private Integer runtime;
	private String network;
	private String air_day; //TODO: enum
	private String air_time;
	private String certification; //TODO: enum
	private String tvdb_id;
	private String tvrage_id;
	private List<TvShowEpisode> episodes;
	private List<TvShowEpisode> topEpisodes;
	private List<WatchedSeasons> seasons;
	
	public Date getFirstAired() {
		return this.firstAired;
	}
	public String getCountry() {
		return this.country;
	}
	public String getOverview() {
		return this.overview;
	}
	public Integer getRuntime() {
		return this.runtime;
	}
	public String getNetwork() {
		return this.network;
	}
	public String getAirDay() {
		return this.air_day;
	}
	public String getAirTime() {
		return this.air_time;
	}
	public String getCertification() {
		return this.certification;
	}
	public String getTvdbId() {
		return this.tvdb_id;
	}
	public String getTvRageId() {
		return this.tvrage_id;
	}
	public List<TvShowEpisode> getEpisodes() {
		return this.episodes;
	}
	public List<TvShowEpisode> getTopEpisodes() {
		return this.topEpisodes;
	}
	public List<WatchedSeasons> getSeasons() {
	    return this.seasons;
	}
}
