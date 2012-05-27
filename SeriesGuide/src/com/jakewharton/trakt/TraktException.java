package com.jakewharton.trakt;

import com.google.myjson.JsonObject;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.entities.Response;

public final class TraktException extends RuntimeException {
    private static final long serialVersionUID = 6158978902757706299L;

    private final String url;
    private final JsonObject postBody;
    private final Response response;

    public TraktException(String url, ApiException cause) {
        this(url, null, cause);
    }
    public TraktException(String url, JsonObject postBody, ApiException cause) {
        super(cause);
        this.url = url;
        this.postBody = postBody;
        this.response = null;
    }
    public TraktException(String url, JsonObject postBody, ApiException cause, Response response) {
        super(response.error, cause);
        this.url = url;
        this.postBody = postBody;
        this.response = response;
    }

    public String getUrl() {
        return this.url;
    }
    public JsonObject getPostBody() {
        return this.postBody;
    }
    public Response getResponse() {
        return this.response;
    }
}
