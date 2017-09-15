package com.battlelancer.seriesguide.jobs.episodes;

public enum JobAction {
    UNKNOWN(0),
    EPISODE_COLLECTION(1),
    EPISODE_WATCHED_FLAG(2);

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
