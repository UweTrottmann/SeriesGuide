package com.uwetrottmann.seriesguide;

import javax.persistence.Entity;

@Entity
public class Show extends BaseEntity {

    public int tvdbId;

    public Show() {
    }

    public int getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(int tvdbId) {
        this.tvdbId = tvdbId;
    }

}
