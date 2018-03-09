package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.util.DBUtils;
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
    
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(Movies.TMDB_ID, tmdbId);
        values.put(Movies.IMDB_ID, imdbId);
        values.put(Movies.TITLE, title);
        values.put(Movies.TITLE_NOARTICLE, DBUtils.trimLeadingArticle(title));
        values.put(Movies.RELEASED_UTC_MS, releasedUtcMs);
        values.put(Movies.RUNTIME_MIN, runtimeMin);
        values.put(Movies.POSTER, poster);
        values.put(Movies.IN_COLLECTION, inCollection);
        values.put(Movies.IN_WATCHLIST, inWatchlist);
        values.put(Movies.WATCHED, watched);
        // full dump values
        values.put(Movies.OVERVIEW, overview);
        return values;
    }

}
