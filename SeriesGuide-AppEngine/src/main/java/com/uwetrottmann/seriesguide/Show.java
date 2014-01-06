/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uwetrottmann.seriesguide;

import javax.persistence.Entity;

@Entity
public class Show extends BaseEntity {

    private int tvdbId;

    private Boolean isFavorite;

    /**
     * If this show is hidden from the show list.
     */
    private Boolean isHidden;

    /**
     * If this show was removed on at least one device. Check this flag before auto-adding to your
     * local database.
     */
    private Boolean isRemoved;

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
        if (hasDifferentValueExceptNull(show.getIsRemoved(), getIsRemoved())) {
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
        if (show.getIsRemoved() != null) {
            setIsRemoved(show.getIsRemoved());
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

    public Boolean getIsRemoved() {
        return isRemoved;
    }

    public void setIsRemoved(Boolean isRemoved) {
        this.isRemoved = isRemoved;
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
