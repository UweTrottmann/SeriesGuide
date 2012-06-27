package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;
import com.jakewharton.trakt.enumerations.Rating;

import java.util.Date;

/**
 * Represents a Trakt activity item. See
 * <a href="http://trakt.tv/api-docs/activity-community"> the documentation</a>
 * for a list of {@link #type}s and {@link #action}s and which properties they
 * include.
 */
public class ActivityItemBase implements TraktEntity {
    private static final long serialVersionUID = -7644201423350992899L;

    public static class When implements TraktEntity {
        private static final long serialVersionUID = 8126529523279348951L;

        public String day;
        public String time;
    }
    public static class Elapsed implements TraktEntity {
        private static final long serialVersionUID = -6458210319412047876L;

        @SerializedName("short")
        public String _short;
        public String full;
    }
    public static class Shout implements TraktEntity {
        private static final long serialVersionUID = 7034369697434197979L;

        public String text;
    }

    public Date timestamp;
    public Date watched;
    public When when;
    public Elapsed elapsed;
    public ActivityType type;
    public ActivityAction action;
    public UserProfile user;

    public Rating rating;
    public Shout shout;

    public TvShow show;
    public TvShowEpisode episode;
    public java.util.List<TvShowEpisode> episodes;

    public Movie movie;

    public List list;
    @SerializedName("list_item")
    public ListItem listItem;
}
