/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

/**
 * Settings related to creating and restoring backups of the database.
 */
public class BackupSettings {

    public static final String KEY_BACKUP_SHOWS_URI = "com.battlelancer.seriesguide.backup.shows";

    @Nullable
    public static Uri getBackupShowsUri(Context context) {
        String uriString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_BACKUP_SHOWS_URI, null);
        return uriString == null ? null : Uri.parse(uriString);
    }

    public static boolean storeBackupShowsUri(Context context, @Nullable Uri uri) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(BackupSettings.KEY_BACKUP_SHOWS_URI, uri == null ? null : uri.toString())
                .commit();
    }
}
