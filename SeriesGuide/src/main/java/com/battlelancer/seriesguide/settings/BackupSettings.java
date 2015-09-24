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
import android.support.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Settings related to creating and restoring backups of the database.
 */
public class BackupSettings {

    public static final String KEY_SHOWS_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.showsExport";
    public static final String KEY_SHOWS_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.showsImport";
    public static final String KEY_LISTS_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.listsExport";
    public static final String KEY_LISTS_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.listsImport";
    public static final String KEY_MOVIES_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.moviesExport";
    public static final String KEY_MOVIES_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.moviesImport";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            KEY_SHOWS_EXPORT_URI,
            KEY_SHOWS_IMPORT_URI,
            KEY_LISTS_EXPORT_URI,
            KEY_LISTS_IMPORT_URI,
            KEY_MOVIES_EXPORT_URI,
            KEY_MOVIES_IMPORT_URI
    })
    public @interface FileUriSettingsKey {
    }

    /**
     * Store or remove (by setting it {@code null}) the URI to a backup file.
     */
    public static boolean storeFileUri(Context context, @FileUriSettingsKey String key,
            @Nullable Uri uri) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, uri == null ? null : uri.toString())
                .commit();
    }

    /**
     * Retrieve a backup file URI. If looking for an import URI and there is none defined, tries to
     * get the export URI.
     */
    @Nullable
    public static Uri getFileUri(Context context, @FileUriSettingsKey String key) {
        String uriString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key, null);
        if (uriString == null) {
            // if there is no import uri, try to fall back to the export file uri
            if (KEY_SHOWS_IMPORT_URI.equals(key)) {
                return getFileUri(context, KEY_SHOWS_EXPORT_URI);
            } else if (KEY_LISTS_IMPORT_URI.equals(key)) {
                return getFileUri(context, KEY_LISTS_EXPORT_URI);
            } else if (KEY_MOVIES_IMPORT_URI.equals(key)) {
                return getFileUri(context, KEY_MOVIES_EXPORT_URI);
            }
            return null;
        }
        return Uri.parse(uriString);
    }
}
