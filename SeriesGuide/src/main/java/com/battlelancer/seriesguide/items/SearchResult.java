package com.battlelancer.seriesguide.items;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds a search result, used later for adding this show. Supplying a poster URL is optional.
 */
public class SearchResult implements Parcelable {

    public static final int STATE_ADD = 0;
    public static final int STATE_ADDING = 1;
    public static final int STATE_ADDED = 2;

    public int tvdbid;

    /** Two-letter ISO 639-1 language code. */
    public String language;

    public String title;

    public String overview;

    public String posterPath;

    public int state;

    public static final Parcelable.Creator<SearchResult> CREATOR
            = new Parcelable.Creator<SearchResult>() {
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
        language = in.readString();
        title = in.readString();
        overview = in.readString();
        posterPath = in.readString();
        state = in.readInt();
    }

    public SearchResult copy() {
        SearchResult copy = new SearchResult();
        copy.tvdbid = this.tvdbid;
        copy.language = this.language;
        copy.title = this.title;
        copy.overview = this.overview;
        copy.posterPath = this.posterPath;
        copy.state = this.state;
        return copy;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(tvdbid);
        dest.writeString(language);
        dest.writeString(title);
        dest.writeString(overview);
        dest.writeString(posterPath);
        dest.writeInt(state);
    }
}
