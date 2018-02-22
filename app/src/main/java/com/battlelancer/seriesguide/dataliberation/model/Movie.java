package com.battlelancer.seriesguide.dataliberation.model;

import com.google.gson.annotations.SerializedName;

public class Movie {

    @SerializedName("tmdb_id")
    public int tmdbId;

    @SerializedName("imdb_id")
    public String imdbId;

    public String title;

    @SerializedName("released_utc_ms")
    public long releasedUtcMs;

    @SerializedName("runtime_min")
    public int runtimeMin;

    public String poster;

    public String overview;

    @SerializedName("in_collection")
    public boolean inCollection;

    @SerializedName("in_watchlist")
    public boolean inWatchlist;

    public boolean watched;

}
