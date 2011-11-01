package com.jakewharton.trakt.entities;

import java.util.Calendar;
import java.util.Date;
import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.Rating;

public final class TvShowEpisode implements TraktEntity {
	private static final long serialVersionUID = -1550739539663499211L;
	
	private Integer season;
	private Integer number;
	private String title;
	private String overview;
	private String url;
	@SerializedName("first_aired") private Date firstAired;
	private Calendar inserted;
	private Integer plays;
	private Images images;
	private Ratings ratings;
	private Boolean watched;
	private Rating rating;
	@SerializedName("in_watchlist") private Boolean inWatchlist;
	
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
	public Calendar getInserted() {
		return this.inserted;
	}
	public Integer getPlays() {
		return this.plays;
	}
	public Images getImages() {
		return this.images;
	}
	public Ratings getRatings() {
		return this.ratings;
	}
	public Boolean getWatched() {
		return this.watched;
	}
	public Rating getRating() {
		return this.rating;
	}
	public Boolean getInWatchlist() {
		return this.inWatchlist;
	}
}
