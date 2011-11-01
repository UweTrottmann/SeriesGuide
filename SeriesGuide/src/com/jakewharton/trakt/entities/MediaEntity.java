package com.jakewharton.trakt.entities;

import java.util.Calendar;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.MediaType;

public class MediaEntity implements TraktEntity {
	private static final long serialVersionUID = 4535846809492296227L;

	private MediaType type;
	private Calendar watched;
	private Calendar date;
	private Movie movie;
	private TvShow show;
	private TvShowEpisode episode;
	
	public MediaType getType() {
		return this.type;
	}
	public Calendar getWatched() {
		return this.watched;
	}
	public Calendar getDate() {
		return this.date;
	}
	public Movie getMovie() {
		return this.movie;
	}
	public TvShow getShow() {
		return this.show;
	}
	public TvShowEpisode getEpisode() {
		return this.episode;
	}
}
