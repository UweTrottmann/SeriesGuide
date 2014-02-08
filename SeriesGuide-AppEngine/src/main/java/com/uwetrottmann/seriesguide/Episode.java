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
public class Episode extends BaseEntity {

    private int tvdbId;

    private int showTvdbId;

    private Integer watchedFlag;

    private Boolean isCollected;

    public Episode() {
    }

    public int getTvdbId() {
        return tvdbId;
    }

    public void setTvdbId(int tvdbId) {
        this.tvdbId = tvdbId;
    }

    public int getShowTvdbId() {
        return showTvdbId;
    }

    public void setShowTvdbId(int showTvdbId) {
        this.showTvdbId = showTvdbId;
    }

    public boolean hasValidValues() {
        if (getTvdbId() <= 0) {
            return false;
        }
        if (getShowTvdbId() <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns false if any of the property values of the given show (except the TVDb ids) are not
     * equal to ours. However, if a new value is null (e.g. not set) it is seen as equal, regardless
     * of our value.
     */
    public boolean shouldUpdateWith(Episode episode) {
        if (hasDifferentValueExceptNull(episode.getWatchedFlag(), getWatchedFlag())) {
            return false;
        }
        if (hasDifferentValueExceptNull(episode.getIsCollected(), getIsCollected())) {
            return false;
        }
        return true;
    }

    private boolean hasDifferentValueExceptNull(Boolean boolNew, Boolean boolOld) {
        return boolNew != null && boolNew != boolOld;
    }

    private boolean hasDifferentValueExceptNull(Integer integerNew, Integer integerOld) {
        // is there NO old value?
        if (integerOld == null) {
            // is the new value valid?
            return integerNew != null;
        }
        // we have an old value
        // is the new value NOT valid?
        if (integerNew == null) {
            return false;
        }
        return !integerOld.equals(integerNew);
    }

    /**
     * Copies all values, other than the TVDb ids, if they are not null into this entity.
     */
    public void updateWith(Episode episode) {
        if (episode.getWatchedFlag() != null) {
            setWatchedFlag(episode.getWatchedFlag());
        }
        if (episode.getIsCollected() != null) {
            setIsCollected(episode.getIsCollected());
        }
    }

    public Integer getWatchedFlag() {
        return watchedFlag;
    }

    public void setWatchedFlag(Integer watchedFlag) {
        this.watchedFlag = watchedFlag;
    }

    public Boolean getIsCollected() {
        return isCollected;
    }

    public void setIsCollected(Boolean isCollected) {
        this.isCollected = isCollected;
    }
}
