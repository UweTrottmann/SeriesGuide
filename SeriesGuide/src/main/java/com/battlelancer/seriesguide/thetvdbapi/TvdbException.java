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

    public TvdbException() {
    }

    public TvdbException(String detailMessage) {
        super(detailMessage);
    }

    public TvdbException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public TvdbException(Throwable throwable) {
        super(throwable);
    }
}
