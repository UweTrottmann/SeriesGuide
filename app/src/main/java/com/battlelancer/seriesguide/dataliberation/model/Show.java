
package com.battlelancer.seriesguide.dataliberation.model;

import androidx.annotation.Nullable;
import java.util.List;

/**
 * Model to import/export show data.
 */
public class Show {

    /**
     * May be null for legacy backups.
     */
    @Nullable public Integer tmdb_id;
    /**
     * Is null on new backups.
     */
    @Nullable public Integer tvdb_id;
    public String imdb_id;
    public Integer trakt_id;

    public String title;
    public String overview;

    public String language;

    public String first_aired;
    public int release_time;
    public int release_weekday;
    public String release_timezone;
    public String country;

    public String poster;
    public String content_rating;
    public String status;
    public int runtime;
    public String genres;
    public String network;

    public double rating;
    public int rating_votes;
    public Integer rating_user;

    /** SeriesGuide specific values */
    public boolean favorite;
    public Boolean notify;
    public boolean hidden;

    public long last_watched_ms;

    public List<Season> seasons;
}
