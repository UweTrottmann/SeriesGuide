package com.jakewharton.trakt.entities;

public class DismissResponse extends Response {
    private static final long serialVersionUID = -5706552629205669409L;

    public Movie movie;
    public TvShow show;

    /** @deprecated Use {@link #movie} */
    @Deprecated
    public Movie getMovie() {
        return this.movie;
    }
    /** @deprecated Use {@link #show} */
    @Deprecated
    public TvShow getTvShow() {
        return this.show;
    }
}
