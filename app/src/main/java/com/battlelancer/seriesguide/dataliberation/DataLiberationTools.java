package com.battlelancer.seriesguide.dataliberation;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.ui.shows.ShowTools.Status;
import com.battlelancer.seriesguide.util.Utils;

public class DataLiberationTools {

    /**
     * Transform a string representation of {@link ShowStatusExport}
     * to a {@link Status} to be stored in the database.
     *
     * <p>Falls back to {@link Status#UNKNOWN}.
     */
    public static int encodeShowStatus(@Nullable String status) {
        if (status == null) {
            return Status.UNKNOWN;
        }
        switch (status) {
            case ShowStatusExport.IN_PRODUCTION:
                return Status.IN_PRODUCTION;
            case ShowStatusExport.PILOT:
                return Status.PILOT;
            case ShowStatusExport.CANCELED:
                return Status.CANCELED;
            case ShowStatusExport.UPCOMING:
                return Status.PLANNED;
            case ShowStatusExport.CONTINUING:
                return Status.RETURNING;
            case ShowStatusExport.ENDED:
                return Status.ENDED;
            default:
                return Status.UNKNOWN;
        }
    }

    /**
     * Transform an int representation of {@link Status}
     * to a {@link ShowStatusExport} to
     * be used for exporting data.
     *
     * @param encodedStatus Detection based on {@link Status}.
     */
    public static String decodeShowStatus(int encodedStatus) {
        switch (encodedStatus) {
            case Status.IN_PRODUCTION:
                return ShowStatusExport.IN_PRODUCTION;
            case Status.PILOT:
                return ShowStatusExport.PILOT;
            case Status.CANCELED:
                return ShowStatusExport.CANCELED;
            case Status.PLANNED:
                return ShowStatusExport.UPCOMING;
            case Status.RETURNING:
                return ShowStatusExport.CONTINUING;
            case Status.ENDED:
                return ShowStatusExport.ENDED;
            default:
                return ShowStatusExport.UNKNOWN;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void selectExportFile(Fragment fragment, String suggestedFileName,
            int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // do NOT use the probably correct application/json as it would prevent selecting existing
        // backup files on Android, which re-classifies them as application/octet-stream.
        // also do NOT use application/octet-stream as it prevents selecting backup files from
        // providers where the correct application/json mime type is used, *sigh*
        // so, use application/* and let the provider decide
        intent.setType("application/*");
        intent.putExtra(Intent.EXTRA_TITLE, suggestedFileName);

        Utils.tryStartActivityForResult(fragment, intent, requestCode);
    }

}
