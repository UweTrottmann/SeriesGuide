package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.Rating;

import java.util.List;

public abstract class MediaBase implements TraktEntity {
    private static final long serialVersionUID = 753880113366868498L;

    public static class Stats implements TraktEntity {
        private static final long serialVersionUID = -5436127125832664020L;

        public Integer watchers;
        public Integer plays;

        /** @deprecated Use {@link #watchers} */
        @Deprecated
        public Integer getWatchers() {
            return this.watchers;
        }
        /** @deprecated Use {@link #plays} */
        @Deprecated
        public Integer getPlays() {
            return this.plays;
        }
    }

    public String title;
    public Integer year;
    public String url;
    public Images images;
    @SerializedName("top_watchers") public List<UserProfile> topWatchers;
    public Ratings ratings;
    public Stats stats;
    @SerializedName("imdb_id") public String imdbId;
    public Rating rating;
    @SerializedName("in_watchlist") public Boolean inWatchlist;

    /** @deprecated Use {@link #title} */
    @Deprecated
    public String getTitle() {
        return this.title;
    }
    /** @deprecated Use {@link #year} */
    @Deprecated
    public Integer getYear() {
        return this.year;
    }
    /** @deprecated Use {@link #url} */
    @Deprecated
    public String getUrl() {
        return this.url;
    }
    /** @deprecated Use {@link #images} */
    @Deprecated
    public Images getImages() {
        return this.images;
    }
    /** @deprecated Use {@link #topWatchers} */
    @Deprecated
    public List<UserProfile> getTopWatchers() {
        return this.topWatchers;
    }
    /** @deprecated Use {@link #ratings} */
    @Deprecated
    public Ratings getRatings() {
        return this.ratings;
    }
    /** @deprecated Use {@link #stats} */
    @Deprecated
    public Stats getStats() {
        return this.stats;
    }
    /** @deprecated Use {@link #imdbId} */
    @Deprecated
    public String getImdbId() {
        return this.imdbId;
    }
    /** @deprecated Use {@link #rating} */
    @Deprecated
    public Rating getRating() {
        return this.rating;
    }
    /** @deprecated Use {@link #inWatchlist} */
    @Deprecated
    public Boolean getInWatchlist() {
        return this.inWatchlist;
    }
}
