package com.jakewharton.trakt.entities;

import java.util.Date;
import java.util.List;
import com.jakewharton.trakt.TraktEntity;

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
			private Integer watchedUnique;
			private Integer watchedTrakt;
			private Integer watchedTraktUnique;
			private Integer watchedElsewhere;
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
			private Integer watchedUnique;
			private Integer watchedTrakt;
			private Integer watchedTraktUnique;
			private Integer watchedElsewhere;
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
	private Boolean _protected;
	private String fullName;
	private String gender; //TODO: enum
	private Integer age;
	private String location;
	private String about;
	private Date joined;
	private String avatar;
	private String url;
	private Stats stats;
	private MediaEntity watching;
	private List<MediaEntity> watched;
	private Integer plays;
	private TvShowEpisode episode;
	
	public String getUsername() {
		return this.username;
	}
	public Boolean get_protected() {
		return this._protected;
	}
	public String getFullName() {
		return this.fullName;
	}
	public String getGender() {
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
	public Date getJoined() {
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
}
