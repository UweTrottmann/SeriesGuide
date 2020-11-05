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
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.FragmentAutoBackupBinding;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.BackupSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.material.snackbar.Snackbar;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Configuration of auto backup, the backup files and restoring the last auto backup.
 */
public class AutoBackupFragment extends Fragment implements JsonExportTask.OnTaskProgressListener {

    private static final int REQUEST_CODE_ENABLE_AUTO_BACKUP = 1;
    private static final int REQUEST_CODE_IMPORT_AUTOBACKUP = 2;
    private static final int REQUEST_CODE_SHOWS_EXPORT_URI = 3;
    private static final int REQUEST_CODE_LISTS_EXPORT_URI = 4;
    private static final int REQUEST_CODE_MOVIES_EXPORT_URI = 5;

    private AsyncTask<Void, Integer, Integer> importTask;
    private FragmentAutoBackupBinding binding;

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
        binding = FragmentAutoBackupBinding
                .inflate(inflater, container, false);

        binding.progressBarAutoBackup.setVisibility(View.GONE);

        // setup listeners
        binding.switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tryEnableAutoBackup();
            } else {
                DataLiberationTools.setAutoBackupDisabled(getContext());
                setContainerSettingsVisible(false);
            }
        });
        binding.buttonAutoBackupImport.setOnClickListener(view -> tryDataLiberationAction());

        // selecting custom backup files is only supported on KitKat and up
        // as we use Storage Access Framework in this case
        if (AndroidUtils.isKitKatOrHigher()) {
            binding.checkBoxAutoBackupDefaultFiles.setChecked(
                    BackupSettings.isUseAutoBackupDefaultFiles(getContext()));
            binding.checkBoxAutoBackupDefaultFiles
                    .setOnCheckedChangeListener((buttonView, isChecked) -> {
                        PreferenceManager.getDefaultSharedPreferences(buttonView.getContext())
                                .edit()
                                .putBoolean(BackupSettings.KEY_AUTO_BACKUP_USE_DEFAULT_FILES,
                                        isChecked)
                                .apply();
                        updateFileViews();
                    });
            binding.buttonAutoBackupShowsExportFile.setOnClickListener(view ->
                    DataLiberationTools.selectExportFile(AutoBackupFragment.this,
                            JsonExportTask.EXPORT_JSON_FILE_SHOWS, REQUEST_CODE_SHOWS_EXPORT_URI));
            binding.buttonAutoBackupListsExportFile.setOnClickListener(view ->
                    DataLiberationTools.selectExportFile(AutoBackupFragment.this,
                            JsonExportTask.EXPORT_JSON_FILE_LISTS, REQUEST_CODE_LISTS_EXPORT_URI));
            binding.buttonAutoBackupMoviesExportFile.setOnClickListener(view ->
                    DataLiberationTools.selectExportFile(AutoBackupFragment.this,
                            JsonExportTask.EXPORT_JSON_FILE_MOVIES,
                            REQUEST_CODE_MOVIES_EXPORT_URI));
        } else {
            binding.checkBoxAutoBackupDefaultFiles.setVisibility(View.GONE);
        }
        updateFileViews();

        return binding.getRoot();
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
    public void onStart() {
        super.onStart();

        // auto-disable if permission is missing
        if (DataLiberationTools.isAutoBackupPermissionMissing(getContext())) {
            DataLiberationTools.setAutoBackupDisabled(getContext());
        }

        // update enabled state
        boolean autoBackupEnabled = AdvancedSettings.isAutoBackupEnabled(getContext());
        setContainerSettingsVisible(autoBackupEnabled);
        binding.switchAutoBackup.setChecked(autoBackupEnabled);

        // update last auto-backup date
        long lastAutoBackupTime = AdvancedSettings.getLastAutoBackupTime(getActivity());
        boolean showLastBackupTime = BackupSettings.isUseAutoBackupDefaultFiles(getContext())
                ? DataLiberationTools.isAutoBackupDefaultFilesAvailable()
                : !BackupSettings.isMissingAutoBackupFile(getContext());
        binding.textViewAutoBackupLastTime
                .setText(getString(R.string.last_auto_backup, showLastBackupTime ?
                        DateUtils.getRelativeDateTimeString(getActivity(),
                                lastAutoBackupTime, DateUtils.SECOND_IN_MILLIS,
                                DateUtils.DAY_IN_MILLIS, 0) : "n/a"));

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        if (binding.progressBarAutoBackup == null) {
            return;
        }
        binding.progressBarAutoBackup.setIndeterminate(values[0].equals(values[1]));
        binding.progressBarAutoBackup.setMax(values[0]);
        binding.progressBarAutoBackup.setProgress(values[1]);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DataLiberationFragment.LiberationResultEvent event) {
        event.handle(getView());
        if (!isAdded()) {
            // don't touch views if fragment is not added to activity any longer
            return;
        }
        if (AndroidUtils.isKitKatOrHigher()) {
            updateFileViews();
        }
        setProgressLock(false);
    }

    private boolean permissionRequired(int requestCode) {
        // make sure we have write permission
        if (DataLiberationTools.isAutoBackupPermissionMissing(getContext())) {
            // don't have it? request it, resume task if granted
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    requestCode);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ENABLE_AUTO_BACKUP) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tryEnableAutoBackup();
            } else {
                if (getView() != null && binding.switchAutoBackup != null) {
                    // disable auto backup as we don't have the required permission
                    binding.switchAutoBackup.setChecked(false);
                    Snackbar.make(getView(), R.string.autobackup_permission_missing,
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }
        if (requestCode == REQUEST_CODE_IMPORT_AUTOBACKUP) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tryDataLiberationAction();
            } else {
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.dataliberation_permission_missing,
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    private void tryEnableAutoBackup() {
        if (permissionRequired(REQUEST_CODE_ENABLE_AUTO_BACKUP)) {
            return; // will be called again if we get permission
        }
        DataLiberationTools.setAutoBackupEnabled(getContext());
        setContainerSettingsVisible(true);
    }

    private void tryDataLiberationAction() {
        if (permissionRequired(REQUEST_CODE_IMPORT_AUTOBACKUP)) {
            return; // will be called again if we get permission
        }
        setProgressLock(true);

        importTask = new JsonImportTask(getContext());
        Utils.executeInOrder(importTask);
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
            if (uri == null) return;

            // persist read and write permission for this URI across device reboots
            try {
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } catch (SecurityException e) {
                Timber.e(e, "Could not persist r/w permission for backup file URI.");
            }

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

    private void setContainerSettingsVisible(boolean visible) {
        binding.containerAutoBackupSettings.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setProgressLock(boolean isLocked) {
        if (isLocked) {
            binding.buttonAutoBackupImport.setEnabled(false);
        } else {
            binding.buttonAutoBackupImport.setEnabled(true);
        }
        binding.progressBarAutoBackup.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        binding.buttonAutoBackupShowsExportFile.setEnabled(!isLocked);
        binding.buttonAutoBackupListsExportFile.setEnabled(!isLocked);
        binding.buttonAutoBackupMoviesExportFile.setEnabled(!isLocked);
    }

    private void updateFileViews() {
        if (!BackupSettings.isUseAutoBackupDefaultFiles(getContext())
                && AndroidUtils.isKitKatOrHigher()) {
            setUriOrPlaceholder(binding.textViewAutoBackupShowsExportFile, BackupSettings.getFileUri(getContext(),
                    BackupSettings.KEY_AUTO_BACKUP_SHOWS_EXPORT_URI));
            setUriOrPlaceholder(binding.textViewAutoBackupListsExportFile, BackupSettings.getFileUri(getContext(),
                    BackupSettings.KEY_AUTO_BACKUP_LISTS_EXPORT_URI));
            setUriOrPlaceholder(binding.textViewAutoBackupMoviesExportFile, BackupSettings.getFileUri(getContext(),
                    BackupSettings.KEY_AUTO_BACKUP_MOVIES_EXPORT_URI));
            binding.buttonAutoBackupShowsExportFile.setVisibility(View.VISIBLE);
            binding.buttonAutoBackupListsExportFile.setVisibility(View.VISIBLE);
            binding.buttonAutoBackupMoviesExportFile.setVisibility(View.VISIBLE);
        } else {
            String path = JsonExportTask.getExportPath(true).toString();
            String showsFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_SHOWS;
            binding.textViewAutoBackupShowsExportFile.setText(showsFilePath);
            String listsFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_LISTS;
            binding.textViewAutoBackupListsExportFile.setText(listsFilePath);
            String moviesFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_MOVIES;
            binding.textViewAutoBackupMoviesExportFile.setText(moviesFilePath);
            binding.buttonAutoBackupShowsExportFile.setVisibility(View.GONE);
            binding.buttonAutoBackupListsExportFile.setVisibility(View.GONE);
            binding.buttonAutoBackupMoviesExportFile.setVisibility(View.GONE);
        }
    }

    private void setUriOrPlaceholder(TextView textView, Uri uri) {
        textView.setText(uri == null ? getString(R.string.no_file_selected) : uri.toString());
    }
}
