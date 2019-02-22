package com.battlelancer.seriesguide;

import android.database.sqlite.SQLiteException;
import android.util.Log;
import androidx.annotation.Nullable;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.gson.JsonParseException;
import org.threeten.bp.format.DateTimeParseException;
import timber.log.Timber;

/**
 * A customized {@link timber.log.Timber.DebugTree} that logs to Crashlytics.
 * Always drops debug and verbose logs.
 */
public class AnalyticsTree extends Timber.DebugTree {

    public AnalyticsTree() {
    }

    @Override
    protected void log(int priority, String tag, @Nullable String message, Throwable t) {
        // drop empty messages
        if (message == null) {
            return;
        }
        // drop debug and verbose logs
        if (priority == Log.DEBUG || priority == Log.VERBOSE) {
            return;
        }

        // transform priority into string
        String level = null;
        switch (priority) {
            case Log.INFO:
                level = "INFO";
                break;
            case Log.WARN:
                level = "WARN";
                break;
            case Log.ERROR:
                level = "ERROR";
                break;
        }

        // finally log to crashlytics
        CrashlyticsCore.getInstance().log(level + "/" + tag + ": " + message);

        // track some non-fatal exceptions with crashlytics
        if (priority == Log.ERROR) {
            if (t instanceof SQLiteException /* Content provider */
                    || t instanceof JsonParseException /* Retrofit */
                    || t instanceof DateTimeParseException /* TheTVDB */) {
                CrashlyticsCore.getInstance().logException(t);
            }
        }
    }
}
