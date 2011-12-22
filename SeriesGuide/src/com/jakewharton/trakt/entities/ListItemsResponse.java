package com.jakewharton.trakt.entities;

import com.google.myjson.JsonArray;
import com.google.myjson.annotations.SerializedName;

public class ListItemsResponse extends Response {
    private static final long serialVersionUID = 8123553856114248596L;

    public Integer inserted;
    @SerializedName("already_exist") public Integer alreadyExist;
    public Integer skipped;
    @SerializedName("skipped_array") public JsonArray skippedArray;

    /** @deprecated Use {@link #inserted} */
    @Deprecated
    public Integer getInserted() {
        return this.inserted;
    }
    /** @deprecated Use {@link #alreadyExist} */
    @Deprecated
    public Integer getAlreadyExist() {
        return this.alreadyExist;
    }
    /** @deprecated Use {@link #skipped} */
    @Deprecated
    public Integer getSkipped() {
        return this.skipped;
    }
    /** @deprecated Use {@link #skippedArray} */
    @Deprecated
    public JsonArray getSkippedArray() {
        return this.skippedArray;
    }
}
