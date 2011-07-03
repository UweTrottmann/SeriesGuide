package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.Rating;

public class RatingResponse extends Response implements TraktEntity {
	private static final long serialVersionUID = 8424378149600617021L;
	
	private String type; //TODO: enum
	private Rating rating;
	private Ratings ratings;
	private Boolean facebook;
	private Boolean twitter;
	private Boolean tumblr;
	
	public String getType() {
		return this.type;
	}
	public Rating getRating() {
		return this.rating;
	}
	public Ratings getRatings() {
		return this.ratings;
	}
	public Boolean getFacebook() {
		return this.facebook;
	}
	public Boolean getTwitter() {
		return this.twitter;
	}
	public Boolean getTumblr() {
		return this.tumblr;
	}
}
