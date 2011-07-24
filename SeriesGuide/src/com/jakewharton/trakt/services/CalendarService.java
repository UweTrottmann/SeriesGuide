package com.jakewharton.trakt.services;

import java.util.Date;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.CalendarDate;

public class CalendarService extends TraktApiService {
	/**
	 * Returns all shows premiering during the time period specified.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public PremieresBuilder premieres(String username) {
		return new PremieresBuilder(this, username);
	}
	
	public static final class PremieresBuilder extends TraktApiBuilder<List<CalendarDate>> {
		private static final int DEFAULT_DAYS = 7;
		private static final String URI = "/calendar/premieres.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME + "/" + FIELD_DATE + "/" + FIELD_DAYS;
		
		private PremieresBuilder(CalendarService service, String username) {
			super(service, new TypeToken<List<CalendarDate>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
		
		/**
		 * Start date for the calendar.
		 * 
		 * @param date Value.
		 * @return Builder instance.
		 */
		public PremieresBuilder date(Date date) {
			this.field(FIELD_DATE, date);
			
			if (!this.hasField(FIELD_DAYS)) {
				//Set default.
				this.field(FIELD_DAYS, DEFAULT_DAYS);
			}
			
			return this;
		}
		
		/**
		 * Number of days to display starting from the date.
		 * 
		 * @param days Value.
		 * @return Builder instance.
		 */
		public PremieresBuilder days(int days) {
			this.field(FIELD_DAYS, days);
			
			if (!this.hasField(FIELD_DATE)) {
				//Set default (today).
				this.field(FIELD_DATE, new Date());
			}
			
			return this;
		}
	}
	
	/**
	 * Returns all shows premiering during the time period specified.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public ShowsBuilder shows(String username) {
		return new ShowsBuilder(this, username);
	}
	
	public static final class ShowsBuilder extends TraktApiBuilder<List<CalendarDate>> {
		private static final int DEFAULT_DAYS = 7;
		private static final String URI = "/calendar/shows.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME + "/" + FIELD_DATE + "/" + FIELD_DAYS;
		
		private ShowsBuilder(CalendarService service, String username) {
			super(service, new TypeToken<List<CalendarDate>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
		
		/**
		 * Start date for the calendar.
		 * 
		 * @param date Value.
		 * @return Builder instance.
		 */
		public ShowsBuilder date(Date date) {
			this.field(FIELD_DATE, date);
			
			if (!this.hasField(FIELD_DAYS)) {
				//Set default.
				this.field(FIELD_DAYS, DEFAULT_DAYS);
			}
			
			return this;
		}
		
		/**
		 * Number of days to display starting from the date.
		 * 
		 * @param days Value.
		 * @return Builder instance.
		 */
		public ShowsBuilder days(int days) {
			this.field(FIELD_DAYS, days);
			
			if (!this.hasField(FIELD_DATE)) {
				//Set default (today).
				this.field(FIELD_DATE, new Date());
			}
			
			return this;
		}
	}
}
