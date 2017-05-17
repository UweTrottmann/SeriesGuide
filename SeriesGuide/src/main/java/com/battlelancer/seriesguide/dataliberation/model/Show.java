
package com.battlelancer.seriesguide.dataliberation.model;

import java.util.List;

/**
 * @see com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns ShowsColumns
 */
public class Show {

    public int tvdb_id;
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
    public int rating_user;

    public long last_edited;

    /** SeriesGuide specific values */
    public boolean favorite;
    public Boolean notify;
    public boolean hidden;

    public long last_updated;
    public int last_watched_episode;
    public long last_watched_ms;

    public List<Season> seasons;
}
