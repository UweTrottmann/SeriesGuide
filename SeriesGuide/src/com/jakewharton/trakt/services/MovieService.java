package com.jakewharton.trakt.services;

import java.util.Date;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.entities.Shout;
import com.jakewharton.trakt.entities.UserProfile;

public class MovieService extends TraktApiService {
	/**
	 * <p>Notify Trakt that a user has stopped watching a movie.</p>
	 * 
	 * <p><em>Warning</em>: This method requires a developer API key.</p>
	 * 
	 * @return Builder instance.
	 */
	public CancelWatchingBuilder cancelWatching() {
		return new CancelWatchingBuilder(this);
	}
	
	public static final class CancelWatchingBuilder extends TraktApiBuilder<Response> {
		private static final String URI = "/movie/cancelwatching/" + FIELD_API_KEY;
		
		private CancelWatchingBuilder(MovieService service) {
			super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
		}
	}
	
	/**
	 * <p>Notify Trakt that a user has finsihed watching a movie. This commits
	 * the movie to the users profile. You should use movie/watching prior to
	 * calling this method.</p>
	 * 
	 * <p><em>Warning</em>: This method requires a developer API key.</p>
	 * 
	 * @param imdbId IMDB ID for the movie.
	 * @return Builder instance.
	 */
	public ScrobbleBuilder scrobble(String imdbId) {
		return new ScrobbleBuilder(this).imdbId(imdbId);
	}
	
	/**
	 * <p>Notify Trakt that a user has finsihed watching a movie. This commits
	 * the movie to the users profile. You should use movie/watching prior to
	 * calling this method.</p>
	 * 
	 * <p><em>Warning</em>: This method requires a developer API key.</p>
	 * 
	 * @param tmdbId TMDB (themoviedb.org) ID for the movie. 
	 * @return Builder instance.
	 */
	public ScrobbleBuilder scrobble(int tmdbId) {
		return new ScrobbleBuilder(this).tmdbId(tmdbId);
	}
	
	/**
	 * <p>Notify Trakt that a user has finsihed watching a movie. This commits
	 * the movie to the users profile. You should use movie/watching prior to
	 * calling this method.</p>
	 * 
	 * <p><em>Warning</em>: This method requires a developer API key.</p>
	 * 
	 * @param title Movie title.
	 * @param year Movie year.
	 * @return Builder instance.
	 */
	public ScrobbleBuilder scrobble(String title, int year) {
		return new ScrobbleBuilder(this).title(title).year(year);
	}
	
	public static final class ScrobbleBuilder extends TraktApiBuilder<Response> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TMDB_ID = "tmdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_DURATION = "duration";
		private static final String POST_PROGRESS = "progress";
		
		private static final String URI = "/movie/scrobble/" + FIELD_API_KEY;
		
		private ScrobbleBuilder(MovieService service) {
			super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
			this.includeDebugStrings();
		}
		
		/**
		 * IMDB ID for the movie.
		 * 
		 * @param imdbId Value.
		 * @return Builder instance.
		 */
		public ScrobbleBuilder imdbId(String imdbId) {
			this.postParameter(POST_IMDB_ID, imdbId);
			return this;
		}
		
		/**
		 * TMDB (themoviedb.org) ID for the movie.
		 * 
		 * @param tmdbId Value.
		 * @return Builder instance.
		 */
		public ScrobbleBuilder tmdbId(int tmdbId) {
			this.postParameter(POST_TMDB_ID, tmdbId);
			return this;
		}
		
		/**
		 * Movie title.
		 * 
		 * @param title Value.
		 * @return Builder instance.
		 */
		public ScrobbleBuilder title(String title) {
			this.postParameter(POST_TITLE, title);
			return this;
		}
		
		/**
		 * Movie year.
		 * 
		 * @param year Value.
		 * @return Builder instance.
		 */
		public ScrobbleBuilder year(int year) {
			this.postParameter(POST_YEAR, year);
			return this;
		}
		
		/**
		 * Duration (in minutes).
		 * 
		 * @param duration Value.
		 * @return Builder instance.
		 */
		public ScrobbleBuilder duration(int duration) {
			this.postParameter(POST_DURATION, duration);
			return this;
		}
		
		/**
		 * Percent progress (0-100). It is recommended to call the watching API
		 * every 15 minutes, then call the scrobble API near the end of the
		 * movie to lock it in.
		 * 
		 * @param progress Value.
		 * @return Builder instance.
		 */
		public ScrobbleBuilder progress(int progress) {
			this.postParameter(POST_PROGRESS, progress);
			return this;
		}

		@Override
		protected void performValidation() {
			assert this.hasPostParameter(POST_IMDB_ID)
			|| this.hasPostParameter(POST_TMDB_ID)
			|| (this.hasPostParameter(POST_TITLE) && this.hasPostParameter(POST_YEAR))
			: "Either IMDB ID, TMDB ID, or both title and year is required.";
			assert this.hasPostParameter(POST_DURATION) : "Duration is required.";
			assert this.hasPostParameter(POST_PROGRESS) : "Progress is required.";
		}
	}
	
	/**
	 * Add movies watched outside of Trakt to your library.
	 * 
	 * @return Builder instance.
	 */
	public SeenBuilder seen() {
		return new SeenBuilder(this);
	}
	
	public static final class SeenBuilder extends TraktApiBuilder<Void> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TMDB_ID = "tmdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_PLAYS = "plays";
		private static final String POST_LAST_PLAYED = "last_played";
		private static final String POST_MOVIES = "movies";
		
		private static final String URI = "/movie/seen/" + FIELD_API_KEY;
		
		private final JsonArray movieList;
		
		private SeenBuilder(MovieService service) {
			super(service, new TypeToken<Void>() {}, URI);
			
			this.movieList = new JsonArray();
		}
		
		/**
		 * Add a movie to the list of seen movies.
		 * 
		 * @param imdbId IMDB ID for the movie.
		 * @param plays Number of plays.
		 * @param lastPlayed Timestamp of the last time it was played.
		 * @return Builder instance.
		 */
		public SeenBuilder movie(String imdbId, int plays, Date lastPlayed) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_IMDB_ID, imdbId);
			movie.addProperty(POST_PLAYS, plays);
			movie.addProperty(POST_LAST_PLAYED, TraktApiBuilder.dateToUnixTimestamp(lastPlayed));
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of seen movies.
		 * 
		 * @param tmdbId TMDB ID for the movie.
		 * @param plays Number of plays.
		 * @param lastPlayed Timestamp of the last time it was played.
		 * @return Builder instance.
		 */
		public SeenBuilder movie(int tmdbId, int plays, Date lastPlayed) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TMDB_ID, tmdbId);
			movie.addProperty(POST_PLAYS, plays);
			movie.addProperty(POST_LAST_PLAYED, TraktApiBuilder.dateToUnixTimestamp(lastPlayed));
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of seen movies.
		 * 
		 * @param title Movie title.
		 * @param year Movie year.
		 * @param plays Number of plays.
		 * @param lastPlayed Timestamp of the last time it was played.
		 * @return Builder instance.
		 */
		public SeenBuilder movie(String title, int year, int plays, Date lastPlayed) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TITLE, title);
			movie.addProperty(POST_YEAR, year);
			movie.addProperty(POST_PLAYS, plays);
			movie.addProperty(POST_LAST_PLAYED, TraktApiBuilder.dateToUnixTimestamp(lastPlayed));
			this.movieList.add(movie);
			
			return this;
		}

		@Override
		protected void preFireCallback() {
			//Add the assembled movie list to the JSON post body.
			this.postParameter(POST_MOVIES, this.movieList);
		}
	}
	
	/**
	 * Add unwatched movies to your library.
	 * 
	 * @return Builder instance.
	 */
	public LibraryBuilder library() {
		return new LibraryBuilder(this);
	}
	
	public static final class LibraryBuilder extends TraktApiBuilder<Void> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TMDB_ID = "tmdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_MOVIES = "movies";
		
		private static final String URI = "/movie/library/" + FIELD_API_KEY;
		
		private final JsonArray movieList;
		
		private LibraryBuilder(MovieService service) {
			super(service, new TypeToken<Void>() {}, URI);
			
			this.movieList = new JsonArray();
		}

		/**
		 * Add a movie to the list of library movies.
		 * 
		 * @param imdbId IMDB ID for the movie.
		 * @return Builder instance.
		 */
		public LibraryBuilder movie(String imdbId, int plays, Date lastPlayed) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_IMDB_ID, imdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of library movies.
		 * 
		 * @param tmdbId TMDB ID for the movie.
		 * @return Builder instance.
		 */
		public LibraryBuilder movie(int tmdbId, int plays, Date lastPlayed) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TMDB_ID, tmdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of library movies.
		 * 
		 * @param title Movie title.
		 * @param year Movie year.
		 * @return Builder instance.
		 */
		public LibraryBuilder movie(String title, int year, int plays, Date lastPlayed) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TITLE, title);
			movie.addProperty(POST_YEAR, year);
			this.movieList.add(movie);
			
			return this;
		}

		@Override
		protected void preFireCallback() {
			//Add the assembled movie list to the JSON post body.
			this.postParameter(POST_MOVIES, this.movieList);
		}
	}
	
	/**
	 * Returns information for a movie including ratings and top watchers.
	 * 
	 * @param query Either the slug (i.e. the-social-network-2010), IMDB ID, or
	 * TMDB ID. You can get a movie's slug by browsing the website and looking
	 * at the URL when on a movie summary page.
	 * @return Builder instance.
	 */
	public SummaryBuilder summary(String query) {
		return new SummaryBuilder(this, query);
	}
	
	public static final class SummaryBuilder extends TraktApiBuilder<Movie> {
		private static final String URI = "/movie/summary.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;
		
		private SummaryBuilder(MovieService service, String query) {
			super(service, new TypeToken<Movie>() {}, URI);
			
			this.field(FIELD_QUERY, query);
		}
	}
	
	/**
	 * Remove movies from your library collection.
	 * 
	 * @return Builder instance.
	 */
	public UnlibraryBuilder unlibrary() {
		return new UnlibraryBuilder(this);
	}
	
	public static final class UnlibraryBuilder extends TraktApiBuilder<Void> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TMDB_ID = "tmdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_MOVIES = "movies";
		
		private static final String URI = "/movie/unlibrary/" + FIELD_API_KEY;
		
		private final JsonArray movieList;
		
		private UnlibraryBuilder(MovieService service) {
			super(service, new TypeToken<Void>() {}, URI);
			
			this.movieList = new JsonArray();
		}

		/**
		 * Add a movie to the list of library movies for removal.
		 * 
		 * @param imdbId IMDB ID for the movie.
		 * @return Builder instance.
		 */
		public UnlibraryBuilder movie(String imdbId) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_IMDB_ID, imdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of library movies for removal.
		 * 
		 * @param tmdbId TMDB ID for the movie.
		 * @return Builder instance.
		 */
		public UnlibraryBuilder movie(int tmdbId) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TMDB_ID, tmdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of library movies for removal.
		 * 
		 * @param title Movie title.
		 * @param year Movie year.
		 * @return Builder instance.
		 */
		public UnlibraryBuilder movie(String title, int year) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TITLE, title);
			movie.addProperty(POST_YEAR, year);
			this.movieList.add(movie);
			
			return this;
		}

		@Override
		protected void preFireCallback() {
			//Add the assembled movie list to the JSON post body.
			this.postParameter(POST_MOVIES, this.movieList);
		}
	}

	/**
	 * Remove movies watched outside of Trakt from your library.
	 * 
	 * @return Builder instance.
	 */
	public UnseenBuilder unseen() {
		return new UnseenBuilder(this);
	}
	
	public static final class UnseenBuilder extends TraktApiBuilder<Void> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TMDB_ID = "tmdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_MOVIES = "movies";
		
		private static final String URI = "/movie/unseen/" + FIELD_API_KEY;
		
		private final JsonArray movieList;
		
		private UnseenBuilder(MovieService service) {
			super(service, new TypeToken<Void>() {}, URI);
			
			this.movieList = new JsonArray();
		}

		/**
		 * Add a movie to the list of seen movies for removal.
		 * 
		 * @param imdbId IMDB ID for the movie.
		 * @return Builder instance.
		 */
		public UnseenBuilder movie(String imdbId) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_IMDB_ID, imdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of seen movies for removal.
		 * 
		 * @param tmdbId TMDB ID for the movie.
		 * @return Builder instance.
		 */
		public UnseenBuilder movie(int tmdbId) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TMDB_ID, tmdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of seen movies for removal.
		 * 
		 * @param title Movie title.
		 * @param year Movie year.
		 * @return Builder instance.
		 */
		public UnseenBuilder movie(String title, int year) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TITLE, title);
			movie.addProperty(POST_YEAR, year);
			this.movieList.add(movie);
			
			return this;
		}

		@Override
		protected void preFireCallback() {
			//Add the assembled movie list to the JSON post body.
			this.postParameter(POST_MOVIES, this.movieList);
		}
	}

	/**
	 * Remove one or more movies from your watchlist.
	 * 
	 * @return Builder instance.
	 */
	public UnwatchlistBuilder unwatchlist() {
		return new UnwatchlistBuilder(this);
	}
	
	public static final class UnwatchlistBuilder extends TraktApiBuilder<Void> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TMDB_ID = "tmdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_MOVIES = "movies";
		
		private static final String URI = "/movie/unwatchlist/" + FIELD_API_KEY;
		
		private final JsonArray movieList;
		
		private UnwatchlistBuilder(MovieService service) {
			super(service, new TypeToken<Void>() {}, URI);
			
			this.movieList = new JsonArray();
		}

		/**
		 * Add a movie to the list of watchlist movies for removal.
		 * 
		 * @param imdbId IMDB ID for the movie.
		 * @return Builder instance.
		 */
		public UnwatchlistBuilder movie(String imdbId) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_IMDB_ID, imdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of watchlist movies for removal.
		 * 
		 * @param tmdbId TMDB ID for the movie.
		 * @return Builder instance.
		 */
		public UnwatchlistBuilder movie(int tmdbId) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TMDB_ID, tmdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of watchlist movies for removal.
		 * 
		 * @param title Movie title.
		 * @param year Movie year.
		 * @return Builder instance.
		 */
		public UnwatchlistBuilder movie(String title, int year) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TITLE, title);
			movie.addProperty(POST_YEAR, year);
			this.movieList.add(movie);
			
			return this;
		}

		@Override
		protected void preFireCallback() {
			//Add the assembled movie list to the JSON post body.
			this.postParameter(POST_MOVIES, this.movieList);
		}
	}
	
	/**
	 * Notify trakt that a user has started watching a movie.
	 * 
	 * <p><em>Warning</em>: This method requires a developer API key.</p>
	 * 
	 * @param imdbId IMDB ID for the movie.
	 * @return Builder instance.
	 */
	public WatchingBuilder watching(String imdbId) {
		return new WatchingBuilder(this).imdbId(imdbId);
	}
	
	/**
	 * Notify trakt that a user has started watching a movie.
	 * 
	 * <p><em>Warning</em>: This method requires a developer API key.</p>
	 * 
	 * @param tmdbId TMDB (themoviedb.org) ID for the movie. 
	 * @return Builder instance.
	 */
	public WatchingBuilder watching(int tmdbId) {
		return new WatchingBuilder(this).tmdbId(tmdbId);
	}
	
	/**
	 * Notify trakt that a user has started watching a movie.
	 * 
	 * @param title Movie title.
	 * @param year Movie year.
	 * @return Builder instance.
	 */
	public WatchingBuilder watching(String title, int year) {
		return new WatchingBuilder(this).title(title).year(year);
	}
	
	public static final class WatchingBuilder extends TraktApiBuilder<Response> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TMDB_ID = "tmdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_DURATION = "duration";
		private static final String POST_PROGRESS = "progress";
		
		private static final String URI = "/movie/watching/" + FIELD_API_KEY;
		
		private WatchingBuilder(MovieService service) {
			super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
			this.includeDebugStrings();
		}
		/**
		 * IMDB ID for the movie.
		 * 
		 * @param imdbId Value.
		 * @return Builder instance.
		 */
		public WatchingBuilder imdbId(String imdbId) {
			this.postParameter(POST_IMDB_ID, imdbId);
			return this;
		}
		
		/**
		 * TMDB (themoviedb.org) ID for the movie.
		 * 
		 * @param tmdbId Value.
		 * @return Builder instance.
		 */
		public WatchingBuilder tmdbId(int tmdbId) {
			this.postParameter(POST_TMDB_ID, tmdbId);
			return this;
		}
		
		/**
		 * Movie title.
		 * 
		 * @param title Value.
		 * @return Builder instance.
		 */
		public WatchingBuilder title(String title) {
			this.postParameter(POST_TITLE, title);
			return this;
		}
		
		/**
		 * Movie year.
		 * 
		 * @param year Value.
		 * @return Builder instance.
		 */
		public WatchingBuilder year(int year) {
			this.postParameter(POST_YEAR, year);
			return this;
		}
		
		/**
		 * Duration (in minutes).
		 * 
		 * @param duration Value.
		 * @return Builder instance.
		 */
		public WatchingBuilder duration(int duration) {
			this.postParameter(POST_DURATION, duration);
			return this;
		}
		
		/**
		 * Percent progress (0-100). It is recommended to call the watching API
		 * every 15 minutes, then call the scrobble API near the end of the
		 * movie to lock it in.
		 * 
		 * @param progress Value.
		 * @return Builder instance.
		 */
		public WatchingBuilder progress(int progress) {
			this.postParameter(POST_PROGRESS, progress);
			return this;
		}

		@Override
		protected void performValidation() {
			assert this.hasPostParameter(POST_IMDB_ID)
			|| this.hasPostParameter(POST_TMDB_ID)
			|| (this.hasPostParameter(POST_TITLE) && this.hasPostParameter(POST_YEAR))
			: "Either IMDB ID, TMDB ID, or both title and year is required.";
			assert this.hasPostParameter(POST_DURATION) : "Duration is required.";
			assert this.hasPostParameter(POST_PROGRESS) : "Progress is required.";
		}
	}
	
	/**
	 * Returns a array of all users watching a movie.
	 * 
	 * @param query Either the slug (i.e. the-social-network-2010), IMDB ID, or
	 * TMDB ID. You can get a movie's slug by browsing the website and looking
	 * at the URL when on a movie summary page.
	 * @return Builder instance.
	 */
	public WatchingNowBuilder watchingNow(String query) {
		return new WatchingNowBuilder(this, query);
	}
	
	public static final class WatchingNowBuilder extends TraktApiBuilder<List<UserProfile>> {
		private static final String URI = "/movie/watchingnow.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;
		
		private WatchingNowBuilder(MovieService service, String query) {
			super(service, new TypeToken<List<UserProfile>>() {}, URI);
			
			this.field(FIELD_QUERY, query);
		}
	}

	/**
	 * Add one or more movies to your watchlist.
	 * 
	 * @return Builder instance.
	 */
	public WatchlistBuilder watchlist() {
		return new WatchlistBuilder(this);
	}
	
	public static final class WatchlistBuilder extends TraktApiBuilder<Void> {
		private static final String POST_IMDB_ID = "imdb_id";
		private static final String POST_TMDB_ID = "tmdb_id";
		private static final String POST_TITLE = "title";
		private static final String POST_YEAR = "year";
		private static final String POST_MOVIES = "movies";
		
		private static final String URI = "/movie/watchlist/" + FIELD_API_KEY;
		
		private final JsonArray movieList;
		
		private WatchlistBuilder(MovieService service) {
			super(service, new TypeToken<Void>() {}, URI);
			
			this.movieList = new JsonArray();
		}

		/**
		 * Add a movie to the list of library movies for removal.
		 * 
		 * @param imdbId IMDB ID for the movie.
		 * @return Builder instance.
		 */
		public WatchlistBuilder movie(String imdbId) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_IMDB_ID, imdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of library movies for removal.
		 * 
		 * @param tmdbId TMDB ID for the movie.
		 * @return Builder instance.
		 */
		public WatchlistBuilder movie(int tmdbId) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TMDB_ID, tmdbId);
			this.movieList.add(movie);
			
			return this;
		}

		/**
		 * Add a movie to the list of library movies for removal.
		 * 
		 * @param title Movie title.
		 * @param year Movie year.
		 * @return Builder instance.
		 */
		public WatchlistBuilder movie(String title, int year) {
			JsonObject movie = new JsonObject();
			movie.addProperty(POST_TITLE, title);
			movie.addProperty(POST_YEAR, year);
			this.movieList.add(movie);
			
			return this;
		}

		@Override
		protected void preFireCallback() {
			//Add the assembled movie list to the JSON post body.
			this.postParameter(POST_MOVIES, this.movieList);
		}
	}

	/**
	 * Returns all shouts for a movie. Most recent shouts returned first.
	 * 
	 * @param titleOrImdbId Movie title or IMDB ID.
	 * @return Builder instance.
	 */
	public ShoutsBuilder shouts(String titleOrImdbId) {
		return new ShoutsBuilder(this).title(titleOrImdbId);
	}
	
	/**
	 * Returns all shouts for a movie. Most recent shouts returned first.
	 * 
	 * @param tmdbId TMDB ID.
	 * @return Builder instance.
	 */
	public ShoutsBuilder shouts(int tmdbId) {
		return new ShoutsBuilder(this).title(tmdbId);
	}
	
	public static final class ShoutsBuilder extends TraktApiBuilder<List<Shout>> {
		private static final String URI = "/movie/shouts.json/" + FIELD_API_KEY + "/" + FIELD_TITLE;
		
		private ShoutsBuilder(MovieService service) {
			super(service, new TypeToken<List<Shout>>() {}, URI);
		}
		
		/**
		 * Set show title or IMDB ID.
		 * 
		 * @param titleOrImdbId Value.
		 * @return Builder instance.
		 */
		public ShoutsBuilder title(String titleOrImdbId) {
			this.field(FIELD_TITLE, titleOrImdbId);
			return this;
		}
		
		/**
		 * Set show TMDB ID.
		 * 
		 * @param tmdbId Value.
		 * @return Builder instance.
		 */
		public ShoutsBuilder title(int tmdbId) {
			this.field(FIELD_TITLE, tmdbId);
			return this;
		}
	}
}
