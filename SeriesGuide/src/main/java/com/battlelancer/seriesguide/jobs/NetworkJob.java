package com.battlelancer.seriesguide.jobs;

import com.battlelancer.seriesguide.jobs.episodes.JobAction;

public class NetworkJob {

    public static final int SUCCESS = 0;
    /** Issue connecting or reading a response, should retry. */
    public static final int ERROR_CONNECTION = -1;
    public static final int ERROR_TRAKT_AUTH = -2;
    public static final int ERROR_TRAKT_API = -3;
    /** Issue with the request, do not retry. */
    public static final int ERROR_HEXAGON_CLIENT = -4;
    /** Issue at the server, should retry. */
    public static final int ERROR_HEXAGON_SERVER = -5;
    public static final int ERROR_HEXAGON_AUTH = -6;

    final JobAction action;
    final SgJobInfo jobInfo;

    public NetworkJob(JobAction action, SgJobInfo jobInfo) {
        this.action = action;
        this.jobInfo = jobInfo;
    }
}
