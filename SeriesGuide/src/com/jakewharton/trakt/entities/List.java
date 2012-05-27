package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;
import com.jakewharton.trakt.enumerations.ListPrivacy;

public class List implements TraktEntity {
    private static final long serialVersionUID = -5768791212077534364L;

    public String name;
    public String slug;
    public String url;
    public String description;
    public ListPrivacy privacy;
    public java.util.List<ListItem> items;

    /** @deprecated Use {@link #name} */
    @Deprecated
    public String getName() {
        return this.name;
    }
    /** @deprecated Use {@link #slug} */
    @Deprecated
    public String getSlug() {
        return this.slug;
    }
    /** @deprecated Use {@link #url} */
    @Deprecated
    public String getUrl() {
        return this.url;
    }
    /** @deprecated Use {@link #description} */
    @Deprecated
    public String getDescription() {
        return this.description;
    }
    /** @deprecated Use {@link #privacy} */
    @Deprecated
    public ListPrivacy getPrivacy() {
        return this.privacy;
    }
    /** @deprecated Use {@link #items} */
    @Deprecated
    public java.util.List<ListItem> getItems() {
        return this.items;
    }
}
