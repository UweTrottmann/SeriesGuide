package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

public class Response implements TraktEntity {
	private static final long serialVersionUID = 5921890886906816035L;
	
	private String status; //TODO: enum
	private String message;
	private String error;

	public String getStatus() {
		return this.status;
	}
	public String getMessage() {
		return this.message;
	}
	public String getError() {
	    return this.error;
	}
}
