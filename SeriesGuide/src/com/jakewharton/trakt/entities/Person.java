package com.jakewharton.trakt.entities;

import com.google.myjson.annotations.SerializedName;
import com.jakewharton.trakt.TraktEntity;

import java.util.Date;

public class Person implements TraktEntity {
    private static final long serialVersionUID = -4755476212550445673L;

    public String name;
    public String url;
    public String biography;
    public Date birthday;
    public String birthplace;
    @SerializedName("tmdb_id") public Integer tmdbId;
    public Images images;

    /** @deprecated Use {@link #name} */
    @Deprecated
    public String getName() {
        return this.name;
    }
    /** @deprecated Use {@link #url} */
    @Deprecated
    public String getUrl() {
        return this.url;
    }
    /** @deprecated Use {@link #biography} */
    @Deprecated
    public String getBiography() {
        return this.biography;
    }
    /** @deprecated Use {@link #birthday} */
    @Deprecated
    public Date getBirthday() {
        return this.birthday;
    }
    /** @deprecated Use {@link #birthplace} */
    @Deprecated
    public String getBirthplace() {
        return this.birthplace;
    }
    /** @deprecated Use {@link #tmdbId} */
    @Deprecated
    public Integer getTmdbId() {
        return this.tmdbId;
    }
    /** @deprecated Use {@link #images} */
    @Deprecated
    public Images getImages() {
        return this.images;
    }
}
