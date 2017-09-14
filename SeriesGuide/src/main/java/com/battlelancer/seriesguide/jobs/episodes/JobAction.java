package com.battlelancer.seriesguide.jobs.episodes;

public enum JobAction {
    EPISODE_COLLECTED(false),
    SEASON_COLLECTED(false),
    SHOW_COLLECTED(false),
    EPISODE_WATCHED(true),
    EPISODE_WATCHED_PREVIOUS(true),
    SEASON_WATCHED(true),
    SHOW_WATCHED(true);

    final boolean isWatchNotCollect;

    JobAction(boolean isWatchNotCollect) {
        this.isWatchNotCollect = isWatchNotCollect;
    }
}
