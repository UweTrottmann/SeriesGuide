package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.DayOfTheWeek;

import java.util.Date;
import java.util.List;

public class TvShow extends MediaBase implements TraktEntity {
    private static final long serialVersionUID = 862473930551420996L;

    @SerializedName("first_aired") public Date firstAired;
    public String country;
    public String overview;
    public Integer runtime;
    public String network;
    @SerializedName("air_day") public DayOfTheWeek airDay;
    @SerializedName("air_time") public String airTime;
    public String certification; //TODO: enum
    @SerializedName("tvdb_id") public String tvdbId;
    @SerializedName("tvrage_id") public String tvrageId;
    public List<TvShowEpisode> episodes;
    @SerializedName("top_episodes") public List<TvShowEpisode> topEpisodes;
    public List<TvShowSeason> seasons;

    /** @deprecated Use {@link #firstAired} */
    @Deprecated
    public Date getFirstAired() {
        return this.firstAired;
    }
    /** @deprecated Use {@link #country} */
    @Deprecated
    public String getCountry() {
        return this.country;
    }
    /** @deprecated Use {@link #overview} */
    @Deprecated
    public String getOverview() {
        return this.overview;
    }
    /** @deprecated Use {@link #runtime} */
    @Deprecated
    public Integer getRuntime() {
        return this.runtime;
    }
    /** @deprecated Use {@link #network} */
    @Deprecated
    public String getNetwork() {
        return this.network;
    }
    /** @deprecated Use {@link #airDay} */
    @Deprecated
    public DayOfTheWeek getAirDay() {
        return this.airDay;
    }
    /** @deprecated Use {@link #airTime} */
    @Deprecated
    public String getAirTime() {
        return this.airTime;
    }
    /** @deprecated Use {@link #certification} */
    @Deprecated
    public String getCertification() {
        return this.certification;
    }
    /** @deprecated Use {@link #tvdbId} */
    @Deprecated
    public String getTvdbId() {
        return this.tvdbId;
    }
    /** @deprecated Use {@link #tvrageId} */
    @Deprecated
    public String getTvRageId() {
        return this.tvrageId;
    }
    /** @deprecated Use {@link #episodes} */
    @Deprecated
    public List<TvShowEpisode> getEpisodes() {
        return this.episodes;
    }
    /** @deprecated Use {@link #topEpisodes} */
    @Deprecated
    public List<TvShowEpisode> getTopEpisodes() {
        return this.topEpisodes;
    }
    /** @deprecated Use {@link #seasons} */
    @Deprecated
    public List<TvShowSeason> getSeasons() {
        return this.seasons;
    }
}
