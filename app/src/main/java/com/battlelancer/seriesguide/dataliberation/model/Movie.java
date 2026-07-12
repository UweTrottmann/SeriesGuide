// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation.model;

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
}
