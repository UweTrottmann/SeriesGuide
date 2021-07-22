package com.battlelancer.seriesguide.dataliberation;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.FragmentDataLiberationBinding;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.material.snackbar.Snackbar;
import kotlinx.coroutines.Job;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * One button export or import of the show database using a JSON file on external storage.
 * Uses Storage Access Framework so no permissions are required.
 */
public class DataLiberationFragment extends Fragment implements
        JsonExportTask.OnTaskProgressListener {

    public static class LiberationResultEvent {

        private final String message;
        private final boolean showIndefinite;

        public LiberationResultEvent() {
            this.message = null;
            this.showIndefinite = false;
        }

        public LiberationResultEvent(String message, String errorCause, boolean showIndefinite) {
            if (errorCause != null) {
                message += " (" + errorCause + ")";
            }
            this.message = message;
            this.showIndefinite = showIndefinite;
        }

        public void handle(@Nullable View view) {
            if (view != null && message != null) {
                Snackbar snackbar = Snackbar.make(view, message,
                        showIndefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_SHORT);
                TextView textView = snackbar.getView().findViewById(
                        com.google.android.material.R.id.snackbar_text);
                textView.setMaxLines(5);
                snackbar.show();
            }
        }
    }

    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;
    private static final int REQUEST_CODE_SHOWS_EXPORT_URI = 3;
    private static final int REQUEST_CODE_SHOWS_IMPORT_URI = 4;
    private static final int REQUEST_CODE_LISTS_EXPORT_URI = 5;
    private static final int REQUEST_CODE_LISTS_IMPORT_URI = 6;
    private static final int REQUEST_CODE_MOVIES_EXPORT_URI = 7;
    private static final int REQUEST_CODE_MOVIES_IMPORT_URI = 8;

    @Nullable
    private FragmentDataLiberationBinding binding;

    @Nullable private Integer type;
    @Nullable private AsyncTask<Void, Integer, Integer> dataLibTask;
    @Nullable private Job dataLibJob;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentDataLiberationBinding binding = FragmentDataLiberationBinding
                .inflate(inflater, container, false);
        this.binding = binding;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FragmentDataLiberationBinding binding = this.binding;
        if (binding == null) return;

        binding.progressBarDataLib.setVisibility(View.GONE);

        // setup listeners
        binding.checkBoxDataLibShows.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateImportButtonEnabledState());
        binding.checkBoxDataLibLists.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateImportButtonEnabledState());
        binding.checkBoxDataLibMovies.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateImportButtonEnabledState());
        binding.buttonDataLibImport.setOnClickListener(v -> doDataLiberationAction(REQUEST_CODE_IMPORT));

        // note: selecting custom backup files is only supported on KitKat and up
        // as we use Storage Access Framework in this case
        binding.buttonDataLibShowsExportFile.setOnClickListener(
                v -> DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_SHOWS,
                        REQUEST_CODE_SHOWS_EXPORT_URI));
        binding.buttonDataLibShowsImportFile.setOnClickListener(
                v -> DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                        REQUEST_CODE_SHOWS_IMPORT_URI));

        binding.buttonDataLibListsExportFile.setOnClickListener(
                v -> DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_LISTS,
                        REQUEST_CODE_LISTS_EXPORT_URI));
        binding.buttonDataLibListsImportFile.setOnClickListener(
                v -> DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                        REQUEST_CODE_LISTS_IMPORT_URI));

        binding.buttonDataLibMoviesExportFile.setOnClickListener(
                v -> DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_MOVIES,
                        REQUEST_CODE_MOVIES_EXPORT_URI));
        binding.buttonDataLibMoviesImportFile.setOnClickListener(
                v -> DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                        REQUEST_CODE_MOVIES_IMPORT_URI));
        updateFileViews();

        // restore UI state
        if (isDataLibTaskNotCompleted()) {
            setProgressLock(true);
        }
    }

    private void updateImportButtonEnabledState() {
        FragmentDataLiberationBinding binding = this.binding;
        if (binding == null) return;

        binding.buttonDataLibImport.setEnabled(
                binding.checkBoxDataLibShows.isChecked()
                        || binding.checkBoxDataLibLists.isChecked()
                        || binding.checkBoxDataLibMovies.isChecked()
        );
    }

    @Override
    public void onStart() {
        super.onStart();

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
        if (isDataLibTaskNotCompleted()) {
            if (dataLibTask != null) {
                dataLibTask.cancel(true);
            }
            if (dataLibJob != null) {
                dataLibJob.cancel(null);
            }
        }
        dataLibTask = null;
        dataLibJob = null;

        super.onDestroy();
    }

    @Override
    public void onProgressUpdate(int total, int completed) {
        FragmentDataLiberationBinding binding = this.binding;
        if (binding == null) return;
        binding.progressBarDataLib.setIndeterminate(total == completed);
        binding.progressBarDataLib.setMax(total);
        binding.progressBarDataLib.setProgress(completed);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LiberationResultEvent event) {
        event.handle(getView());
        if (!isAdded()) {
            // don't touch views if fragment is not added to activity any longer
            return;
        }
        updateFileViews();
        setProgressLock(false);
    }

    private void setProgressLock(boolean isLocked) {
        FragmentDataLiberationBinding binding = this.binding;
        if (binding == null) return;
        if (isLocked) {
            binding.buttonDataLibImport.setEnabled(false);
        } else {
            updateImportButtonEnabledState();
        }
        binding.progressBarDataLib.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        binding.checkBoxDataLibFullDump.setEnabled(!isLocked);
        binding.buttonDataLibShowsExportFile.setEnabled(!isLocked);
        binding.buttonDataLibShowsImportFile.setEnabled(!isLocked);
        binding.buttonDataLibListsExportFile.setEnabled(!isLocked);
        binding.buttonDataLibListsImportFile.setEnabled(!isLocked);
        binding.buttonDataLibMoviesExportFile.setEnabled(!isLocked);
        binding.buttonDataLibMoviesImportFile.setEnabled(!isLocked);
        binding.checkBoxDataLibShows.setEnabled(!isLocked);
        binding.checkBoxDataLibLists.setEnabled(!isLocked);
        binding.checkBoxDataLibMovies.setEnabled(!isLocked);
    }

    private void doDataLiberationAction(int requestCode) {
        FragmentDataLiberationBinding binding = this.binding;
        if (binding == null) return;
        if (requestCode == REQUEST_CODE_EXPORT) {
            setProgressLock(true);

            JsonExportTask exportTask = new JsonExportTask(requireContext(),
                    DataLiberationFragment.this,
                    binding.checkBoxDataLibFullDump.isChecked(), false, type);
            dataLibJob = exportTask.launch();
        } else if (requestCode == REQUEST_CODE_IMPORT) {
            setProgressLock(true);

            dataLibTask = new JsonImportTask(requireContext(),
                    binding.checkBoxDataLibShows.isChecked(), binding.checkBoxDataLibLists.isChecked(),
                    binding.checkBoxDataLibMovies.isChecked());
            Utils.executeInOrder(dataLibTask);
        }
    }

    private boolean isDataLibTaskNotCompleted() {
        return (dataLibTask != null && dataLibTask.getStatus() != AsyncTask.Status.FINISHED)
                || (dataLibJob != null && !dataLibJob.isCompleted());
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || !isAdded() || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return; // required
        }
        if (requestCode == REQUEST_CODE_SHOWS_EXPORT_URI
                || requestCode == REQUEST_CODE_SHOWS_IMPORT_URI
                || requestCode == REQUEST_CODE_LISTS_EXPORT_URI
                || requestCode == REQUEST_CODE_LISTS_IMPORT_URI
                || requestCode == REQUEST_CODE_MOVIES_EXPORT_URI
                || requestCode == REQUEST_CODE_MOVIES_IMPORT_URI) {

            // try to persist read and write permission for this URI across device reboots
            try {
                requireContext().getContentResolver()
                        .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (SecurityException e) {
                Timber.e(e, "Could not persist r/w permission for backup file URI.");
            }

            switch (requestCode) {
                case REQUEST_CODE_SHOWS_EXPORT_URI:
                    type = JsonExportTask.BACKUP_SHOWS;
                    BackupSettings.storeExportFileUri(getContext(), type, uri, false);
                    doDataLiberationAction(REQUEST_CODE_EXPORT);
                    break;
                case REQUEST_CODE_LISTS_EXPORT_URI:
                    type = JsonExportTask.BACKUP_LISTS;
                    BackupSettings.storeExportFileUri(getContext(), type, uri, false);
                    doDataLiberationAction(REQUEST_CODE_EXPORT);
                    break;
                case REQUEST_CODE_MOVIES_EXPORT_URI:
                    type = JsonExportTask.BACKUP_MOVIES;
                    BackupSettings.storeExportFileUri(getContext(), type, uri, false);
                    doDataLiberationAction(REQUEST_CODE_EXPORT);
                    break;
                case REQUEST_CODE_SHOWS_IMPORT_URI:
                    BackupSettings.storeImportFileUri(getContext(),
                            JsonExportTask.BACKUP_SHOWS, uri);
                    break;
                case REQUEST_CODE_LISTS_IMPORT_URI:
                    BackupSettings.storeImportFileUri(getContext(),
                            JsonExportTask.BACKUP_LISTS, uri);
                    break;
                default:
                    BackupSettings.storeImportFileUri(getContext(),
                            JsonExportTask.BACKUP_MOVIES, uri);
                    break;
            }
            updateFileViews();
        }
    }

    private void updateFileViews() {
        FragmentDataLiberationBinding binding = this.binding;
        if (binding == null) return;

        setUriOrPlaceholder(binding.textViewDataLibShowsExportFile,
                BackupSettings.getExportFileUri(
                        getContext(), JsonExportTask.BACKUP_SHOWS, false));
        setUriOrPlaceholder(binding.textViewDataLibListsExportFile,
                BackupSettings.getExportFileUri(
                        getContext(), JsonExportTask.BACKUP_LISTS, false));
        setUriOrPlaceholder(binding.textViewDataLibMoviesExportFile,
                BackupSettings.getExportFileUri(
                        getContext(), JsonExportTask.BACKUP_MOVIES, false));

        setUriOrPlaceholder(binding.textViewDataLibShowsImportFile,
                BackupSettings.getImportFileUriOrExportFileUri(
                        getContext(), JsonExportTask.BACKUP_SHOWS));
        setUriOrPlaceholder(binding.textViewDataLibListsImportFile,
                BackupSettings.getImportFileUriOrExportFileUri(
                        getContext(), JsonExportTask.BACKUP_LISTS));
        setUriOrPlaceholder(binding.textViewDataLibMoviesImportFile,
                BackupSettings.getImportFileUriOrExportFileUri(
                        getContext(), JsonExportTask.BACKUP_MOVIES));
    }

    private void setUriOrPlaceholder(TextView textView, Uri uri) {
        textView.setText(uri == null ? getString(R.string.no_file_selected) : uri.toString());
    }
}
