package com.battlelancer.seriesguide;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.support.annotation.Nullable;
import android.util.Log;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.Utils;
import com.crashlytics.android.Crashlytics;
import java.net.UnknownHostException;
import timber.log.Timber;

/**
 * A customized {@link timber.log.Timber.DebugTree} that logs to Crashlytics and Google Analytics.
 * Always drops debug and verbose logs.
 */
public class AnalyticsTree extends Timber.DebugTree {

    public static final String CATEGORY_THETVDB_ERROR = "TheTVDB Error";

    private final Context context;

    public AnalyticsTree(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected void log(int priority, String tag, @Nullable String message, Throwable t) {
        if (priority == Log.ERROR) {
            // remove any stack trace attached by Timber
            if (message != null) {
                int newLine = message.indexOf('\n');
                if (newLine > 0) {
                    message = message.substring(0, newLine);
                }
            }

            // special treatment for some exceptions
            if (t instanceof TvdbException) {
                TvdbException e = (TvdbException) t;
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof UnknownHostException) {
                    return; // do not track
                }
                Utils.trackCustomEvent(context,
                        CATEGORY_THETVDB_ERROR,
                        tag + ": " + message,
                        e.getMessage());
                return;
            }
        }

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
        Crashlytics.log(level + "/" + tag + ": " + message);

        // track some non-fatal exceptions with crashlytics
        if (priority == Log.ERROR) {
            if (t instanceof SQLiteException) {
                Crashlytics.logException(t);
            }
        }
    }
}
