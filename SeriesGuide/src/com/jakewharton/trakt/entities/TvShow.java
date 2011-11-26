package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.DayOfTheWeek;

import java.util.Date;
import java.util.List;

public final class TvShow extends MediaBase implements TraktEntity {
	private static final long serialVersionUID = 862473930551420996L;

	@SerializedName("first_aired") private Date firstAired;
	private String country;
	private String overview;
	private Integer runtime;
	private String network;
	@SerializedName("air_day") private DayOfTheWeek airDay;
	@SerializedName("air_time") private String airTime;
	private String certification; //TODO: enum
	@SerializedName("tvdb_id") private String tvdbId;
	@SerializedName("tvrage_id") private String tvrageId;
	private List<TvShowEpisode> episodes;
	@SerializedName("top_episodes") private List<TvShowEpisode> topEpisodes;
	private List<TvShowSeason> seasons;
	
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
	public DayOfTheWeek getAirDay() {
		return this.airDay;
	}
	public String getAirTime() {
		return this.airTime;
	}
	public String getCertification() {
		return this.certification;
	}
	public String getTvdbId() {
		return this.tvdbId;
	}
	public String getTvRageId() {
		return this.tvrageId;
	}
	public List<TvShowEpisode> getEpisodes() {
		return this.episodes;
	}
	public List<TvShowEpisode> getTopEpisodes() {
		return this.topEpisodes;
	}
	public List<TvShowSeason> getSeasons() {
		return this.seasons;
	}
}
