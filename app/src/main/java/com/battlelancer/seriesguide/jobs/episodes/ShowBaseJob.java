package com.battlelancer.seriesguide.jobs.episodes;

public abstract class ShowBaseJob extends BaseEpisodesJob {

    private final long showId;

    public ShowBaseJob(long showId, int flagValue, JobAction action) {
        super(flagValue, action);
        this.showId = showId;
    }

    @Override
    public long getShowId() {
        return showId;
    }
}
