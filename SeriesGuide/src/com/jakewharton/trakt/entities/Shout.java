package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

import java.util.Date;

public class Shout implements TraktEntity {
	private static final long serialVersionUID = 4324069488018464744L;

	private Date inserted;
	private String shout;
	private UserProfile user;
	
	public Date getInserted() {
		return this.inserted;
	}
	public String getShout() {
		return this.shout;
	}
	public UserProfile getUser() {
		return this.user;
	}
}
