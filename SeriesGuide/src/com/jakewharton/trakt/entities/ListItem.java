package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.ListItemType;

public class ListItem implements TraktEntity {
    private static final long serialVersionUID = 7584772036063464460L;

    public ListItemType type;
    public Movie movie;
    public TvShow show;
    public String season;
    @SerializedName("episode_num") public String episodeNumber;
    public TvShowEpisode episode;

    /** @deprecated Use {@link #type} */
    @Deprecated
    public ListItemType getType() {
        return type;
    }
    /** @deprecated Use {@link #movie} */
    @Deprecated
    public Movie getMovie() {
        return movie;
    }
    /** @deprecated Use {@link #show} */
    @Deprecated
    public TvShow getShow() {
        return show;
    }
    /** @deprecated Use {@link #season} */
    @Deprecated
    public String getSeason() {
        return season;
    }
    /** @deprecated Use {@link #episodeNumber} */
    @Deprecated
    public String getEpisodeNumber() {
        return episodeNumber;
    }
    /** @deprecated Use {@link #episode} */
    @Deprecated
    public TvShowEpisode getEpisode() {
        return episode;
    }
}
