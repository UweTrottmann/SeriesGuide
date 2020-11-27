package com.battlelancer.seriesguide.traktapi;

import com.battlelancer.seriesguide.enums.NetworkResult;

/**
 * Adds trakt API related error codes.
 */
public interface TraktResult extends NetworkResult {

    int AUTH_ERROR = -3;
    int API_ERROR = -4;
    int ACCOUNT_LOCKED = -5;

}
