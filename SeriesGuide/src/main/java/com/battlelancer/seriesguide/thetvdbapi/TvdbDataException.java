package com.battlelancer.seriesguide.thetvdbapi;

/**
 * Thrown when a {@link TvdbTools} data operation fails.
 */
public class TvdbDataException extends TvdbException {

    public TvdbDataException(String message) {
        this(message, null);
    }

    public TvdbDataException(String message, Throwable throwable) {
        super(message, throwable, Service.DATA, false);
    }
}
