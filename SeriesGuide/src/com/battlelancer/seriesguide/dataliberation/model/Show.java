
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

import java.util.List;

public class Show {
    @SerializedName("tvdb_id")
    public int tvdbId;

    public String title;

    public List<Season> seasons;

    public boolean favorite;

    public boolean hidden;

    public boolean sync;

    public long airtime;

    public String airday;

    @SerializedName("check_in_getglue_id")
    public String checkInGetGlueId;

    @SerializedName("last_watched_episode")
    public int lastWatchedEpisode;

    public String poster;

    @SerializedName("content_rating")
    public String contentRating;

    public String status;

    public int runtime;

    public String network;

    @SerializedName("imdb_id")
    public String imdbId;

    @SerializedName("first_aired")
    public String firstAired;

    /*
     * Full dump only following.
     */

    public String overview;

    public double rating;

    public String genres;

    public String actors;

    @SerializedName("last_updated")
    public long lastUpdated;

    @SerializedName("last_edited")
    public long lastEdited;
}
