package com.jakewharton.trakt.entities;

import java.util.List;
import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.Rating;

public abstract class MediaBase implements TraktEntity {
	private static final long serialVersionUID = 753880113366868498L;

	public static class Stats implements TraktEntity {
		private static final long serialVersionUID = -5436127125832664020L;

		private Integer watchers;
		private Integer plays;

		public Integer getWatchers() {
			return this.watchers;
		}
		public Integer getPlays() {
			return this.plays;
		}
	}

	private String title;
	private Integer year;
	private String url;
	private Images images;
	@SerializedName("top_watchers") private List<UserProfile> topWatchers;
	private Ratings ratings;
	private Stats stats;
	@SerializedName("imdb_id") private String imdbId;
	private Rating rating;
	@SerializedName("in_watchlist") private Boolean inWatchlist;

	public String getTitle() {
		return this.title;
	}
	public Integer getYear() {
		return this.year;
	}
	public String getUrl() {
		return this.url;
	}
	public Images getImages() {
		return this.images;
	}
	public List<UserProfile> getTopWatchers() {
		return this.topWatchers;
	}
	public Ratings getRatings() {
		return this.ratings;
	}
	public Stats getStats() {
		return this.stats;
	}
	public String getImdbId() {
		return this.imdbId;
	}
	public Rating getRating() {
		return this.rating;
	}
	public Boolean getInWatchlist() {
		return this.inWatchlist;
	}
}
