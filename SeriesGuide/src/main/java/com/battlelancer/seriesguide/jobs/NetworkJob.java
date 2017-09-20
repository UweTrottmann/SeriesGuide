package com.battlelancer.seriesguide.jobs;

import com.battlelancer.seriesguide.jobs.episodes.JobAction;

public class NetworkJob {

    public static final int SUCCESS = 0;
    /** Issue connecting or reading a response, should retry. */
    public static final int ERROR_CONNECTION = -1;
    public static final int ERROR_TRAKT_AUTH = -2;
    /** Issue with request, do not retry. */
    public static final int ERROR_TRAKT_CLIENT = -3;
    /** Issue with connection or server, do retry. */
    public static final int ERROR_TRAKT_SERVER = -4;
    /** Show, season or episode not found, do not retry, but notify. */
    public static final int ERROR_TRAKT_NOT_FOUND = -5;
    /** Issue with the request, do not retry. */
    public static final int ERROR_HEXAGON_CLIENT = -6;
    /** Issue with connection or server, should retry. */
    public static final int ERROR_HEXAGON_SERVER = -7;
    public static final int ERROR_HEXAGON_AUTH = -8;

    final JobAction action;
    final SgJobInfo jobInfo;

    public NetworkJob(JobAction action, SgJobInfo jobInfo) {
        this.action = action;
        this.jobInfo = jobInfo;
    }
}
