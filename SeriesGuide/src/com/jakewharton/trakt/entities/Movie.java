package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;

import java.util.Date;

public class Movie extends MediaBase implements TraktEntity {
    private static final long serialVersionUID = -1543214252495012419L;

    @SerializedName("tmdb_id") public String tmdbId;
    public Integer plays;
    @SerializedName("in_collection") public Boolean inCollection;
    public Date released;
    public String trailer;
    public Integer runtime;
    public String tagline;
    public String overview;
    public String certification; //TODO make enum
    public Boolean watched;

    /** @deprecated Use {@link #tmdbId} */
    @Deprecated
    public String getTmdbId() {
        return this.tmdbId;
    }
    /** @deprecated Use {@link #plays} */
    @Deprecated
    public Integer getPlays() {
        return this.plays;
    }
    /** @deprecated Use {@link #inCollection} */
    @Deprecated
    public Boolean getInCollection() {
        return this.inCollection;
    }
    /** @deprecated Use {@link #released} */
    @Deprecated
    public Date getReleased() {
        return this.released;
    }
    /** @deprecated Use {@link #trailer} */
    @Deprecated
    public String getTrailer() {
        return this.trailer;
    }
    /** @deprecated Use {@link #runtime} */
    @Deprecated
    public Integer getRuntime() {
        return this.runtime;
    }
    /** @deprecated Use {@link #tagline} */
    @Deprecated
    public String getTagline() {
        return this.tagline;
    }
    /** @deprecated Use {@link #overview} */
    @Deprecated
    public String getOverview() {
        return this.overview;
    }
    /** @deprecated Use {@link #certification} */
    @Deprecated
    public String getCertification() {
        return this.certification;
    }
    /** @deprecated Use {@link #watched} */
    @Deprecated
    public Boolean getWatched() {
        return this.watched;
    }
}
