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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
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
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.BackupSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Configuration of auto backup, the backup files and restoring the last auto backup.
 */
public class AutoBackupFragment extends Fragment implements OnTaskFinishedListener,
        OnTaskProgressListener {

    private static final int REQUEST_CODE_ENABLE_AUTO_BACKUP = 1;
    private static final int REQUEST_CODE_IMPORT_AUTOBACKUP = 2;
    private static final int REQUEST_CODE_SHOWS_EXPORT_URI = 3;
    private static final int REQUEST_CODE_LISTS_EXPORT_URI = 4;
    private static final int REQUEST_CODE_MOVIES_EXPORT_URI = 5;

    @Bind(R.id.switchAutoBackup) SwitchCompat switchAutoBackup;
    @Bind(R.id.containerAutoBackupSettings) View containerSettings;
    @Bind(R.id.checkBoxAutoBackupDefaultFiles) CheckBox checkBoxDefaultFiles;

    @Bind(R.id.textViewAutoBackupShowsExportFile) TextView textShowsExportFile;
    @Bind(R.id.buttonAutoBackupShowsExportFile) Button buttonShowsExportFile;
    @Bind(R.id.textViewAutoBackupListsExportFile) TextView textListsExportFile;
    @Bind(R.id.buttonAutoBackupListsExportFile) Button buttonListsExportFile;
    @Bind(R.id.textViewAutoBackupMoviesExportFile) TextView textMoviesExportFile;
    @Bind(R.id.buttonAutoBackupMoviesExportFile) Button buttonMoviesExportFile;

    @Bind(R.id.checkBoxAutoBackupImportWarning) CheckBox checkBoxImportWarning;
    @Bind(R.id.textViewAutoBackupLastTime) TextView textViewLastAutoBackup;
    @Bind(R.id.buttonAutoBackupImport) Button buttonImportAutoBackup;
    @Bind(R.id.progressBarAutoBackup) ProgressBar progressBar;

    private AsyncTask<Void, Integer, Integer> importTask;

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
        View v = inflater.inflate(R.layout.fragment_auto_backup, container, false);
        ButterKnife.bind(this, v);

        progressBar.setVisibility(View.GONE);

        // display last auto-backup date
        long lastAutoBackupTime = AdvancedSettings.getLastAutoBackupTime(getActivity());
        textViewLastAutoBackup
                .setText(getString(R.string.last_auto_backup,
                        DataLiberationTools.isAutoBackupAvailable() ?
                                DateUtils.getRelativeDateTimeString(getActivity(),
                                        lastAutoBackupTime, DateUtils.SECOND_IN_MILLIS,
                                        DateUtils.DAY_IN_MILLIS, 0) : "n/a"));

        // setup listeners
        boolean autoBackupEnabled = AdvancedSettings.isAutoBackupEnabled(getContext());
        containerSettings.setVisibility(View.VISIBLE);
        switchAutoBackup.setChecked(autoBackupEnabled);
        switchAutoBackup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tryEnableAutoBackup();
                } else {
                    setAutoBackupEnabled(false);
                }
            }
        });
        checkBoxImportWarning.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonImportAutoBackup.setEnabled(isChecked);
            }
        });
        buttonImportAutoBackup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tryDataLiberationAction(REQUEST_CODE_IMPORT_AUTOBACKUP);
            }
        });

        // selecting custom backup files is only supported on KitKat and up
        // as we use Storage Access Framework in this case
        if (AndroidUtils.isKitKatOrHigher()) {
            checkBoxDefaultFiles.setChecked(
                    BackupSettings.isUseAutoBackupDefaultFiles(getContext()));
            checkBoxDefaultFiles.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    PreferenceManager.getDefaultSharedPreferences(buttonView.getContext())
                            .edit()
                            .putBoolean(BackupSettings.KEY_AUTO_BACKUP_USE_DEFAULT_FILES, isChecked)
                            .commit();
                    updateFileViews();
                }
            });
            buttonShowsExportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectExportFile(JsonExportTask.EXPORT_JSON_FILE_SHOWS,
                            REQUEST_CODE_SHOWS_EXPORT_URI);
                }
            });
            buttonListsExportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectExportFile(JsonExportTask.EXPORT_JSON_FILE_LISTS,
                            REQUEST_CODE_LISTS_EXPORT_URI);
                }
            });
            buttonMoviesExportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectExportFile(JsonExportTask.EXPORT_JSON_FILE_MOVIES,
                            REQUEST_CODE_MOVIES_EXPORT_URI);
                }
            });
        } else {
            checkBoxDefaultFiles.setVisibility(View.GONE);
        }
        updateFileViews();

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // restore UI state
        if (importTask != null && importTask.getStatus() != AsyncTask.Status.FINISHED) {
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
        if (importTask != null && importTask.getStatus() != AsyncTask.Status.FINISHED) {
            importTask.cancel(true);
        }
        importTask = null;

        super.onDestroy();
    }

    @Override
    public void onProgressUpdate(Integer... values) {
        if (progressBar == null) {
            return;
        }
        progressBar.setIndeterminate(values[0].equals(values[1]));
        progressBar.setMax(values[0]);
        progressBar.setProgress(values[1]);
    }

    @Override
    public void onTaskFinished() {
        if (!isAdded()) {
            // don't touch views if fragment is not added to activity any longer
            return;
        }
        if (AndroidUtils.isKitKatOrHigher()) {
            updateFileViews();
        }
        setProgressLock(false);
    }

    private void setProgressLock(boolean isLocked) {
        if (isLocked) {
            buttonImportAutoBackup.setEnabled(false);
        } else {
            buttonImportAutoBackup.setEnabled(checkBoxImportWarning.isChecked());
        }
        progressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        checkBoxImportWarning.setEnabled(!isLocked);
        buttonShowsExportFile.setEnabled(!isLocked);
        buttonListsExportFile.setEnabled(!isLocked);
        buttonMoviesExportFile.setEnabled(!isLocked);
    }

    private void tryEnableAutoBackup() {
        // make sure we have the storage write permission
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // don't have it? request it, do task if granted
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    REQUEST_CODE_ENABLE_AUTO_BACKUP);
            return;
        }
        setAutoBackupEnabled(true);
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
        if (requestCode == REQUEST_CODE_ENABLE_AUTO_BACKUP) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setAutoBackupEnabled(true);
            } else {
                if (getView() != null && switchAutoBackup != null) {
                    // disable auto backup as we don't have the required permission
                    switchAutoBackup.setChecked(false);
                    Snackbar.make(getView(), R.string.autobackup_permission_missing,
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }
        if (requestCode == REQUEST_CODE_IMPORT_AUTOBACKUP) {
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

    private void setAutoBackupEnabled(boolean isEnabled) {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit()
                .putBoolean(AdvancedSettings.KEY_AUTOBACKUP, isEnabled)
                .apply();
        containerSettings.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    private void doDataLiberationAction(int requestCode) {
        if (requestCode == REQUEST_CODE_IMPORT_AUTOBACKUP) {
            setProgressLock(true);

            importTask = new JsonImportTask(getContext(), AutoBackupFragment.this, true);
            Utils.executeInOrder(importTask);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void selectExportFile(String suggestedFileName, int requestCode) {
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

        startActivityForResult(intent, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || !isAdded() || data == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_SHOWS_EXPORT_URI
                || requestCode == REQUEST_CODE_LISTS_EXPORT_URI
                || requestCode == REQUEST_CODE_MOVIES_EXPORT_URI) {
            Uri uri = data.getData();

            // persist read and write permission for this URI across device reboots
            getContext().getContentResolver()
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (requestCode == REQUEST_CODE_SHOWS_EXPORT_URI) {
                BackupSettings.storeFileUri(getContext(),
                        BackupSettings.KEY_AUTO_BACKUP_SHOWS_EXPORT_URI, uri);
            } else if (requestCode == REQUEST_CODE_LISTS_EXPORT_URI) {
                BackupSettings.storeFileUri(getContext(),
                        BackupSettings.KEY_AUTO_BACKUP_LISTS_EXPORT_URI, uri);
            } else {
                BackupSettings.storeFileUri(getContext(),
                        BackupSettings.KEY_AUTO_BACKUP_MOVIES_EXPORT_URI, uri);
            }
            updateFileViews();
        }
    }

    private void updateFileViews() {
        if (!BackupSettings.isUseAutoBackupDefaultFiles(getContext())
                && AndroidUtils.isKitKatOrHigher()) {
            setUriOrPlaceholder(textShowsExportFile, BackupSettings.getFileUri(getContext(),
                    BackupSettings.KEY_AUTO_BACKUP_SHOWS_EXPORT_URI));
            setUriOrPlaceholder(textListsExportFile, BackupSettings.getFileUri(getContext(),
                    BackupSettings.KEY_AUTO_BACKUP_LISTS_EXPORT_URI));
            setUriOrPlaceholder(textMoviesExportFile, BackupSettings.getFileUri(getContext(),
                    BackupSettings.KEY_AUTO_BACKUP_MOVIES_EXPORT_URI));
            buttonShowsExportFile.setVisibility(View.VISIBLE);
            buttonListsExportFile.setVisibility(View.VISIBLE);
            buttonMoviesExportFile.setVisibility(View.VISIBLE);
        } else {
            String path = JsonExportTask.getExportPath(true).toString();
            String showsFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_SHOWS;
            textShowsExportFile.setText(showsFilePath);
            String listsFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_LISTS;
            textListsExportFile.setText(listsFilePath);
            String moviesFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_MOVIES;
            textMoviesExportFile.setText(moviesFilePath);
            //noinspection deprecation
            textShowsExportFile.setTextAppearance(getContext(), R.style.TextAppearance_Body);
            //noinspection deprecation
            textListsExportFile.setTextAppearance(getContext(), R.style.TextAppearance_Body);
            //noinspection deprecation
            textMoviesExportFile.setTextAppearance(getContext(), R.style.TextAppearance_Body);
            buttonShowsExportFile.setVisibility(View.GONE);
            buttonListsExportFile.setVisibility(View.GONE);
            buttonMoviesExportFile.setVisibility(View.GONE);
        }
    }

    private void setUriOrPlaceholder(TextView textView, Uri uri) {
        textView.setText(uri == null ? getString(R.string.no_file_selected) : uri.toString());
        //noinspection deprecation
        textView.setTextAppearance(textView.getContext(),
                uri == null ? R.style.TextAppearance_Body_Highlight_Red
                        : R.style.TextAppearance_Body);
    }
}
