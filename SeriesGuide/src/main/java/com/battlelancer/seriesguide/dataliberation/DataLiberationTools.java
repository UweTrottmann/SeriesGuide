/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.dataliberation;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.Utils;
import java.io.File;

public class DataLiberationTools {

    /**
     * Returns if at least one auto backup file in the default folder exists and is readable.
     */
    public static boolean isAutoBackupDefaultFilesAvailable() {
        File pathAutoBackup = JsonExportTask.getExportPath(true);
        File backupShows = new File(pathAutoBackup, JsonExportTask.EXPORT_JSON_FILE_SHOWS);
        File backupLists = new File(pathAutoBackup, JsonExportTask.EXPORT_JSON_FILE_LISTS);
        File backupMovies = new File(pathAutoBackup, JsonExportTask.EXPORT_JSON_FILE_MOVIES);
        return (backupShows.exists() && backupShows.canRead())
                || (backupLists.exists() && backupLists.canRead()
                || (backupMovies.exists() && backupMovies.canRead()));
    }

    /**
     * Transform a string representation of {@link com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport}
     * to a {@link com.battlelancer.seriesguide.util.ShowTools.Status} to be stored in the
     * database.
     *
     * <p>If neither continuing or ended will default to {@link com.battlelancer.seriesguide.util.ShowTools.Status#UNKNOWN}.
     */
    public static int encodeShowStatus(@Nullable String status) {
        if (status == null) {
            return ShowTools.Status.UNKNOWN;
        }
        switch (status) {
            case JsonExportTask.ShowStatusExport.CONTINUING:
                return ShowTools.Status.CONTINUING;
            case JsonExportTask.ShowStatusExport.ENDED:
                return ShowTools.Status.ENDED;
            default:
                return ShowTools.Status.UNKNOWN;
        }
    }

    /**
     * Transform an int representation of {@link com.battlelancer.seriesguide.util.ShowTools.Status}
     * to a {@link com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport} to
     * be used for exporting data.
     *
     * @param encodedStatus Detection based on {@link com.battlelancer.seriesguide.util.ShowTools.Status}.
     */
    public static String decodeShowStatus(int encodedStatus) {
        switch (encodedStatus) {
            case ShowTools.Status.CONTINUING:
                return JsonExportTask.ShowStatusExport.CONTINUING;
            case ShowTools.Status.ENDED:
                return JsonExportTask.ShowStatusExport.ENDED;
            default:
                return JsonExportTask.ShowStatusExport.UNKNOWN;
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

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void selectImportFile(Fragment fragment, int requestCode) {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // json files might have mime type of "application/octet-stream"
        // but we are going to store them as "application/json"
        // so filter to show all application files
        intent.setType("application/*");

        Utils.tryStartActivityForResult(fragment, intent, requestCode);
    }
}
