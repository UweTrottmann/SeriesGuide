package com.jakewharton.trakt.entities;

import java.util.Date;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.MediaType;

public final class MediaEntity implements TraktEntity {
	private static final long serialVersionUID = 4535846809492296227L;

	private MediaType type;
	private Date watched;
	private Date date;
	private Movie movie;
	private TvShow show;
	private TvShowEpisode episode;
	
	public MediaType getType() {
		return this.type;
	}
	public Date getWatched() {
		return this.watched;
	}
	public Date getDate() {
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
