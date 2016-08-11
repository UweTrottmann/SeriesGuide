package com.battlelancer.seriesguide.thetvdbapi;

/**
 * Thrown when a {@link TvdbTools} operation fails.
 */
public class TvdbException extends Exception {

    private final boolean itemDoesNotExist;

    public TvdbException(String message) {
        this(message, false, null);
    }

    public TvdbException(String message, Throwable throwable) {
        this(message, false, throwable);
    }

    public TvdbException(String message, boolean itemDoesNotExist, Throwable throwable) {
        super(message, throwable);
        this.itemDoesNotExist = itemDoesNotExist;
    }

    /**
     * If the TheTVDB item does not exist (a HTTP 404 response was returned).
     */
    public boolean getItemDoesNotExist() {
        return itemDoesNotExist;
    }
}
