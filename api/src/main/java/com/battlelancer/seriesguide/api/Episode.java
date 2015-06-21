/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.api;

import android.os.Bundle;

public class Episode {
    private static final String KEY_TITLE = "title";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_NUMBER_ABSOLUTE = "numberAbsolute";
    private static final String KEY_SEASON = "season";
    private static final String KEY_IMDBID = "imdbid";
    private static final String KEY_TVDBID = "tvdbid";

    private static final String KEY_SHOW_TITLE = "showTitle";
    private static final String KEY_SHOW_TVDBID = "showTvdbId";
    private static final String KEY_SHOW_IMDBID = "showImdbId";

    private String mTitle;
    private Integer mNumber;
    private Integer mNumberAbsolute;
    private Integer mSeason;
    private Integer mTvdbId;
    private String mImdbId;

    private String mShowTitle;
    private Integer mShowTvdbId;
    private String mShowImdbId;

    private Episode() {
    }

    public String getTitle() {
        return mTitle;
    }

    public Integer getNumber() {
        return mNumber;
    }

    public Integer getNumberAbsolute() {
        return mNumberAbsolute;
    }

    public Integer getSeason() {
        return mSeason;
    }

    public Integer getTvdbId() {
        return mTvdbId;
    }

    public String getImdbId() {
        return mImdbId;
    }

    public String getShowTitle() {
        return mShowTitle;
    }

    public Integer getShowTvdbId() {
        return mShowTvdbId;
    }

    public String getShowImdbId() {
        return mShowImdbId;
    }

    public static class Builder {
        private final Episode mEpisode;

        public Builder() {
            mEpisode = new Episode();
        }

        public Builder title(String episodeTitle) {
            mEpisode.mTitle = episodeTitle;
            return this;
        }

        public Builder number(Integer episodeNumber) {
            mEpisode.mNumber = episodeNumber;
            return this;
        }

        public Builder numberAbsolute(Integer absoluteNumber) {
            mEpisode.mNumberAbsolute = absoluteNumber;
            return this;
        }

        public Builder season(Integer seasonNumber) {
            mEpisode.mSeason = seasonNumber;
            return this;
        }

        public Builder tvdbId(Integer episodeTvdbId) {
            mEpisode.mTvdbId = episodeTvdbId;
            return this;
        }

        public Builder imdbId(String episodeImdbId) {
            mEpisode.mImdbId = episodeImdbId;
            return this;
        }

        public Builder showTitle(String showTitle) {
            mEpisode.mShowTitle = showTitle;
            return this;
        }

        public Builder showTvdbId(Integer showTvdbId) {
            mEpisode.mShowTvdbId = showTvdbId;
            return this;
        }

        public Builder showImdbId(String showImdbId) {
            mEpisode.mShowImdbId = showImdbId;
            return this;
        }

        public Episode build() {
            return mEpisode;
        }
    }

    /**
     * Serializes this {@link Episode} object to a {@link android.os.Bundle} representation.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, mTitle);
        bundle.putInt(KEY_NUMBER, mNumber);
        bundle.putInt(KEY_NUMBER_ABSOLUTE, mNumberAbsolute);
        bundle.putInt(KEY_SEASON, mSeason);
        bundle.putInt(KEY_TVDBID, mTvdbId);
        bundle.putString(KEY_IMDBID, mImdbId);
        bundle.putString(KEY_SHOW_TITLE, mShowTitle);
        bundle.putInt(KEY_SHOW_TVDBID, mShowTvdbId);
        bundle.putString(KEY_SHOW_IMDBID, mShowImdbId);
        return bundle;
    }

    /**
     * Deserializes an {@link Episode} into a {@link android.os.Bundle} object.
     */
    public static Episode fromBundle(Bundle bundle) {
        Builder builder = new Builder()
                .title(bundle.getString(KEY_TITLE))
                .number(bundle.getInt(KEY_NUMBER))
                .numberAbsolute(bundle.getInt(KEY_NUMBER_ABSOLUTE))
                .season(bundle.getInt(KEY_SEASON))
                .tvdbId(bundle.getInt(KEY_TVDBID))
                .imdbId(bundle.getString(KEY_IMDBID))
                .showTitle(bundle.getString(KEY_SHOW_TITLE))
                .showTvdbId(bundle.getInt(KEY_SHOW_TVDBID))
                .showImdbId(bundle.getString(KEY_SHOW_IMDBID));

        return builder.build();
    }
}
