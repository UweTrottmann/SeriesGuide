package com.jakewharton.trakt.services;

import java.util.List;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.entities.TvShow;

public class TrendingService extends TraktApiService {
	/**
	 * Returns all movies being watched right now.
	 * 
	 * @return Builder instance.
	 */
	public MoviesBuilder movies() {
		return new MoviesBuilder(this);
	}
	
	public static final class MoviesBuilder extends TraktApiBuilder<List<Movie>> {
		private static final String URI = "/movies/trending.json/" + FIELD_API_KEY;
		
		private MoviesBuilder(TrendingService service) {
			super(service, new TypeToken<List<Movie>>() {}, URI);
		}
	}
	
	/**
	 * Returns all shows being watched right now.
	 * 
	 * @return Builder instance.
	 */
	public TvShowsBuilder tvShows() {
		return new TvShowsBuilder(this);
	}
	
	public static final class TvShowsBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/shows/trending.json/" + FIELD_API_KEY;
		
		private TvShowsBuilder(TrendingService service) {
			super(service, new TypeToken<List<TvShow>>() {}, URI);
		}
	}
}
