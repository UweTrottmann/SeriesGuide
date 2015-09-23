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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.interfaces.OnTaskFinishedListener;
import com.battlelancer.seriesguide.interfaces.OnTaskProgressListener;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.BackupSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * One button export or import of the show database using a JSON file on external storage.
 */
public class DataLiberationFragment extends Fragment implements OnTaskFinishedListener,
        OnTaskProgressListener {

    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;
    private static final int REQUEST_CODE_IMPORT_AUTOBACKUP = 3;
    private static final int REQUEST_CODE_SHOWS_EXPORT_URI = 4;
    private static final int REQUEST_CODE_SHOWS_IMPORT_URI = 5;

    @Bind(R.id.containerDataLibExportFiles) View containerCustomExportFiles;
    @Bind(R.id.textViewDataLibShowsExportFile) TextView textShowsExportFile;
    @Bind(R.id.buttonDataLibShowsExportFile) Button buttonShowsExportFile;

    @Bind(R.id.containerDataLibImportFiles) View containerCustomImportFiles;
    @Bind(R.id.textViewDataLibShowsImportFile) TextView textShowsImportFile;
    @Bind(R.id.buttonDataLibShowsImportFile) Button buttonShowsImportFile;

    private Button mButtonExport;
    private Button mButtonImport;
    private Button mButtonImportAutoBackup;
    private ProgressBar mProgressBar;
    private CheckBox mCheckBoxFullDump;
    private CheckBox mCheckBoxImportWarning;
    private AsyncTask<Void, Integer, Integer> mTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Try to keep the fragment around on config changes so the backup task
         * does not have to be finished.
         */
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_data_liberation, container, false);
        ButterKnife.bind(this, v);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBarDataLiberation);
        mProgressBar.setVisibility(View.GONE);
        mButtonExport = (Button) v.findViewById(R.id.buttonExport);
        mButtonImport = (Button) v.findViewById(R.id.buttonImport);
        mButtonImportAutoBackup = (Button) v.findViewById(R.id.buttonBackupRestoreAutoBackup);
        mCheckBoxFullDump = (CheckBox) v.findViewById(R.id.checkBoxFullDump);
        mCheckBoxImportWarning = (CheckBox) v.findViewById(R.id.checkBoxImportWarning);

        // display backup path
        TextView backuppath = (TextView) v.findViewById(R.id.textViewBackupPath);
        String path = JsonExportTask.getExportPath(false).toString();
        backuppath.setText(getString(R.string.backup_path) + ": " + path);

        // display current db version
        TextView dbVersion = (TextView) v.findViewById(R.id.textViewBackupDatabaseVersion);
        dbVersion.setText(getString(R.string.backup_version) + ": "
                + SeriesGuideDatabase.DATABASE_VERSION);

        // display last auto-backup date
        TextView lastAutoBackup = (TextView) v.findViewById(R.id.textViewBackupLastAutoBackup);
        long lastAutoBackupTime = AdvancedSettings.getLastAutoBackupTime(getActivity());
        lastAutoBackup
                .setText(getString(R.string.last_auto_backup,
                        DataLiberationTools.isAutoBackupAvailable() ?
                                DateUtils.getRelativeDateTimeString(getActivity(),
                                        lastAutoBackupTime, DateUtils.SECOND_IN_MILLIS,
                                        DateUtils.DAY_IN_MILLIS, 0) : "n/a"));

        // setup listeners
        mButtonExport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tryDataLiberationAction(REQUEST_CODE_EXPORT);
            }
        });
        mCheckBoxImportWarning.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mButtonImport.setEnabled(isChecked);
                mButtonImportAutoBackup.setEnabled(isChecked);
            }
        });
        mButtonImport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tryDataLiberationAction(REQUEST_CODE_IMPORT);
            }
        });
        mButtonImportAutoBackup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tryDataLiberationAction(REQUEST_CODE_IMPORT_AUTOBACKUP);
            }
        });

        // selecting custom backup files is only supported on KitKat and up
        // as we use Storage Access Framework in this case
        if (AndroidUtils.isKitKatOrHigher()) {
            buttonShowsExportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectExportFile();
                }
            });
            buttonShowsImportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectImportFile();
                }
            });
            updateCustomFileViews();
        } else {
            containerCustomExportFiles.setVisibility(View.GONE);
            containerCustomImportFiles.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // restore UI state
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            setProgressLock(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.unbind(this);
    }

    @Override
    public void onDestroy() {
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }
        mTask = null;

        super.onDestroy();
    }

    @Override
    public void onProgressUpdate(Integer... values) {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setIndeterminate(values[0].equals(values[1]));
        mProgressBar.setMax(values[0]);
        mProgressBar.setProgress(values[1]);
    }

    @Override
    public void onTaskFinished() {
        if (!isAdded()) {
            // don't touch views if fragment is not added to activity any longer
            return;
        }
        if (AndroidUtils.isKitKatOrHigher()) {
            updateCustomFileViews();
        }
        setProgressLock(false);
    }

    private void setProgressLock(boolean isLocked) {
        if (isLocked) {
            mButtonImport.setEnabled(false);
            mButtonImportAutoBackup.setEnabled(false);
        } else {
            mButtonImport.setEnabled(mCheckBoxImportWarning.isChecked());
            mButtonImportAutoBackup.setEnabled(mCheckBoxImportWarning.isChecked());
        }
        mButtonExport.setEnabled(!isLocked);
        mProgressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        mCheckBoxFullDump.setEnabled(!isLocked);
        mCheckBoxImportWarning.setEnabled(!isLocked);
        buttonShowsExportFile.setEnabled(!isLocked);
        buttonShowsImportFile.setEnabled(!isLocked);
    }

    private void tryDataLiberationAction(int requestCode) {
        // make sure we have write permission
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // don't have it? request it, do task if granted
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    requestCode);
            return;
        }

        doDataLiberationAction(requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_EXPORT
                || requestCode == REQUEST_CODE_IMPORT
                || requestCode == REQUEST_CODE_IMPORT_AUTOBACKUP) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doDataLiberationAction(requestCode);
            } else {
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.dataliberation_permission_missing,
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    private void doDataLiberationAction(int requestCode) {
        if (requestCode == REQUEST_CODE_EXPORT) {
            setProgressLock(true);

            mTask = new JsonExportTask(getContext(), DataLiberationFragment.this,
                    DataLiberationFragment.this, mCheckBoxFullDump.isChecked(), false);
            AndroidUtils.executeOnPool(mTask);
        } else if (requestCode == REQUEST_CODE_IMPORT) {
            setProgressLock(true);

            mTask = new JsonImportTask(getContext(), DataLiberationFragment.this, false);
            Utils.executeInOrder(mTask);
        } else if (requestCode == REQUEST_CODE_IMPORT_AUTOBACKUP) {
            setProgressLock(true);

            mTask = new JsonImportTask(getContext(), DataLiberationFragment.this, true);
            Utils.executeInOrder(mTask);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void selectExportFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Create a file with the requested MIME type.
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, JsonExportTask.EXPORT_JSON_FILE_SHOWS);

        startActivityForResult(intent, REQUEST_CODE_SHOWS_EXPORT_URI);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void selectImportFile() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // json files might have mime type of "application/octet-stream"
        // but we are going to store them as "application/json"
        // so filter to show all application files
        intent.setType("application/*");

        startActivityForResult(intent, REQUEST_CODE_SHOWS_IMPORT_URI);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || !isAdded() || data == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_SHOWS_EXPORT_URI
                || requestCode == REQUEST_CODE_SHOWS_IMPORT_URI) {
            Uri uri = data.getData();

            // persist read and write permission for this URI across device reboots
            getContext().getContentResolver()
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (requestCode == REQUEST_CODE_SHOWS_EXPORT_URI) {
                BackupSettings.storeShowsExportUri(getContext(), uri);
            } else {
                BackupSettings.storeShowsImportUri(getContext(), uri);
            }
            updateCustomFileViews();
        }
    }

    private void updateCustomFileViews() {
        Uri showsExportUri = BackupSettings.getShowsExportUri(getContext());
        textShowsExportFile.setText(showsExportUri == null ? getString(R.string.no_file_selected)
                : showsExportUri.toString());
        Uri showsImportUri = BackupSettings.getShowsImportUri(getContext());
        textShowsImportFile.setText(showsImportUri == null ? getString(R.string.no_file_selected)
                : showsImportUri.toString());
    }
}
