package com.battlelancer.seriesguide.util;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.os.RemoteException;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import java.util.ArrayList;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

public class DBUtils {

    private static final int SMALL_BATCH_SIZE = 50;

    public static class DatabaseErrorEvent {

        private final String message;
        private final boolean isCorrupted;

        DatabaseErrorEvent(String message, boolean isCorrupted) {
            this.message = message;
            this.isCorrupted = isCorrupted;
        }

        public void handle(Context context) {
            StringBuilder errorText = new StringBuilder(context.getString(R.string.database_error));
            if (isCorrupted) {
                errorText.append(" ").append(context.getString(R.string.reinstall_info));
            }
            errorText.append(" (").append(message).append(")");
            Toast.makeText(context, errorText, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Post an event to simply show a toast with the error message.
     */
    public static void postDatabaseError(SQLiteException e) {
        EventBus.getDefault()
                .post(new DatabaseErrorEvent(e.getMessage(),
                        e instanceof SQLiteDatabaseCorruptException));
    }

    /**
     * Maps a {@link java.lang.Boolean} object to an int value to store in the database.
     */
    public static int convertBooleanToInt(Boolean value) {
        if (value == null) {
            return 0;
        }
        return value ? 1 : 0;
    }

    /**
     * Applies a large {@link ContentProviderOperation} batch in smaller batches as not to overload
     * the transaction cache.
     */
    public static void applyInSmallBatches(Context context,
            ArrayList<ContentProviderOperation> batch) throws OperationApplicationException {
        // split into smaller batches to not overload transaction cache
        // see http://developer.android.com/reference/android/os/TransactionTooLargeException.html

        ArrayList<ContentProviderOperation> smallBatch = new ArrayList<>();

        while (!batch.isEmpty()) {
            if (batch.size() <= SMALL_BATCH_SIZE) {
                // small enough already? apply right away
                applyBatch(context, batch);
                return;
            }

            // take up to 50 elements out of batch
            for (int count = 0; count < SMALL_BATCH_SIZE; count++) {
                if (batch.isEmpty()) {
                    break;
                }
                smallBatch.add(batch.remove(0));
            }

            // apply small batch
            applyBatch(context, smallBatch);

            // prepare for next small batch
            smallBatch.clear();
        }
    }

    private static void applyBatch(Context context, ArrayList<ContentProviderOperation> batch)
            throws OperationApplicationException {
        try {
            context.getContentResolver()
                    .applyBatch(SgApp.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // not using a remote provider, so this should never happen. crash if it does.
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (SQLiteException e) {
            Timber.e(e, "applyBatch: failed, database error.");
            postDatabaseError(e);
        }
    }
}
