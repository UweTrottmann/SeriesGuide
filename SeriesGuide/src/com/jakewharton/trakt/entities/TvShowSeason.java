package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

import java.util.List;

public class TvShowSeason implements TraktEntity {
    private static final long serialVersionUID = -1283154821327471366L;

    public static class Episodes implements TraktEntity {
        private static final long serialVersionUID = -8143500365188820979L;

        public Integer count;
        public List<Integer> numbers;
        public List<TvShowEpisode> episodes;

        /** @deprecated Use {@link #count} */
        @Deprecated
        public Integer getCount() {
            return this.count;
        }
        /** @deprecated Use {@link #numbers} */
        @Deprecated
        public List<Integer> getNumbers() {
            return this.numbers;
        }
        /** @deprecated Use {@link #episodes} */
        @Deprecated
        public List<TvShowEpisode> getEpisodes() {
            return this.episodes;
        }
    }

    public Integer season;
    public Episodes episodes;
    public String url;
    public Images images;

    /** @deprecated Use {@link #season} */
    @Deprecated
    public Integer getSeason() {
        return this.season;
    }
    /** @deprecated Use {@link #episodes} */
    @Deprecated
    public Episodes getEpisodes() {
        return this.episodes;
    }
    /** @deprecated Use {@link #url} */
    @Deprecated
    public String getUrl() {
        return this.url;
    }
    /** @deprecated Use {@link #images} */
    @Deprecated
    public Images getImages() {
        return this.images;
    }
}
