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

package com.battlelancer.seriesguide.thetvdbapi;

/**
 * Thrown when a {@link com.battlelancer.seriesguide.thetvdbapi.TheTVDB} operation fails.
 */
public class TvdbException extends Exception {

    private final boolean itemDoesNotExist;

    public TvdbException(String message) {
        this(message, false, null);
    }

    public TvdbException(String message, Throwable throwable) {
        this(message, false, throwable);
    }

    public TvdbException(String message, boolean itemDoesNotExist, Throwable throwable) {
        super(message, throwable);
        this.itemDoesNotExist = itemDoesNotExist;
    }

    /**
     * If the TheTVDB item does not exist (a HTTP 404 response was returned).
     */
    public boolean getItemDoesNotExist() {
        return itemDoesNotExist;
    }
}
