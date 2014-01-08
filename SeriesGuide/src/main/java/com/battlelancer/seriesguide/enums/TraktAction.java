
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

package com.battlelancer.seriesguide.enums;

import com.battlelancer.seriesguide.util.TraktTask;

/**
 * The trakt action to be performed by {@link TraktTask}.
 */
public enum TraktAction {
    SEEN_EPISODE(0),
    RATE_EPISODE(1),
    CHECKIN_EPISODE(2),
    SHOUT(3),
    RATE_SHOW(4),
    CHECKIN_MOVIE(5),
    WATCHLIST_MOVIE(6),
    UNWATCHLIST_MOVIE(7);

    public final int index;

    private TraktAction(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
