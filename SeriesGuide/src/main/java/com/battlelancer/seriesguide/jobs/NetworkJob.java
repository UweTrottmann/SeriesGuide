package com.battlelancer.seriesguide.jobs;

import com.battlelancer.seriesguide.jobs.episodes.JobAction;

public class NetworkJob {

    public static final int SUCCESS = 0;
    public static final int ERROR_TRAKT_AUTH = -2;
    public static final int ERROR_TRAKT_API = -3;
    public static final int ERROR_HEXAGON_API = -4;

    final JobAction action;
    final SgJobInfo jobInfo;

    public NetworkJob(JobAction action, SgJobInfo jobInfo) {
        this.action = action;
        this.jobInfo = jobInfo;
    }
}
