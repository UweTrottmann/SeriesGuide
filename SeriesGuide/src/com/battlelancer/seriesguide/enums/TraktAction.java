
package com.battlelancer.seriesguide.enums;

import com.battlelancer.seriesguide.util.TraktTask;

/**
 * The trakt action to be performed by {@link TraktTask}.
 */
public enum TraktAction {
    SEEN_EPISODE(0), RATE_EPISODE(1), CHECKIN_EPISODE(2), SHOUT(3), RATE_SHOW(4), CHECKIN_MOVIE(5);

    public final int index;

    private TraktAction(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
