package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.Gender;

import java.util.Calendar;
import java.util.List;

public final class UserProfile implements TraktEntity {
	private static final long serialVersionUID = -4145012978937162733L;
	
	public static final class Stats implements TraktEntity {
		private static final long serialVersionUID = -2737256634772977389L;
		
		public static final class Shows implements TraktEntity {
			private static final long serialVersionUID = -2888630218268563052L;
			
			private Integer library;
			
			public Integer getLibrary() {
				return this.library;
			}
		}
		public static final class Episodes implements TraktEntity {
			private static final long serialVersionUID = 7210925664642958187L;
			
			private Integer watched;
			@SerializedName("watched_unique") private Integer watchedUnique;
			@SerializedName("watched_trakt") private Integer watchedTrakt;
			@SerializedName("watched_trakt_unique") private Integer watchedTraktUnique;
			@SerializedName("watched_elsewhere") private Integer watchedElsewhere;
			private Integer unwatched;
			
			public Integer getWatched() {
				return this.watched;
			}
			public Integer getWatchedUnique() {
				return this.watchedUnique;
			}
			public Integer getWatchedTrakt() {
				return this.watchedTrakt;
			}
			public Integer getWatchedTraktUnique() {
				return this.watchedTraktUnique;
			}
			public Integer getWatchedElsewhere() {
				return this.watchedElsewhere;
			}
			public Integer getUnwatched() {
				return this.unwatched;
			}
		}
		public static final class Movies implements TraktEntity {
			private static final long serialVersionUID = 5061541628681754141L;
			
			private Integer watched;
			@SerializedName("watched_unique") private Integer watchedUnique;
			@SerializedName("watched_trakt") private Integer watchedTrakt;
			@SerializedName("watched_trakt_unique") private Integer watchedTraktUnique;
			@SerializedName("watched_elsewhere") private Integer watchedElsewhere;
			private Integer library;
			private Integer unwatched;
			
			public Integer getWatched() {
				return this.watched;
			}
			public Integer getWatchedUnique() {
				return this.watchedUnique;
			}
			public Integer getWatchedTrakt() {
				return this.watchedTrakt;
			}
			public Integer getWatchedTraktUnique() {
				return this.watchedTraktUnique;
			}
			public Integer getWatchedElsewhere() {
				return this.watchedElsewhere;
			}
			public Integer getLibrary() {
				return this.library;
			}
			public Integer getUnwatched() {
				return this.unwatched;
			}
		}
		
		private Integer friends;
		private Shows shows;
		private Episodes episodes;
		private Movies movies;
		
		public Integer getFriends() {
			return this.friends;
		}
		public Shows getShows() {
			return this.shows;
		}
		public Episodes getEpisodes() {
			return this.episodes;
		}
		public Movies getMovies() {
			return this.movies;
		}
	}

	private String username;
	@SerializedName("protected") private Boolean _protected;
	@SerializedName("full_name") private String fullName;
	private Gender gender;
	private Integer age;
	private String location;
	private String about;
	private Calendar joined;
	private String avatar;
	private String url;
	private Stats stats;
	private WatchedMediaEntity watching;
	private List<MediaEntity> watched;
	private Integer plays;
	private TvShowEpisode episode;
	private Calendar approved;
	
	public String getUsername() {
		return this.username;
	}
	public Boolean getProtected() {
		return this._protected;
	}
	public String getFullName() {
		return this.fullName;
	}
	public Gender getGender() {
		return this.gender;
	}
	public Integer getAge() {
		return this.age;
	}
	public String getLocation() {
		return this.location;
	}
	public String getAbout() {
		return this.about;
	}
	public Calendar getJoined() {
		return this.joined;
	}
	public String getAvatar() {
		return this.avatar;
	}
	public String getUrl() {
		return this.url;
	}
	public Stats getStats() {
		return this.stats;
	}
	public MediaEntity getWatching() {
		return this.watching;
	}
	public List<MediaEntity> getWatched() {
		return this.watched;
	}
	public Integer getPlays() {
		return this.plays;
	}
	public TvShowEpisode getEpisode() {
		return this.episode;
	}
	public Calendar getApproved() {
		return this.approved;
	}
}
