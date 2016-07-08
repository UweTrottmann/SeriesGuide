package com.battlelancer.seriesguide.interfaces;

/**
 * Used by tasks to report their activity, e.g. {@link com.battlelancer.seriesguide.dataliberation.JsonExportTask}.
 */
public interface OnTaskProgressListener {
    void onProgressUpdate(Integer... values);
}
