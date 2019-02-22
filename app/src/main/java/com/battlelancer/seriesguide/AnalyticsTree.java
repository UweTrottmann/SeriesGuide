package com.battlelancer.seriesguide;

import android.database.sqlite.SQLiteException;
import android.util.Log;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTraktException;
import com.battlelancer.seriesguide.util.Utils;
import com.google.gson.JsonParseException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
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

        if (priority == Log.ERROR) {
            // remove any stack trace attached by Timber
            int newLine = message.indexOf('\n');
            if (newLine > 0) {
                message = message.substring(0, newLine);
            }

            // special treatment for some exceptions
            if (t instanceof TvdbTraktException) {
                return; // already tracked as trakt error
            }
            if (t instanceof TvdbException) {
                TvdbException e = (TvdbException) t;
                Throwable cause = e.getCause();
                if (cause instanceof UnknownHostException) {
                    return; // do not track, mostly devices loosing connection
                }
                if (cause instanceof InterruptedIOException) {
                    return; // do not track, mostly timeouts
                }
//                CrashlyticsCore.getInstance().setString("action", message);
                Utils.trackError(AnalyticsEvents.THETVDB_ERROR, e);
                return;
            }
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
//        CrashlyticsCore.getInstance().log(level + "/" + tag + ": " + message);

        // track some non-fatal exceptions with crashlytics
        if (priority == Log.ERROR) {
            if (t instanceof SQLiteException /* Content provider */
                    || t instanceof JsonParseException /* Retrofit */
                    || t instanceof DateTimeParseException /* TheTVDB */) {
//                CrashlyticsCore.getInstance().logException(t);
            }
        }
    }
}
