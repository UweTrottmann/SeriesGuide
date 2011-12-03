package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.MediaType;

import java.util.Calendar;

public class MediaEntity implements TraktEntity {
    private static final long serialVersionUID = 4535846809492296227L;

    public MediaType type;
    public Calendar watched;
    public Calendar date;
    public Movie movie;
    public TvShow show;
    public TvShowEpisode episode;

    /** @deprecated Use {@link #type} */
    @Deprecated
    public MediaType getType() {
        return this.type;
    }
    /** @deprecated Use {@link #watched} */
    @Deprecated
    public Calendar getWatched() {
        return this.watched;
    }
    /** @deprecated Use {@link #date} */
    @Deprecated
    public Calendar getDate() {
        return this.date;
    }
    /** @deprecated Use {@link #movie} */
    @Deprecated
    public Movie getMovie() {
        return this.movie;
    }
    /** @deprecated Use {@link #show} */
    @Deprecated
    public TvShow getShow() {
        return this.show;
    }
    /** @deprecated Use {@link #episode} */
    @Deprecated
    public TvShowEpisode getEpisode() {
        return this.episode;
    }
}
