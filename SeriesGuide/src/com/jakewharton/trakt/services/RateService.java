package com.jakewharton.trakt.services;

import com.google.gson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.RatingResponse;
import com.jakewharton.trakt.enumerations.Rating;

public class RateService extends TraktApiService {
	/**
	 * Rate an episode on Trakt. Depending on the user settings, this will also
	 * send out social updates to Facebook, Twitter, and Tumblr.
	 * 
	 * @param tvdbId TVDB ID for the show.
	 * @return Builder instance.
	 */
	public EpisodeBuilder episode(int tvdbId) {
		return new EpisodeBuilder(this).tvdbId(tvdbId);
	}
	
	/**
	 * Rate an episode on Trakt. Depending on the user settings, this will also
	 * send out social updates to Facebook, Twitter, and Tumblr.
	 * 
	 * @param title Show title.
	 * @param year Show year.
	 * @return Builder instance.
	 */
	public EpisodeBuilder episode(String title, int year) {
		return new EpisodeBuilder(this).title(title).year(year);
	}
	
	public static final class EpisodeBuilder extends TraktApiBuilder<RatingResponse> {
		private static final String POST_TVDB_ID = "tvdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_SEASON = "season";
		private static final String POST_EPISODE = "episode";
		private static final String POST_RATING = "rating";
		
		private static final String URI = "/rate/episode/" + FIELD_API_KEY;
		
		private EpisodeBuilder(RateService service) {
			super(service, new TypeToken<RatingResponse>() {}, URI, HttpMethod.Post);
		}
		
		public EpisodeBuilder tvdbId(int tvdbId) {
			this.postParameter(POST_TVDB_ID, tvdbId);
			return this;
		}
		
		public EpisodeBuilder title(String title) {
			this.postParameter(POST_TITLE, title);
			return this;
		}
		
		public EpisodeBuilder year(int year) {
			this.postParameter(POST_YEAR, year);
			return this;
		}
		
		public EpisodeBuilder season(int season) {
			this.postParameter(POST_SEASON, season);
			return this;
		}
		
		public EpisodeBuilder episode(int episode) {
			this.postParameter(POST_EPISODE, episode);
			return this;
		}
		
		public EpisodeBuilder rating(Rating rating) {
			this.postParameter(POST_RATING, rating);
			return this;
		}

		@Override
		protected void performValidation() {
			assert this.hasPostParameter(POST_TVDB_ID)
				|| (this.hasPostParameter(POST_TITLE) && this.hasPostParameter(POST_YEAR))
				: "Either TVDB ID or both title and year is required.";
			assert this.hasPostParameter(POST_SEASON) : "Season is required.";
			assert this.hasPostParameter(POST_EPISODE) : "Episode is required.";
			assert this.hasPostParameter(POST_RATING) : "Rating is required.";
		}
	}
	
	/**
	 * Rate a movie on Trakt. Depending on the user settings, this will also
	 * send out social updates to Facebook, Twitter, and Tumblr.
	 * 
	 * @param imdbId IMDB ID for the movie.
	 * @return Builder instance.
	 */
	public MovieBuilder movie(String imdbId) {
		return new MovieBuilder(this).imdbId(imdbId);
	}
	
	/**
	 * Rate a movie on Trakt. Depending on the user settings, this will also
	 * send out social updates to Facebook, Twitter, and Tumblr.
	 * 
	 * @param title Movie title.
	 * @param year Movie year.
	 * @return Builder instance.
	 */
	public MovieBuilder movie(String title, int year) {
		return new MovieBuilder(this).title(title).year(year);
	}
	
	public static final class MovieBuilder extends TraktApiBuilder<RatingResponse> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_RATING = "rating";
		
		private static final String URI = "/rate/movie/" + FIELD_API_KEY;
		
		private MovieBuilder(RateService service) {
			super(service, new TypeToken<RatingResponse>() {}, URI, HttpMethod.Post);
		}
		
		public MovieBuilder imdbId(String imdbId) {
			this.postParameter(POST_IMDB_ID, imdbId);
			return this;
		}
		
		public MovieBuilder title(String title) {
			this.postParameter(POST_TITLE, title);
			return this;
		}
		
		public MovieBuilder year(int year) {
			this.postParameter(POST_YEAR, year);
			return this;
		}
		
		public MovieBuilder rating(Rating rating) {
			this.postParameter(POST_RATING, rating);
			return this;
		}

		@Override
		protected void performValidation() {
			assert this.hasPostParameter(POST_IMDB_ID)
				|| (this.hasPostParameter(POST_TITLE) && this.hasPostParameter(POST_YEAR))
				: "Either IMDB ID or both title and year is required.";
			assert this.hasPostParameter(POST_RATING) : "Rating is required.";
		}
	}
	
	/**
	 * Rate a show on Trakt. Depending on the user settings, this will also
	 * send out social updates to Facebook, Twitter, and Tumblr.
	 * 
	 * @param tvdbId TVDB ID for the show.
	 * @return Builder instance.
	 */
	public ShowBuilder show(int tvdbId) {
		return new ShowBuilder(this).tvdbId(tvdbId);
	}
	
	/**
	 * Rate a show on Trakt. Depending on the user settings, this will also
	 * send out social updates to Facebook, Twitter, and Tumblr.
	 * 
	 * @param title Show title.
	 * @param year Show year.
	 * @return Builder instance.
	 */
	public ShowBuilder show(String title, int year) {
		return new ShowBuilder(this).title(title).year(year);
	}
	
	public static final class ShowBuilder extends TraktApiBuilder<RatingResponse> {
		private static final String POST_TVDB_ID = "imdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_RATING = "rating";
		
		private static final String URI = "/rate/show/" + FIELD_API_KEY;
		
		private ShowBuilder(RateService service) {
			super(service, new TypeToken<RatingResponse>() {}, URI, HttpMethod.Post);
		}
		
		public ShowBuilder tvdbId(int tvdbId) {
			this.postParameter(POST_TVDB_ID, tvdbId);
			return this;
		}
		
		public ShowBuilder title(String title) {
			this.postParameter(POST_TITLE, title);
			return this;
		}
		
		public ShowBuilder year(int year) {
			this.postParameter(POST_YEAR, year);
			return this;
		}
		
		public ShowBuilder rating(Rating rating) {
			this.postParameter(POST_RATING, rating);
			return this;
		}

		@Override
		protected void performValidation() {
			assert this.hasPostParameter(POST_TVDB_ID)
				|| (this.hasPostParameter(POST_TITLE) && this.hasPostParameter(POST_YEAR))
				: "Either TVDB ID or both title and year is required.";
			assert this.hasPostParameter(POST_RATING) : "Rating is required.";
		}
	}
}
