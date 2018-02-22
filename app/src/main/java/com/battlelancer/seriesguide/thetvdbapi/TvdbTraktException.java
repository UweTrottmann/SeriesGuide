package com.battlelancer.seriesguide.thetvdbapi;

/**
 * Thrown when a {@link TvdbTools} trakt operation fails.
 */
public class TvdbTraktException extends TvdbException {

    public TvdbTraktException(String message) {
        super(message, null, Service.TRAKT, false);
    }
}
