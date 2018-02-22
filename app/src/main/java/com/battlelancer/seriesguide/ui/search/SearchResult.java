package com.battlelancer.seriesguide.ui.search;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds a search result, used later for adding this show. Supplying a poster URL is optional.
 */
public class SearchResult implements Parcelable {

    static final int STATE_ADD = 0;
    static final int STATE_ADDING = 1;
    static final int STATE_ADDED = 2;

    private int tvdbid;
    private String language;
    private String title;
    private String overview;
    private String posterPath;
    private int state;

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
        setTvdbid(in.readInt());
        setLanguage(in.readString());
        setTitle(in.readString());
        setOverview(in.readString());
        setPosterPath(in.readString());
        setState(in.readInt());
    }

    public SearchResult copy() {
        SearchResult copy = new SearchResult();
        copy.setTvdbid(this.getTvdbid());
        copy.setLanguage(this.getLanguage());
        copy.setTitle(this.getTitle());
        copy.setOverview(this.getOverview());
        copy.setPosterPath(this.getPosterPath());
        copy.setState(this.getState());
        return copy;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getTvdbid());
        dest.writeString(getLanguage());
        dest.writeString(getTitle());
        dest.writeString(getOverview());
        dest.writeString(getPosterPath());
        dest.writeInt(getState());
    }

    public int getTvdbid() {
        return tvdbid;
    }

    public void setTvdbid(int tvdbid) {
        this.tvdbid = tvdbid;
    }

    /** Two-letter ISO 639-1 language code. */
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
