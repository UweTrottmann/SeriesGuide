package com.jakewharton.trakt.entities;

import java.util.Date;
import java.util.List;
import com.jakewharton.trakt.TraktEntity;

public final class TvShow extends MediaBase implements TraktEntity {
	private static final long serialVersionUID = 862473930551420996L;

	private Date firstAired;
	private String country;
	private String overview;
	private Integer runtime;
	private String network;
	private String airDay; //TODO: enum
	private String airTime;
	private String certification; //TODO: enum
	private String tvdbId;
	private String tvrageId;
	private List<TvShowEpisode> episodes;
	private List<TvShowEpisode> topEpisodes;
	
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
}
