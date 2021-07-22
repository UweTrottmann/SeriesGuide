package com.battlelancer.seriesguide.dataliberation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.FragmentAutoBackupBinding;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Configuration of auto backup, creation of optional copies
 * and ability to import the last auto backup.
 */
public class AutoBackupFragment extends Fragment {

    private static final int REQUEST_CODE_SHOWS_EXPORT_URI = 3;
    private static final int REQUEST_CODE_LISTS_EXPORT_URI = 4;
    private static final int REQUEST_CODE_MOVIES_EXPORT_URI = 5;

    @Nullable
    private FragmentAutoBackupBinding binding;

    @NonNull
    private AutoBackupViewModel viewModel;
    private boolean isBackupAvailableForImport = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentAutoBackupBinding binding = FragmentAutoBackupBinding
                .inflate(inflater, container, false);
        this.binding = binding;
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FragmentAutoBackupBinding binding = this.binding;
        if (binding == null) return;

        // setup listeners
        binding.switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                BackupSettings.setAutoBackupEnabled(getContext());
                setContainerSettingsVisible(true);
            } else {
                BackupSettings.setAutoBackupDisabled(getContext());
                setContainerSettingsVisible(false);
            }
        });

        binding.buttonAutoBackupNow.setOnClickListener(
                v -> {
                    if (TaskManager.getInstance().tryBackupTask(requireContext())) {
                        setProgressLock(true);
                    }
                }
        );
        binding.buttonAutoBackupImport.setOnClickListener(
                v -> runAutoBackupImport()
        );

        binding.checkBoxAutoBackupCreateCopy.setChecked(
                BackupSettings.isCreateCopyOfAutoBackup(getContext()));
        binding.checkBoxAutoBackupCreateCopy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            BackupSettings.setCreateCopyOfAutoBackup(buttonView.getContext(), isChecked);
            updateFileViews();
        });

        binding.buttonAutoBackupShowsExportFile.setOnClickListener(v ->
                DataLiberationTools.selectExportFile(AutoBackupFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_SHOWS, REQUEST_CODE_SHOWS_EXPORT_URI));
        binding.buttonAutoBackupListsExportFile.setOnClickListener(v ->
                DataLiberationTools.selectExportFile(AutoBackupFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_LISTS, REQUEST_CODE_LISTS_EXPORT_URI));
        binding.buttonAutoBackupMoviesExportFile.setOnClickListener(v ->
                DataLiberationTools.selectExportFile(AutoBackupFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_MOVIES, REQUEST_CODE_MOVIES_EXPORT_URI));

        binding.groupState.setVisibility(View.GONE);
        updateFileViews();
        setProgressLock(false); // Also disables import button if backup availability unknown.

        viewModel = new ViewModelProvider(this).get(AutoBackupViewModel.class);

        // restore UI state
        if (viewModel.isImportTaskNotCompleted()) {
            setProgressLock(true);
        }

        viewModel.getAvailableBackupLiveData()
                .observe(getViewLifecycleOwner(), availableBackupTimeString -> {
                    String lastBackupTimeString = availableBackupTimeString != null
                            ? availableBackupTimeString : "n/a";
                    this.binding.textViewAutoBackupLastTime
                            .setText(getString(R.string.last_auto_backup, lastBackupTimeString));

                    isBackupAvailableForImport = availableBackupTimeString != null;
                    updateImportButtonState();

                    // Also update status of last backup attempt.
                    String errorOrNull = BackupSettings.getAutoBackupErrorOrNull(requireContext());
                    if (errorOrNull != null) {
                        this.binding.groupState.setVisibility(View.VISIBLE);

                        this.binding.imageViewBackupStatus.setImageResource(
                                R.drawable.ic_cancel_red_24dp);
                        this.binding.textViewBackupStatus.setText(
                                getString(R.string.backup_failed) + " " + errorOrNull);
                    } else if (isBackupAvailableForImport) {
                        this.binding.groupState.setVisibility(View.VISIBLE);

                        this.binding.imageViewBackupStatus.setImageResource(
                                R.drawable.ic_check_circle_green_24dp);
                        this.binding.textViewBackupStatus.setText(R.string.backup_success);
                    } else {
                        // No error + no backup files.
                        this.binding.groupState.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();

        // update enabled state
        boolean autoBackupEnabled = BackupSettings.isAutoBackupEnabled(getContext());
        setContainerSettingsVisible(autoBackupEnabled);
        FragmentAutoBackupBinding binding = this.binding;
        if (binding != null) {
            binding.switchAutoBackup.setChecked(autoBackupEnabled);
        }

        // Update auto-backup availability.
        viewModel.updateAvailableBackupData();

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DataLiberationFragment.LiberationResultEvent event) {
        event.handle(getView());
        if (!isAdded()) {
            // don't touch views if fragment is not added to activity any longer
            return;
        }
        viewModel.updateAvailableBackupData();
        updateFileViews();
        setProgressLock(false);
    }

    private void runAutoBackupImport() {
        setProgressLock(true);

        JsonImportTask importTask = new JsonImportTask(getContext());
        viewModel.setImportTask(importTask);
        Utils.executeInOrder(importTask);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || !isAdded() || data == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_SHOWS_EXPORT_URI
                || requestCode == REQUEST_CODE_LISTS_EXPORT_URI
                || requestCode == REQUEST_CODE_MOVIES_EXPORT_URI) {
            Uri uri = data.getData();
            if (uri == null) {
                return;
            }

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

            switch (requestCode) {
                case REQUEST_CODE_SHOWS_EXPORT_URI:
                    BackupSettings.storeExportFileUri(getContext(),
                            JsonExportTask.BACKUP_SHOWS, uri, true);
                    break;
                case REQUEST_CODE_LISTS_EXPORT_URI:
                    BackupSettings.storeExportFileUri(getContext(),
                            JsonExportTask.BACKUP_LISTS, uri, true);
                    break;
                default:
                    BackupSettings.storeExportFileUri(getContext(),
                            JsonExportTask.BACKUP_MOVIES, uri, true);
                    break;
            }

            updateFileViews();
        }
    }

    private void setContainerSettingsVisible(boolean visible) {
        FragmentAutoBackupBinding binding = this.binding;
        if (binding == null) return;
        binding.containerAutoBackupSettings.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateImportButtonState() {
        FragmentAutoBackupBinding binding = this.binding;
        if (binding == null) return;
        binding.buttonAutoBackupImport.setEnabled(isBackupAvailableForImport);
    }

    private void setProgressLock(boolean isLocked) {
        FragmentAutoBackupBinding binding = this.binding;
        if (binding == null) return;
        binding.buttonAutoBackupNow.setEnabled(!isLocked);
        binding.buttonAutoBackupImport.setEnabled(isBackupAvailableForImport && !isLocked);
        binding.progressBarAutoBackup.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        binding.buttonAutoBackupShowsExportFile.setEnabled(!isLocked);
        binding.buttonAutoBackupListsExportFile.setEnabled(!isLocked);
        binding.buttonAutoBackupMoviesExportFile.setEnabled(!isLocked);
    }

    private void updateFileViews() {
        FragmentAutoBackupBinding binding = this.binding;
        if (binding == null) return;
        if (BackupSettings.isCreateCopyOfAutoBackup(getContext())) {
            setUriOrPlaceholder(binding.textViewAutoBackupShowsExportFile,
                    BackupSettings.getExportFileUri(
                            getContext(), JsonExportTask.BACKUP_SHOWS, true));

            setUriOrPlaceholder(binding.textViewAutoBackupListsExportFile,
                    BackupSettings.getExportFileUri(
                            getContext(), JsonExportTask.BACKUP_LISTS, true));

            setUriOrPlaceholder(binding.textViewAutoBackupMoviesExportFile,
                    BackupSettings.getExportFileUri(
                            getContext(), JsonExportTask.BACKUP_MOVIES, true));

            binding.groupUserFiles.setVisibility(View.VISIBLE);
        } else {
            binding.groupUserFiles.setVisibility(View.GONE);
        }
    }

    private void setUriOrPlaceholder(TextView textView, @Nullable Uri uri) {
        textView.setText(uri == null
                ? getString(R.string.no_file_selected)
                : uri.toString());
        TextViewCompat.setTextAppearance(textView, uri == null
                ? R.style.TextAppearance_SeriesGuide_Body2_Error
                : R.style.TextAppearance_SeriesGuide_Body2_Dim);
    }
}
