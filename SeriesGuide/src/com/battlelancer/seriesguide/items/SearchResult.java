
package com.battlelancer.seriesguide.items;

/**
 * @author trottmann.uwe
 */
public class SearchResult {

    public String tvdbid;

    public String title;

    public String overview;

    public String poster;

    public SearchResult copy() {
        SearchResult copy = new SearchResult();
        copy.tvdbid = this.tvdbid;
        copy.title = this.title;
        copy.overview = this.overview;
        copy.poster = this.poster;
        return copy;
    }

    @Override
    public String toString() {
        return this.title;
    }

}
