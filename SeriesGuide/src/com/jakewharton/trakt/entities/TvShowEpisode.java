package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.Rating;

import java.util.Calendar;
import java.util.Date;

public class TvShowEpisode implements TraktEntity {
    private static final long serialVersionUID = -1550739539663499211L;

    public Integer season;
    public Integer number;
    public String title;
    public String overview;
    public String url;
    @SerializedName("first_aired") public Date firstAired;
    public Calendar inserted;
    public Integer plays;
    public Images images;
    public Ratings ratings;
    public Boolean watched;
    public Rating rating;
    @SerializedName("in_watchlist") public Boolean inWatchlist;

    /** @deprecated Use {@link #season} */
    @Deprecated
    public Integer getSeason() {
        return this.season;
    }
    /** @deprecated Use {@link #number} */
    @Deprecated
    public Integer getNumber() {
        return this.number;
    }
    /** @deprecated Use {@link #title} */
    @Deprecated
    public String getTitle() {
        return this.title;
    }
    /** @deprecated Use {@link #overview} */
    @Deprecated
    public String getOverview() {
        return this.overview;
    }
    /** @deprecated Use {@link #url} */
    @Deprecated
    public String getUrl() {
        return this.url;
    }
    /** @deprecated Use {@link #firstAired} */
    @Deprecated
    public Date getFirstAired() {
        return this.firstAired;
    }
    /** @deprecated Use {@link #inserted} */
    @Deprecated
    public Calendar getInserted() {
        return this.inserted;
    }
    /** @deprecated Use {@link #plays} */
    @Deprecated
    public Integer getPlays() {
        return this.plays;
    }
    /** @deprecated Use {@link #images} */
    @Deprecated
    public Images getImages() {
        return this.images;
    }
    /** @deprecated Use {@link #ratings} */
    @Deprecated
    public Ratings getRatings() {
        return this.ratings;
    }
    /** @deprecated Use {@link #watched} */
    @Deprecated
    public Boolean getWatched() {
        return this.watched;
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
