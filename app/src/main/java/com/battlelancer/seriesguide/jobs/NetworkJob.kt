package com.battlelancer.seriesguide.jobs;

import android.content.Context;
import com.battlelancer.seriesguide.sync.NetworkJobProcessor;

public interface NetworkJob {

    int SUCCESS = 0;
    /** Issue connecting or reading a response, should retry. */
    int ERROR_CONNECTION = -1;
    int ERROR_TRAKT_AUTH = -2;
    /** Issue with request, do not retry. */
    int ERROR_TRAKT_CLIENT = -3;
    /** Issue with connection or server, do retry. */
    int ERROR_TRAKT_SERVER = -4;
    /** Show, season or episode not found, do not retry, but notify. */
    int ERROR_TRAKT_NOT_FOUND = -5;
    /** Issue with the request, do not retry. */
    int ERROR_HEXAGON_CLIENT = -6;
    /** Issue with connection or server, should retry. */
    int ERROR_HEXAGON_SERVER = -7;
    int ERROR_HEXAGON_AUTH = -8;

    NetworkJobProcessor.JobResult execute(Context context);

}
