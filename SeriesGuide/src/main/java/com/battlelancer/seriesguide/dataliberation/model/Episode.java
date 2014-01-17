
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

package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

public class Episode {

    @SerializedName("tvdb_id")
    public int tvdbId;

    public int episode;

    @SerializedName("episode_absolute")
    public int episodeAbsolute;

    public String title;

    @SerializedName("first_aired")
    public long firstAired;

    public boolean watched;

    public boolean skipped;

    public boolean collected;

    @SerializedName("imdb_id")
    public String imdbId;

    /*
     * Full dump only follows.
     */

    @SerializedName("episode_dvd")
    public double episodeDvd;

    public String overview;

    public String image;

    public String writers;

    public String gueststars;

    public String directors;

    public double rating;

    @SerializedName("last_edited")
    public long lastEdited;

}
