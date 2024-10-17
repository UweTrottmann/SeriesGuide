// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover;

/**
 * Holds a search result, used later for adding this show. Supplying a poster URL is optional.
 */
public class SearchResult {

    public static final int STATE_ADD = 0;
    public static final int STATE_ADDING = 1;
    public static final int STATE_ADDED = 2;

    private int tmdbId;
    private String language;
    private String title;
    private String overview;
    private String posterPath;
    private int state;

    public SearchResult() {
    }

    public SearchResult copy() {
        SearchResult copy = new SearchResult();
        copy.setTmdbId(this.getTmdbId());
        copy.setLanguage(this.getLanguage());
        copy.setTitle(this.getTitle());
        copy.setOverview(this.getOverview());
        copy.setPosterPath(this.getPosterPath());
        copy.setState(this.getState());
        return copy;
    }

    public int getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(int tmdbId) {
        this.tmdbId = tmdbId;
    }

    /** Two-letter ISO 639-1 language code plus ISO-3166-1 region tag. */
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
