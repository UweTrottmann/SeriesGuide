package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

public class Ratings implements TraktEntity {
    private static final long serialVersionUID = -7517132370821535250L;

    public Integer percentage;
    public Integer votes;
    public Integer loved;
    public Integer hated;

    /** @deprecated Use {@link #percentage} */
    @Deprecated
    public Integer getPercentage() {
        return percentage;
    }
    /** @deprecated Use {@link #votes} */
    @Deprecated
    public Integer getVotes() {
        return votes;
    }
    /** @deprecated Use {@link #loved} */
    @Deprecated
    public Integer getLoved() {
        return loved;
    }
    /** @deprecated Use {@link #hated} */
    @Deprecated
    public Integer getHated() {
        return hated;
    }
}