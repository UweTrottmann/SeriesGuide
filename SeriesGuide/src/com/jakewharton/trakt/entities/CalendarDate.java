package com.jakewharton.trakt.entities;

import java.util.Date;
import java.util.List;
import com.jakewharton.trakt.TraktEntity;

public final class CalendarDate implements TraktEntity {
	private static final long serialVersionUID = 5985118362541597172L;
	
	public static final class CalendarTvShowEpisode implements TraktEntity {
		private static final long serialVersionUID = -7066863350641449761L;
		
		private Boolean watched;
		private String rating; //TODO: enum
		private TvShow show;
		private TvShowEpisode episode;
		
		public Boolean getWatched() {
			return this.watched;
		}
		public String getRating() {
			return this.rating;
		}
		public TvShow getShow() {
			return this.show;
		}
		public TvShowEpisode getEpisode() {
			return this.episode;
		}
	}
	
	private Date date;
	private List<CalendarTvShowEpisode> episodes;
	
	public Date getDate() {
		return this.date;
	}
	public List<CalendarTvShowEpisode> getEpisodes() {
		return this.episodes;
	}
}
