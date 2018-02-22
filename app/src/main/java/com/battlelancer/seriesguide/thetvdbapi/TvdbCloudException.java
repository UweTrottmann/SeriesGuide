package com.battlelancer.seriesguide.thetvdbapi;

/**
 * Thrown when a {@link TvdbTools} Cloud operation fails.
 */
public class TvdbCloudException extends TvdbException {

    public TvdbCloudException(String message, Throwable throwable) {
        super(message, throwable, TvdbException.Service.HEXAGON, false);
    }
}
