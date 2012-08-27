/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.uwetrottmann.androidutils.AndroidUtils;

import java.io.File;
import java.io.IOException;

/**
 * Creates a physical copy of the database file and stores it on external
 * storage.
 * 
 * @author Uwe Trottmann
 */
public class BackupTask extends AsyncTask<String, Void, Void> {

    private static final String TAG = "BackupTask";

    private Context mContext;

    public BackupTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {
        if (!AndroidUtils.isExtStorageAvailable()) {
            return null;
        }

        // ensure backup directory exists
        File exportDir = new File(Environment.getExternalStorageDirectory(), "seriesguidebackup");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        if (isCancelled()) {
            return null;
        }

        File original = new File(params[0]);
        File backup = new File(exportDir, original.getName() + "_auto.db");
        try {
            // copy the database file
            AndroidUtils.copyFile(original, backup);

            // store current time = last backup time
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putLong(SeriesGuidePreferences.KEY_LASTBACKUP, System.currentTimeMillis())
                    .commit();

            Log.i(TAG, "Created automatic backup of show database on external storage.");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

}
