/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */
package com.battlelancer.seriesguide.items;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds a search result, used later for adding this show. Supplying a poster URL is optional.
 */
public class SearchResult implements Parcelable {

    public int tvdbid;

    public String title;

    public String overview;

    public String poster;

    public boolean isAdded;

    public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

    public SearchResult() {
    }

    public SearchResult(Parcel in) {
        tvdbid = in.readInt();
        title = in.readString();
        overview = in.readString();
        poster = in.readString();
        isAdded = in.readInt() == 1;
    }

    public SearchResult copy() {
        SearchResult copy = new SearchResult();
        copy.tvdbid = this.tvdbid;
        copy.title = this.title;
        copy.overview = this.overview;
        copy.poster = this.poster;
        copy.isAdded = this.isAdded;
        return copy;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(tvdbid);
        dest.writeString(title);
        dest.writeString(overview);
        dest.writeString(poster);
        dest.writeInt(isAdded ? 1 : 0);
    }

}
