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

import java.io.File;

public class DataLiberationTools {

    /**
     * Returns if auto backup files exist and are readable.
     */
    public static boolean isAutoBackupAvailable() {
        File pathAutoBackup = JsonExportTask.getExportPath(true);
        File backupShows = new File(pathAutoBackup, JsonExportTask.EXPORT_JSON_FILE_SHOWS);
        File backupLists = new File(pathAutoBackup, JsonExportTask.EXPORT_JSON_FILE_LISTS);
        return (backupShows.exists() && backupShows.canRead())
                || (backupLists.exists() && backupLists.canRead());
    }
}
