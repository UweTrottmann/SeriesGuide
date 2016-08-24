package com.battlelancer.seriesguide.interfaces;

/**
 * Used by tasks to report finishing their activity, e.g. {@link com.battlelancer.seriesguide.dataliberation.JsonExportTask}
 * and {@link com.battlelancer.seriesguide.dataliberation.JsonImportTask}.
 */
public interface OnTaskFinishedListener {
    void onTaskFinished();
}
