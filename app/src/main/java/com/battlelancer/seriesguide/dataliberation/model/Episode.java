
package com.battlelancer.seriesguide.dataliberation.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class Episode {

    /**
     * May be null for legacy backups.
     */
    @Nullable public Integer tmdb_id;
    /**
     * Is null on new backups.
     */
    @Nullable
    @SerializedName("tvdb_id")
    public Integer tvdbId;

    public int episode;

    @Nullable
    @SerializedName("episode_absolute")
    public Integer episodeAbsolute;

    @Nullable
    public String title;

    @SerializedName("first_aired")
    public long firstAired;

    public boolean watched;

    public int plays;

    public boolean skipped;

    public boolean collected;

    @Nullable
    @SerializedName("imdb_id")
    public String imdbId;

    /*
     * Full dump only follows.
     */

    @Nullable
    @SerializedName("episode_dvd")
    public Double episodeDvd;

    @Nullable
    public String overview;

    @Nullable
    public String image;

    @Nullable
    public String writers;

    @Nullable
    public String gueststars;

    @Nullable
    public String directors;

    @Nullable
    public Double rating;
    @Nullable
    public Integer rating_votes;
    @Nullable
    public Integer rating_user;
}
