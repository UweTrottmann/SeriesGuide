package com.jakewharton.trakt.entities;

public class DismissResponse extends Response {
	private static final long serialVersionUID = -5706552629205669409L;

	private Movie movie;
	private TvShow show;

	public Movie getMovie() {
		return this.movie;
	}
	public TvShow getTvShow() {
		return this.show;
	}
}
