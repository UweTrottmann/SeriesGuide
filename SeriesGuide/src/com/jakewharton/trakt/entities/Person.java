package com.jakewharton.trakt.entities;

import java.util.Date;
import com.jakewharton.trakt.TraktEntity;

public final class Person implements TraktEntity {
	private static final long serialVersionUID = -4755476212550445673L;
	
	private String name;
	private String url;
	private String biography;
	private Date birthday;
	private String birthplace;
	private Integer tmdbId;
	private Images images;
	
	public String getName() {
		return this.name;
	}
	public String getUrl() {
		return this.url;
	}
	public String getBiography() {
		return this.biography;
	}
	public Date getBirthday() {
		return this.birthday;
	}
	public String getBirthplace() {
		return this.birthplace;
	}
	public Integer getTmdbId() {
		return this.tmdbId;
	}
	public Images getImages() {
		return this.images;
	}
}
