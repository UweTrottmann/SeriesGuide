package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;

import java.util.Date;

public final class Movie extends MediaBase implements TraktEntity {
	private static final long serialVersionUID = -1543214252495012419L;

	@SerializedName("tmdb_id") private String tmdbId;
	private Integer plays;
	@SerializedName("in_collection") private Boolean inCollection;
	private Date released;
	private String trailer;
	private Integer runtime;
	private String tagline;
	private String overview;
	private String certification; //TODO make enum
	private Boolean watched;
	
	public String getTmdbId() {
		return this.tmdbId;
	}
	public Integer getPlays() {
		return this.plays;
	}
	public Boolean getInCollection() {
		return this.inCollection;
	}
	public Date getReleased() {
		return this.released;
	}
	public String getTrailer() {
		return this.trailer;
	}
	public Integer getRuntime() {
		return this.runtime;
	}
	public String getTagline() {
		return this.tagline;
	}
	public String getOverview() {
		return this.overview;
	}
	public String getCertification() {
		return this.certification;
	}
	public Boolean getWatched() {
	    return this.watched;
	}
}
