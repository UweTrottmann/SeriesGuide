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

package com.battlelancer.seriesguide.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.TaskManager;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import java.io.File;
import java.io.IOException;

/**
 * Allows to back up or restore the show database to external storage.
 */
public class BackupDeleteActivity extends BaseActivity {

    private static final String TAG = "Backup";

    private static final int EXPORT_DIALOG = 0;

    private static final int IMPORT_DIALOG = 1;

    private static final int EXPORT_PROGRESS = 3;

    private static final int IMPORT_PROGRESS = 4;

    private Button mExportDbToSdButton;

    private Button mImportDbFromSdButton;

    private ExportDatabaseTask mExportTask;

    private ImportDatabaseTask mImportTask;

    private ProgressDialog importProgress;

    private ProgressDialog exportProgress;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup);

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getString(R.string.backup));
        actionBar.setDisplayShowTitleEnabled(true);
    }

    private void setupViews() {
        mExportDbToSdButton = (Button) findViewById(R.id.ButtonExportDBtoSD);
        mExportDbToSdButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                showDialog(EXPORT_DIALOG);
            }
        });

        mImportDbFromSdButton = (Button) findViewById(R.id.ButtonImportDBfromSD);
        mImportDbFromSdButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                showDialog(IMPORT_DIALOG);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onCancelTasks();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return false;
    }

    private void onCancelTasks() {
        if (mImportTask != null && mImportTask.getStatus() == AsyncTask.Status.RUNNING) {
            mImportTask.cancel(true);
            mImportTask = null;
        }
        if (mExportTask != null && mExportTask.getStatus() == AsyncTask.Status.RUNNING) {
            mExportTask.cancel(true);
            mExportTask = null;
        }
    }

    private class ExportDatabaseTask extends AsyncTask<Void, Void, String> {

        // can use UI thread here
        @Override
        protected void onPreExecute() {
            showDialog(EXPORT_PROGRESS);
        }

        // automatically done on worker thread (separate from UI thread)
        @Override
        protected String doInBackground(final Void... args) {
            TaskManager tm = TaskManager.getInstance(BackupDeleteActivity.this);
            if (SgSyncAdapter.isSyncActive(BackupDeleteActivity.this, false)
                    || tm.isAddTaskRunning()) {
                return getString(R.string.update_inprogress);
            }

            File dbFile = getApplication().getDatabasePath(SeriesGuideDatabase.DATABASE_NAME);

            File exportDir = new File(Environment.getExternalStorageDirectory(),
                    "seriesguidebackup");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File file = new File(exportDir, dbFile.getName());

            if (isCancelled()) {
                return null;
            }

            String errorMsg = null;
            try {
                file.createNewFile();
                AndroidUtils.copyFile(dbFile, file);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                errorMsg = e.getMessage();
            }
            return errorMsg;
        }

        // can use UI thread here
        @Override
        protected void onPostExecute(final String errorMsg) {
            if (exportProgress.isShowing()) {
                exportProgress.dismiss();
            }
            if (errorMsg == null) {
                Toast.makeText(BackupDeleteActivity.this, getString(R.string.backup_success),
                        Toast.LENGTH_SHORT).show();
                EasyTracker.getInstance(BackupDeleteActivity.this).send(
                        MapBuilder.createEvent(TAG, "Backup", "Success", null).build()
                );
            } else {
                Toast.makeText(BackupDeleteActivity.this,
                        getString(R.string.backup_failed) + " - " + errorMsg, Toast.LENGTH_LONG)
                        .show();
                EasyTracker.getInstance(BackupDeleteActivity.this).send(
                        MapBuilder.createEvent(TAG, "Backup", "Failure", null).build()
                );
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private class ImportDatabaseTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            showDialog(IMPORT_PROGRESS);
        }

        // could pass the params used here in AsyncTask<String, Void, String> -
        // but not being re-used
        @Override
        protected String doInBackground(final Void... args) {
            TaskManager tm = TaskManager.getInstance(BackupDeleteActivity.this);
            if (SgSyncAdapter.isSyncActive(BackupDeleteActivity.this, false)
                    || tm.isAddTaskRunning()) {
                return getString(R.string.update_inprogress);
            }

            File dbBackupFile = new File(Environment.getExternalStorageDirectory()
                    + "/seriesguidebackup/seriesdatabase");
            if (!dbBackupFile.exists()) {
                return getString(R.string.import_failed_nofile);
            } else if (!dbBackupFile.canRead()) {
                return getString(R.string.import_failed_noread);
            }

            if (isCancelled()) {
                return null;
            }

            File dbFile = getApplication().getDatabasePath(SeriesGuideDatabase.DATABASE_NAME);

            getApplication().deleteDatabase(SeriesGuideDatabase.DATABASE_NAME);

            try {
                dbFile.createNewFile();
                AndroidUtils.copyFile(dbBackupFile, dbFile);

                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                        .putBoolean(SeriesGuidePreferences.KEY_DATABASEIMPORTED, true).commit();
                getContentResolver().notifyChange(Shows.CONTENT_URI, null);

                // wait a little for the new db to settle in
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                }

                // tell user something might have gone wrong if there are no
                // shows in the database right now
                try {
                    final Cursor shows = getContentResolver().query(Shows.CONTENT_URI,
                            new String[] {
                                Shows._ID
                            }, null, null, null);
                    if (shows != null) {
                        if (shows.getCount() == 0) {
                            return getString(R.string.dbupgradefailed);
                        }
                        shows.close();
                    }
                } catch (SQLiteException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return e.getMessage();
                }

                return null;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(final String errMsg) {
            if (importProgress.isShowing()) {
                importProgress.dismiss();
            }
            if (errMsg == null) {
                Toast.makeText(BackupDeleteActivity.this, getString(R.string.import_success),
                        Toast.LENGTH_SHORT).show();
                EasyTracker.getInstance(BackupDeleteActivity.this).send(
                        MapBuilder.createEvent(TAG, "Import", "Success", null).build()
                );

            } else {
                Toast.makeText(BackupDeleteActivity.this,
                        getString(R.string.import_failed) + " - " + errMsg, Toast.LENGTH_LONG)
                        .show();
                EasyTracker.getInstance(BackupDeleteActivity.this).send(
                        MapBuilder.createEvent(TAG, "Import", "Failure", null).build()
                );
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case EXPORT_DIALOG:
                return new AlertDialog.Builder(BackupDeleteActivity.this)
                        .setMessage(getString(R.string.backup_question))
                        .setPositiveButton(getString(R.string.backup_button),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        if (AndroidUtils.isExtStorageAvailable()) {
                                            mExportTask = new ExportDatabaseTask();
                                            mExportTask.execute();
                                        } else {
                                            Toast.makeText(BackupDeleteActivity.this,
                                                    getString(R.string.backup_failed_nosd),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }).setNegativeButton(getString(R.string.backup_no), null).create();
            case IMPORT_DIALOG:
                return new AlertDialog.Builder(BackupDeleteActivity.this)
                        .setMessage(getString(R.string.import_question))
                        .setPositiveButton(getString(R.string.import_button),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        if (AndroidUtils.isExtStorageAvailable()) {
                                            mImportTask = new ImportDatabaseTask();
                                            mImportTask.execute();
                                        } else {
                                            Toast.makeText(BackupDeleteActivity.this,
                                                    getString(R.string.import_failed_nosd),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }).setNegativeButton(getString(R.string.import_no), null).create();
            case EXPORT_PROGRESS:
                exportProgress = new ProgressDialog(BackupDeleteActivity.this);
                exportProgress.setMessage(getString(R.string.backup_inprogress));
                return exportProgress;
            case IMPORT_PROGRESS:
                importProgress = new ProgressDialog(BackupDeleteActivity.this);
                importProgress.setMessage(getString(R.string.import_inprogress));
                return importProgress;
        }
        return null;
    }
}
