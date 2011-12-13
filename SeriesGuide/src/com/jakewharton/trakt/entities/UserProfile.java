package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.Gender;

import java.util.Calendar;
import java.util.List;

public class UserProfile implements TraktEntity {
    private static final long serialVersionUID = -4145012978937162733L;

    public static class Stats implements TraktEntity {
        private static final long serialVersionUID = -2737256634772977389L;

        public static class Shows implements TraktEntity {
            private static final long serialVersionUID = -2888630218268563052L;

            public Integer library;

            /** @deprecated Use {@link #library} */
            @Deprecated
            public Integer getLibrary() {
                return this.library;
            }
        }
        public static class Episodes implements TraktEntity {
            private static final long serialVersionUID = 7210925664642958187L;

            public Integer watched;
            @SerializedName("watched_unique") public Integer watchedUnique;
            @SerializedName("watched_trakt") public Integer watchedTrakt;
            @SerializedName("watched_trakt_unique") public Integer watchedTraktUnique;
            @SerializedName("watched_elsewhere") public Integer watchedElsewhere;
            public Integer unwatched;

            /** @deprecated Use {@link #watched} */
            @Deprecated
            public Integer getWatched() {
                return this.watched;
            }
            /** @deprecated Use {@link #watchedUnique} */
            @Deprecated
            public Integer getWatchedUnique() {
                return this.watchedUnique;
            }
            /** @deprecated Use {@link #watchedTrakt} */
            @Deprecated
            public Integer getWatchedTrakt() {
                return this.watchedTrakt;
            }
            /** @deprecated Use {@link #watchedTraktUnique} */
            @Deprecated
            public Integer getWatchedTraktUnique() {
                return this.watchedTraktUnique;
            }
            /** @deprecated Use {@link #watchedElsewhere} */
            @Deprecated
            public Integer getWatchedElsewhere() {
                return this.watchedElsewhere;
            }
            /** @deprecated Use {@link #unwatched} */
            @Deprecated
            public Integer getUnwatched() {
                return this.unwatched;
            }
        }
        public static class Movies implements TraktEntity {
            private static final long serialVersionUID = 5061541628681754141L;

            public Integer watched;
            @SerializedName("watched_unique") public Integer watchedUnique;
            @SerializedName("watched_trakt") public Integer watchedTrakt;
            @SerializedName("watched_trakt_unique") public Integer watchedTraktUnique;
            @SerializedName("watched_elsewhere") public Integer watchedElsewhere;
            public Integer library;
            public Integer unwatched;

            /** @deprecated Use {@link #watched} */
            @Deprecated
            public Integer getWatched() {
                return this.watched;
            }
            /** @deprecated Use {@link #watchedUnique} */
            @Deprecated
            public Integer getWatchedUnique() {
                return this.watchedUnique;
            }
            /** @deprecated Use {@link #watchedTrakt} */
            @Deprecated
            public Integer getWatchedTrakt() {
                return this.watchedTrakt;
            }
            /** @deprecated Use {@link #watchedTraktUnique} */
            @Deprecated
            public Integer getWatchedTraktUnique() {
                return this.watchedTraktUnique;
            }
            /** @deprecated Use {@link #watchedElsewhere} */
            @Deprecated
            public Integer getWatchedElsewhere() {
                return this.watchedElsewhere;
            }
            /** @deprecated Use {@link #library} */
            @Deprecated
            public Integer getLibrary() {
                return this.library;
            }
            /** @deprecated Use {@link #unwatched} */
            @Deprecated
            public Integer getUnwatched() {
                return this.unwatched;
            }
        }

        public Integer friends;
        public Shows shows;
        public Episodes episodes;
        public Movies movies;

        /** @deprecated Use {@link #friends} */
        @Deprecated
        public Integer getFriends() {
            return this.friends;
        }
        /** @deprecated Use {@link #shows} */
        @Deprecated
        public Shows getShows() {
            return this.shows;
        }
        /** @deprecated Use {@link #episodes} */
        @Deprecated
        public Episodes getEpisodes() {
            return this.episodes;
        }
        /** @deprecated Use {@link #movies} */
        @Deprecated
        public Movies getMovies() {
            return this.movies;
        }
    }

    public String username;
    @SerializedName("protected") public Boolean _protected;
    @SerializedName("full_name") public String fullName;
    public Gender gender;
    public Integer age;
    public String location;
    public String about;
    public Calendar joined;
    public String avatar;
    public String url;
    public Stats stats;
    public ActivityItemBase watching;
    public List<ActivityItem> watched;
    public Integer plays;
    public TvShowEpisode episode;
    public Calendar approved;

    /** @deprecated Use {@link #username} */
    @Deprecated
    public String getUsername() {
        return this.username;
    }
    /** @deprecated Use {@link #_protected} */
    @Deprecated
    public Boolean getProtected() {
        return this._protected;
    }
    /** @deprecated Use {@link #fullName} */
    @Deprecated
    public String getFullName() {
        return this.fullName;
    }
    /** @deprecated Use {@link #gender} */
    @Deprecated
    public Gender getGender() {
        return this.gender;
    }
    /** @deprecated Use {@link #age} */
    @Deprecated
    public Integer getAge() {
        return this.age;
    }
    /** @deprecated Use {@link #location} */
    @Deprecated
    public String getLocation() {
        return this.location;
    }
    /** @deprecated Use {@link #about} */
    @Deprecated
    public String getAbout() {
        return this.about;
    }
    /** @deprecated Use {@link #joined} */
    @Deprecated
    public Calendar getJoined() {
        return this.joined;
    }
    /** @deprecated Use {@link #avatar} */
    @Deprecated
    public String getAvatar() {
        return this.avatar;
    }
    /** @deprecated Use {@link #url} */
    @Deprecated
    public String getUrl() {
        return this.url;
    }
    /** @deprecated Use {@link #stats} */
    @Deprecated
    public Stats getStats() {
        return this.stats;
    }
    /** @deprecated Use {@link #watching} */
    @Deprecated
    public ActivityItemBase getWatching() {
        return this.watching;
    }
    /** @deprecated Use {@link #watched} */
    @Deprecated
    public List<ActivityItem> getWatched() {
        return this.watched;
    }
    /** @deprecated Use {@link #plays} */
    @Deprecated
    public Integer getPlays() {
        return this.plays;
    }
    /** @deprecated Use {@link #episode} */
    @Deprecated
    public TvShowEpisode getEpisode() {
        return this.episode;
    }
    /** @deprecated Use {@link #approved} */
    @Deprecated
    public Calendar getApproved() {
        return this.approved;
    }
}
