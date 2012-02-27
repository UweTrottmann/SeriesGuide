package com.battlelancer.seriesguide.items;

public class Episode {
    private String mId;

    private String mNumber;

    private String mSeason;

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String mNumber) {
        this.mNumber = mNumber;
    }

    public String getSeason() {
        return mSeason;
    }

    public void setSeason(String mSeason) {
        this.mSeason = mSeason;
    }
}