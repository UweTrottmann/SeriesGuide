package com.battlelancer.seriesguide.jobs.episodes;

public enum JobAction {
    UNKNOWN(0),
    EPISODE_COLLECTION(1),
    EPISODE_WATCHED_FLAG(2),
    MOVIE_COLLECTION_ADD(3),
    MOVIE_COLLECTION_REMOVE(4),
    MOVIE_WATCHLIST_ADD(5),
    MOVIE_WATCHLIST_REMOVE(6),
    MOVIE_WATCHED_SET(7),
    MOVIE_WATCHED_REMOVE(8);

    public int id;

    JobAction(int id) {
        this.id = id;
    }

    public static JobAction fromId(int id) {
        for (JobAction action : JobAction.values()) {
            if (action.id == id) {
                return action;
            }
        }
        return UNKNOWN;
    }
}
