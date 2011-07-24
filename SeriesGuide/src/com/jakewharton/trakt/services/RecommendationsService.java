package com.jakewharton.trakt.services;

import java.util.List;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.entities.TvShow;

public class RecommendationsService extends TraktApiService {
	/**
	 * Get a list of movie recommendations created from your watching history
	 * and your friends. Results returned with the top recommendation first.
	 * 
	 * @return Builder instance.
	 */
	public MoviesBuilder movies() {
		return new MoviesBuilder(this);
	}
	
	public static final class MoviesBuilder extends TraktApiBuilder<List<Movie>> {
		private static final String URI = "/recommendations/movies/" + FIELD_API_KEY;
		
		private MoviesBuilder(RecommendationsService service) {
			super(service, new TypeToken<List<Movie>>() {}, URI, HttpMethod.Post);
		}
	}
	
	/**
	 * Get a list of show recommendations created from your watching history
	 * and your friends. Results returned with the top recommendation first.
	 * 
	 * @return Builder instance.
	 */
	public ShowsBuilder shows() {
		return new ShowsBuilder(this);
	}
	
	public static final class ShowsBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/recommendations/shows/" + FIELD_API_KEY;
		
		private ShowsBuilder(RecommendationsService service) {
			super(service, new TypeToken<List<TvShow>>() {}, URI, HttpMethod.Post);
		}
	}
}
