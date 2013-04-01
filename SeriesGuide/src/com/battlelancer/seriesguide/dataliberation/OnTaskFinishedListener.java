
package com.battlelancer.seriesguide.dataliberation;

/**
 * Used by {@link JsonExportTask} and {@link JsonImportTask} to report finishing
 * their activity.
 */
public interface OnTaskFinishedListener {
    public void onTaskFinished();
}
