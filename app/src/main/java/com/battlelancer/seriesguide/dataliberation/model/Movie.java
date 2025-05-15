// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.util.TextTools;

public class Movie {

    /**
     * Required when importing.
     */
    public int tmdb_id;

    /**
     * Optional, enables link to IMDB.
     */
    public String imdb_id;

    public String title;

    /**
     * Release date in milliseconds.
     */
    public long released_utc_ms;

    public int runtime_min;

    /**
     * TMDB poster path.
     */
    public String poster;

    public String overview;

    public boolean in_collection;

    public boolean in_watchlist;

    public boolean watched;

    /**
     * The number of times this was watched.
     * <p>
     * Depending on {@link #watched}, defaults to 1 or 0.
     * <p>
     * If 1 or greater and {@link #watched} is not {@code true}, is ignored when importing.
     */
    public int plays;

    /**
     * Time in milliseconds a movie was last updated.
     */
    public long last_updated_ms;
    
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(Movies.TMDB_ID, tmdb_id);
        values.put(Movies.IMDB_ID, imdb_id);
        values.put(Movies.TITLE, title);
        values.put(Movies.TITLE_NOARTICLE, TextTools.trimLeadingArticle(title));
        values.put(Movies.RELEASED_UTC_MS, released_utc_ms);
        values.put(Movies.RUNTIME_MIN, runtime_min);
        values.put(Movies.POSTER, poster);
        values.put(Movies.IN_COLLECTION, in_collection ? 1 : 0);
        values.put(Movies.IN_WATCHLIST, in_watchlist ? 1 : 0);
        values.put(Movies.WATCHED, watched ? 1 : 0);
        int playsValue;
        if (watched && plays >= 1) {
            playsValue = plays;
        } else {
            playsValue = watched ? 1 : 0;
        }
        values.put(Movies.PLAYS, playsValue);
        values.put(Movies.LAST_UPDATED, last_updated_ms);
        // full dump values
        values.put(Movies.OVERVIEW, overview);
        // set default values
        values.put(Movies.RATING_TMDB, 0);
        values.put(Movies.RATING_VOTES_TMDB, 0);
        values.put(Movies.RATING_TRAKT, 0);
        values.put(Movies.RATING_VOTES_TRAKT, 0);
        return values;
    }

}
