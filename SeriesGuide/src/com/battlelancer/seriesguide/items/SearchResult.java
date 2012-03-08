
package com.battlelancer.seriesguide.items;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author trottmann.uwe
 */
public class SearchResult implements Parcelable {

    public String tvdbid;

    public String title;

    public String overview;

    public String poster;

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
        tvdbid = in.readString();
        title = in.readString();
        overview = in.readString();
        poster = in.readString();
    }

    public SearchResult copy() {
        SearchResult copy = new SearchResult();
        copy.tvdbid = this.tvdbid;
        copy.title = this.title;
        copy.overview = this.overview;
        copy.poster = this.poster;
        return copy;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(tvdbid);
        dest.writeString(title);
        dest.writeString(overview);
        dest.writeString(poster);
    }

}
