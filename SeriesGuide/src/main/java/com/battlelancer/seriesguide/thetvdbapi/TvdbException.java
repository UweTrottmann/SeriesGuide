package com.battlelancer.seriesguide.thetvdbapi;

import android.support.annotation.Nullable;

/**
 * Thrown when a {@link TvdbTools} operation fails.
 */
public class TvdbException extends Exception {

    public enum Service {
        TVDB,
        HEXAGON,
        DATA
    }

    private final Service service;
    private final boolean itemDoesNotExist;

    public TvdbException(String message) {
        this(message, null, Service.TVDB, false);
    }

    public TvdbException(String message, Throwable throwable) {
        this(message, throwable, Service.TVDB, false);
    }

    public TvdbException(String message, boolean itemDoesNotExist) {
        this(message, null, Service.TVDB, itemDoesNotExist);
    }

    protected TvdbException(String message, Throwable throwable, Service service,
            boolean itemDoesNotExist) {
        super(message, throwable);
        this.service = service;
        this.itemDoesNotExist = itemDoesNotExist;
    }

    /**
     * If not {@code null} the service that interacting with failed.
     */
    @Nullable
    public Service service() {
        return service;
    }

    /**
     * If the TheTVDB item does not exist (a HTTP 404 response was returned).
     */
    public boolean itemDoesNotExist() {
        return itemDoesNotExist;
    }
}
