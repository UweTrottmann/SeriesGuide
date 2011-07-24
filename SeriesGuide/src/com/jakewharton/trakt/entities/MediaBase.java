package com.jakewharton.trakt.entities;

import java.util.List;
import com.jakewharton.trakt.TraktEntity;

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
	private List<UserProfile> topWatchers;
	private Ratings ratings;
	private Stats stats;
	private String imdbId;

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
}
