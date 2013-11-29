package com.uwetrottmann.seriesguide;

import javax.persistence.Entity;

@Entity
public class Show extends BaseEntity {

    public int tvdbId;

    public boolean isFavorite;

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

}
