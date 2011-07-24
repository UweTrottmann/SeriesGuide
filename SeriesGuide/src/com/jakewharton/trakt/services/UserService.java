package com.jakewharton.trakt.services;

import java.util.Date;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.CalendarDate;
import com.jakewharton.trakt.entities.MediaEntity;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.UserProfile;

public final class UserService extends TraktApiService {
	/**
	 * Returns a users shows airing during the time period specified. Protected
	 * users won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public CalendarShowsBuilder calendarShows(String username) {
		return new CalendarShowsBuilder(this, username);
	}
	
	public static final class CalendarShowsBuilder extends TraktApiBuilder<List<CalendarDate>> {
		private static final int DEFAULT_DAYS = 7;
		private static final String URI = "/user/calendar/shows.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME + "/" + FIELD_DATE + "/" + FIELD_DAYS;
		
		private CalendarShowsBuilder(UserService service, String username) {
			super(service, new TypeToken<List<CalendarDate>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
		
		/**
		 * Start date for the calendar.
		 * 
		 * @param date Value.
		 * @return Builder instance.
		 */
		public CalendarShowsBuilder date(Date date) {
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
		public CalendarShowsBuilder days(int days) {
			this.field(FIELD_DAYS, days);
			
			if (!this.hasField(FIELD_DATE)) {
				//Set default.
				this.field(FIELD_DATE, new Date());
			}
			
			return this;
		}
	}
	
	/**
	 * Returns the TV show episode or movie a user is currently watching. If
	 * they aren't watching anything, a blank object will be returned.
	 * Protected users won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public WatchingBuilder watching(String username) {
		return new WatchingBuilder(this, username);
	}
	
	public static final class WatchingBuilder extends TraktApiBuilder<MediaEntity> {
		private static final String URI = "/user/watching.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private WatchingBuilder(UserService service, String username) {
			super(service, new TypeToken<MediaEntity>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}
	
	/**
	 * Returns the last 100 TV show episodes and movies a user has watched
	 * using Trakt. Protected users won't return any data unless you are
	 * friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public WatchedBuilder watched(String username) {
		return new WatchedBuilder(this, username);
	}
	
	public static final class WatchedBuilder extends TraktApiBuilder<List<MediaEntity>> {
		private static final String URI = "/user/watched.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private WatchedBuilder(UserService service, String username) {
			super(service, new TypeToken<List<MediaEntity>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}
	
	/**
	 * Returns the last 100 TV show episodes a user has watched using Trakt.
	 * Protected users won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public WatchedEpisodesBuilder watchedEpisodes(String username) {
		return new WatchedEpisodesBuilder(this, username);
	}
	
	public static final class WatchedEpisodesBuilder extends TraktApiBuilder<List<MediaEntity>> {
		private static final String URI = "/user/watched/episodes.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private WatchedEpisodesBuilder(UserService service, String username) {
			super(service, new TypeToken<List<MediaEntity>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns the last 100 movies a user has watched using Trakt. Protected
	 * users won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public WatchedMoviesBuilder watchedMovies(String username) {
		return new WatchedMoviesBuilder(this, username);
	}
	
	public static final class WatchedMoviesBuilder extends TraktApiBuilder<List<MediaEntity>> {
		private static final String FIELD_USERNAME = "username";
		
		private static final String URI = "/user/watched/movies.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private WatchedMoviesBuilder(UserService service, String username) {
			super(service, new TypeToken<List<MediaEntity>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all episodes in a user's watchlist. Each show will have its own
	 * entry and will contain all episodes in the watchlist. Protected users
	 * won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public WatchlistEpisodesBuilder watchlistEpisodes(String username) {
		return new WatchlistEpisodesBuilder(this, username);
	}
	
	public static final class WatchlistEpisodesBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/user/watchlist/episodes.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private WatchlistEpisodesBuilder(UserService service, String username) {
			super(service, new TypeToken<List<TvShow>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all movies in a user's watchlist. Each movie will indicate when
	 * it was added to the watchlist. Protected users won't return any data
	 * unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public WatchlistMoviesBuilder watchlistMovies(String username) {
		return new WatchlistMoviesBuilder(this, username);
	}
	
	public static final class WatchlistMoviesBuilder extends TraktApiBuilder<List<Movie>> {
		private static final String URI = "/user/watchlist/movies.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private WatchlistMoviesBuilder(UserService service, String username) {
			super(service, new TypeToken<List<Movie>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all shows in a user's watchlist. Each show will indicate when it
	 * was added to the watchlist. Protected users won't return any data unless
	 * you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public WatchlistShowsBuilder watchlistShows(String username) {
		return new WatchlistShowsBuilder(this, username);
	}
	
	public static final class WatchlistShowsBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/user/watchlist/shows.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private WatchlistShowsBuilder(UserService service, String username) {
			super(service, new TypeToken<List<TvShow>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}
	
	/**
	 * Returns profile information for a user. Protected users won't return any
	 * data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public ProfileBuilder profile(String username) {
		return new ProfileBuilder(this, username);
	}
	
	public static final class ProfileBuilder extends TraktApiBuilder<UserProfile> {
		private static final String URI = "/user/profile.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private ProfileBuilder(UserService service, String username) {
			super(service, new TypeToken<UserProfile>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}
	
	/**
	 * Returns an array of the user's friends. Each friend has the detailed
	 * profile including what they are currently watching and their recent
	 * watches. Protected users won't return any data unless you are friends.
	 * Any friends of the main user that are protected won't display data
	 * either.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public FriendsBuilder friends(String username) {
		return new FriendsBuilder(this, username);
	}
	
	public static final class FriendsBuilder extends TraktApiBuilder<List<UserProfile>> {
		private static final String URI = "/user/friends.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private FriendsBuilder(UserService service, String username) {
			super(service, new TypeToken<List<UserProfile>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all movies in a user's library. Each movie will indicate if it's
	 * in the user's collection and how many plays it has. Protected users
	 * won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryMoviesAllBuilder libraryMoviesAll(String username) {
		return new LibraryMoviesAllBuilder(this, username);
	}
	
	public static final class LibraryMoviesAllBuilder extends TraktApiBuilder<List<Movie>> {
		private static final String URI = "/user/library/movies/all.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryMoviesAllBuilder(UserService service, String username) {
			super(service, new TypeToken<List<Movie>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all movies in a user's library collection. Collection items
	 * might include Blu-rays, DVDs, and digital downloads. Protected users
	 * won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryMoviesCollectionBuilder libraryMoviesCollection(String username) {
		return new LibraryMoviesCollectionBuilder(this, username);
	}
	
	public static final class LibraryMoviesCollectionBuilder extends TraktApiBuilder<List<Movie>> {
		private static final String URI = "/user/library/movies/collection.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryMoviesCollectionBuilder(UserService service, String username) {
			super(service, new TypeToken<List<Movie>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all movies a user has hated. Protected users won't return any
	 * data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryMoviesHatedBuilder libraryMoviesHated(String username) {
		return new LibraryMoviesHatedBuilder(this, username);
	}
	
	public static final class LibraryMoviesHatedBuilder extends TraktApiBuilder<List<Movie>> {
		private static final String URI = "/user/library/movies/hated.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryMoviesHatedBuilder(UserService service, String username) {
			super(service, new TypeToken<List<Movie>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all movies a user has loved. Protected users won't return any
	 * data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryMoviesLovedBuilder libraryMoviesLoved(String username) {
		return new LibraryMoviesLovedBuilder(this, username);
	}
	
	public static final class LibraryMoviesLovedBuilder extends TraktApiBuilder<List<Movie>> {
		private static final String URI = "/user/library/movies/loved.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryMoviesLovedBuilder(UserService service, String username) {
			super(service, new TypeToken<List<Movie>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all shows in a user's library. Each show will indicate how many
	 * plays it has. Protected users won't return any data unless you are
	 * friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryShowsAllBuilder libraryShowsAll(String username) {
		return new LibraryShowsAllBuilder(this, username);
	}
	
	public static final class LibraryShowsAllBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/user/library/shows/all.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryShowsAllBuilder(UserService service, String username) {
			super(service, new TypeToken<List<TvShow>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all shows and episodes in a user's library collection.
	 * Collection items might include Blu-rays, DVDs, and digital downloads.
	 * Protected users won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryShowsCollectionBuilder libraryShowsCollection(String username) {
		return new LibraryShowsCollectionBuilder(this, username);
	}
	
	public static final class LibraryShowsCollectionBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/user/library/shows/collection.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryShowsCollectionBuilder(UserService service, String username) {
			super(service, new TypeToken<List<TvShow>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all shows a user has hated. Protected users won't return any
	 * data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryShowsHatedBuilder libraryShowsHated(String username) {
		return new LibraryShowsHatedBuilder(this, username);
	}
	
	public static final class LibraryShowsHatedBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/user/library/shows/hated.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryShowsHatedBuilder(UserService service, String username) {
			super(service, new TypeToken<List<TvShow>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all shows a user has loved. Protected users won't return any
	 * data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryShowsLovedBuilder libraryShowsLoved(String username) {
		return new LibraryShowsLovedBuilder(this, username);
	}
	
	public static final class LibraryShowsLovedBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/user/library/shows/loved.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryShowsLovedBuilder(UserService service, String username) {
			super(service, new TypeToken<List<TvShow>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}

	/**
	 * Returns all shows and episodes that a user has watched. This method is
	 * useful to sync trakt's data with local media center. Protected users
	 * won't return any data unless you are friends.
	 * 
	 * @param username You can get a username by browsing the website and
	 * looking at the URL when on a profile page.
	 * @return Builder instance.
	 */
	public LibraryShowsWatchedBuilder libraryShowsWatched(String username) {
		return new LibraryShowsWatchedBuilder(this, username);
	}
	
	public static final class LibraryShowsWatchedBuilder extends TraktApiBuilder<List<TvShow>> {
		private static final String URI = "/user/library/shows/watched.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME;
		
		private LibraryShowsWatchedBuilder(UserService service, String username) {
			super(service, new TypeToken<List<TvShow>>() {}, URI);
			
			this.field(FIELD_USERNAME, username);
		}
	}
}
