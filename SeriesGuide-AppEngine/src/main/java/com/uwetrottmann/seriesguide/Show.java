package com.uwetrottmann.seriesguide;

public class Show {

    public int tvdbId;

    public String owner;

    public Show() {
    }

    public Show(int tvdbId, String owner) {
        setTvdbId(tvdbId);
        setOwner(owner);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(int tvdbId) {
        this.tvdbId = tvdbId;
    }

}
