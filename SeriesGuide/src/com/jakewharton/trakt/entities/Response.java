package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

public class Response implements TraktEntity {
    private static final long serialVersionUID = 5921890886906816035L;

    public String status; //TODO: enum
    public String message;
    public String error;
    public int wait;

    /** @deprecated Use {@link #status} */
    @Deprecated
    public String getStatus() {
        return this.status;
    }
    /** @deprecated Use {@link #message} */
    @Deprecated
    public String getMessage() {
        return this.message;
    }
    /** @deprecated Use {@link #error} */
    @Deprecated
    public String getError() {
        return this.error;
    }
}
