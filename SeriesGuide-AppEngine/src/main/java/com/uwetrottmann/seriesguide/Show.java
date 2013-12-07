package com.uwetrottmann.seriesguide;

import org.datanucleus.util.StringUtils;

import javax.persistence.Entity;

@Entity
public class Show extends BaseEntity {

    private int tvdbId;

    private Boolean isFavorite;

    private Boolean isHidden;

    private Boolean isSyncEnabled;

    private String getGlueId;

    public Show() {
    }

    public int getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(int tvdbId) {
        this.tvdbId = tvdbId;
    }

    /**
     * Returns false if any of the property values of the given show (except TVDb id) are not equal
     * to ours. However, if a new value is null (e.g. not set) it is seen as equal, regardless of
     * our value.
     */
    public boolean hasSameValues(Show show) {
        if (hasDifferentValueExceptNull(show.getIsFavorite(), getIsFavorite())) {
            return false;
        }
        if (hasDifferentValueExceptNull(show.getIsHidden(), getIsHidden())) {
            return false;
        }
        if (hasDifferentValueExceptNull(show.getIsSyncEnabled(), getIsSyncEnabled())) {
            return false;
        }
        if (hasDifferentValueExceptNull(show.getGetGlueId(), getGetGlueId())) {
            return false;
        }
        return true;
    }

    private boolean hasDifferentValueExceptNull(Boolean boolNew, Boolean boolOld) {
        return boolNew != null && boolNew != boolOld;
    }

    private boolean hasDifferentValueExceptNull(String stringNew, String stringOld) {
        if (stringNew == null && stringOld == null) {
            return false;
        }
        if (stringNew != null && stringOld == null) {
            return true;
        }
        if (stringNew == null && stringOld != null) {
            return false;
        }
        return !stringOld.equals(stringNew);
    }

    /**
     * Copies all values other than the TVDb id if they are not null into this entity.
     */
    public void copyPropertyValues(Show show) {
        if (show.getIsFavorite() != null) {
            setIsFavorite(show.getIsFavorite());
        }
        if (show.getIsHidden() != null) {
            setIsHidden(show.getIsHidden());
        }
        if (show.getIsSyncEnabled() != null) {
            setIsSyncEnabled(show.getIsSyncEnabled());
        }
        if (show.getGetGlueId() != null) {
            setGetGlueId(show.getGetGlueId());
        }
    }

    public Boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public Boolean getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }

    public Boolean getIsSyncEnabled() {
        return isSyncEnabled;
    }

    public void setIsSyncEnabled(Boolean isSyncEnabled) {
        this.isSyncEnabled = isSyncEnabled;
    }

    public String getGetGlueId() {
        return getGlueId;
    }

    public void setGetGlueId(String getGlueId) {
        this.getGlueId = getGlueId;
    }
}
