
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

public class Episode {

    @SerializedName("tvdb_id")
    public int tvdbId;

    public int episode;

    @SerializedName("episode_absolute")
    public int episodeAbsolute;

    public String title;

    @SerializedName("first_aired")
    public long firstAired;

    public boolean watched;

    public boolean collected;

    @SerializedName("imdb_id")
    public String imdbId;

    /*
     * Full dump only follows.
     */

    @SerializedName("episode_dvd")
    public double episodeDvd;

    public String overview;

    public String image;

    public String writers;

    public String gueststars;

    public String directors;

    public double rating;

    @SerializedName("last_edited")
    public long lastEdited;

}
