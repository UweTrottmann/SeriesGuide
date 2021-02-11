package com.battlelancer.seriesguide.sync;

import android.os.Bundle;

public class SyncOptions {

    public enum SyncType {
        DELTA(0),
        SINGLE(1),
        FULL(2),
        JOBS(3);

        public int id;

        SyncType(int id) {
            this.id = id;
        }

        public static SyncType from(int id) {
            return values()[id];
        }
    }

    public final SyncType syncType;
    public final boolean syncImmediately;
    public final long singleShowId;

    public SyncOptions(Bundle extras) {
        syncType = SyncType.from(extras.getInt(SgSyncAdapter.EXTRA_SYNC_TYPE, SyncType.DELTA.id));
        singleShowId = extras.getLong(SgSyncAdapter.EXTRA_SYNC_SHOW_ID, 0);
        syncImmediately = extras.getBoolean(SgSyncAdapter.EXTRA_SYNC_IMMEDIATE, false);
    }

}
