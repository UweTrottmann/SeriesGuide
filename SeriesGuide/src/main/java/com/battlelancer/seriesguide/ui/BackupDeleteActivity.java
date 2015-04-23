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

package com.battlelancer.seriesguide.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.io.File;
import java.io.IOException;
import timber.log.Timber;

/**
 * <b>DEPRECATED.</b> Just keeping this around for legacy users. Copying the database file for
 * backup is dangerous and error prone.
 *
 * <p>Also the tasks in this class reference the activity, which in itself is badly designed (they
 * should be static).
 *
 * <p>Allows to back up or restore the show database to external storage.
 */
public class BackupDeleteActivity extends BaseActivity {

    private AsyncTask<Void, Void, String> mTask;
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        setupActionBar();

        setupViews();
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getString(R.string.backup));
        actionBar.setDisplayShowTitleEnabled(true);
    }

    private void setupViews() {
        Button exportDbToSdButton = (Button) findViewById(R.id.ButtonExportDBtoSD);
        exportDbToSdButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                showExportDialog();
            }
        });

        Button importDbFromSdButton = (Button) findViewById(R.id.ButtonImportDBfromSD);
        importDbFromSdButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                showImportDialog();
            }
        });

        // display backup path
        TextView backuppath = (TextView) findViewById(R.id.textViewBackupPath);
        String path = getBackupFolder().toString();
        backuppath.setText(getString(R.string.backup_path) + ": " + path);

        // display current db version
        TextView dbVersion = (TextView) findViewById(R.id.textViewBackupDatabaseVersion);
        dbVersion.setText(getString(R.string.backup_version) + ": "
                + SeriesGuideDatabase.DATABASE_VERSION);
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
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }
        mTask = null;
    }

    private File getBackupFolder() {
        return new File(Environment.getExternalStorageDirectory(), "seriesguidebackup");
    }

    private class ExportDatabaseTask extends AsyncTask<Void, Void, String> {

        // can use UI thread here
        @Override
        protected void onPreExecute() {
            showProgressDialog(R.string.backup_inprogress);
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

            File exportDir = getBackupFolder();
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
                Timber.e(e, "Creating backup failed");
                errorMsg = e.getMessage();
            }
            return errorMsg;
        }

        // can use UI thread here
        @Override
        protected void onPostExecute(final String errorMsg) {
            hideProgressDialog();
            if (errorMsg == null) {
                Toast.makeText(BackupDeleteActivity.this, getString(R.string.backup_success),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(BackupDeleteActivity.this,
                        getString(R.string.backup_failed) + " - " + errorMsg, Toast.LENGTH_LONG)
                        .show();
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private class ImportDatabaseTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            showProgressDialog(R.string.import_inprogress);
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

            File dbBackupFile = new File(getBackupFolder(), "seriesdatabase");
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
                    Timber.e(e, "Failed to sleep");
                }

                // tell user something might have gone wrong if there are no
                // shows in the database right now
                try {
                    final Cursor shows = getContentResolver().query(Shows.CONTENT_URI,
                            new String[] {
                                    Shows._ID
                            }, null, null, null
                    );
                    if (shows != null) {
                        if (shows.getCount() == 0) {
                            return getString(R.string.dbupgradefailed);
                        }
                        shows.close();
                    }
                } catch (SQLiteException e) {
                    Timber.e(e, "Failed to import backup");
                    return e.getMessage();
                }

                return null;
            } catch (IOException e) {
                Timber.e(e, "Failed to import backup");
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(final String errMsg) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (errMsg == null) {
                Toast.makeText(BackupDeleteActivity.this, getString(R.string.import_success),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(BackupDeleteActivity.this,
                        getString(R.string.import_failed) + " - " + errMsg, Toast.LENGTH_LONG)
                        .show();
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showProgressDialog(int messageId) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(getString(messageId));
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    private boolean isExternalStorageAvailable(int errorMessageID) {
        boolean extStorageAvailable = AndroidUtils.isExtStorageAvailable();
        if (!extStorageAvailable) {
            Toast.makeText(BackupDeleteActivity.this, getString(errorMessageID), Toast.LENGTH_LONG)
                    .show();
        }
        return extStorageAvailable;
    }

    private void showExportDialog() {
        ExportDialogFragment f = new ExportDialogFragment();
        f.show(getSupportFragmentManager(), "export-dialog");
    }

    private void showImportDialog() {
        ImportDialogFragment f = new ImportDialogFragment();
        f.show(getSupportFragmentManager(), "import-dialog");
    }

    public void exportDatabase() {
        if (isExternalStorageAvailable(R.string.backup_failed_nosd)) {
            mTask = new ExportDatabaseTask();
            Utils.executeInOrder(mTask);
        }
    }

    public void importDatabase() {
        if (isExternalStorageAvailable(R.string.import_failed_nosd)) {
            mTask = new ImportDatabaseTask();
            Utils.executeInOrder(mTask);
        }
    }

    public static class ExportDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.backup_question))
                    .setPositiveButton(getString(R.string.backup_button),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    ((BackupDeleteActivity) getActivity()).exportDatabase();
                                }
                            }
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
    }

    public static class ImportDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.import_question))
                    .setPositiveButton(getString(R.string.import_button),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    ((BackupDeleteActivity) getActivity()).importDatabase();
                                }
                            }
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
    }
}
