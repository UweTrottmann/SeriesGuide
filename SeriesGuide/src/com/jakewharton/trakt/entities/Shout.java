package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

import java.util.Calendar;

public class Shout implements TraktEntity {
    private static final long serialVersionUID = 4324069488018464744L;

    public Calendar inserted;
    public String shout;
    public UserProfile user;
    public boolean spoiler;

    /** @deprecated Use {@link #inserted} */
    @Deprecated
    public Calendar getInserted() {
        return this.inserted;
    }
    /** @deprecated Use {@link #shout} */
    @Deprecated
    public String getShout() {
        return this.shout;
    }
    /** @deprecated Use {@link #user} */
    @Deprecated
    public UserProfile getUser() {
        return this.user;
    }
}
