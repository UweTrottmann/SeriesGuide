package com.uwetrottmann.seriesguide;

import javax.persistence.Entity;

@Entity
public class Show extends BaseEntity {

    private int tvdbId;

    private boolean isFavorite;

    private boolean isHidden;

    private boolean isSyncEnabled;

    private String getGlueId;

    public Show() {
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public int getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(int tvdbId) {
        this.tvdbId = tvdbId;
    }

    public void copyPropertyValues(Show show) {
        setFavorite(show.isFavorite());
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public boolean isSyncEnabled() {
        return isSyncEnabled;
    }

    public void setSyncEnabled(boolean isSyncEnabled) {
        this.isSyncEnabled = isSyncEnabled;
    }

    public String getGetGlueId() {
        return getGlueId;
    }

    public void setGetGlueId(String getGlueId) {
        this.getGlueId = getGlueId;
    }
}
