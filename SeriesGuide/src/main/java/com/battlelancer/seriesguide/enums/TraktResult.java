package com.battlelancer.seriesguide.enums;

/**
 * Adds trakt API related error codes.
 */
public interface TraktResult extends NetworkResult {

    int AUTH_ERROR = -3;
    int API_ERROR = -4;

}
