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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.BackupSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.material.snackbar.Snackbar;
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

    @BindView(R.id.textViewDataLibShowsExportFile) TextView textShowsExportFile;
    @BindView(R.id.buttonDataLibShowsExportFile) Button buttonShowsExportFile;
    @BindView(R.id.textViewDataLibListsExportFile) TextView textListsExportFile;
    @BindView(R.id.buttonDataLibListsExportFile) Button buttonListsExportFile;
    @BindView(R.id.textViewDataLibMoviesExportFile) TextView textMoviesExportFile;
    @BindView(R.id.buttonDataLibMoviesExportFile) Button buttonMoviesExportFile;

    @BindView(R.id.checkBoxDataLibShows) CheckBox checkBoxShows;
    @BindView(R.id.checkBoxDataLibLists) CheckBox checkBoxLists;
    @BindView(R.id.checkBoxDataLibMovies) CheckBox checkBoxMovies;
    @BindView(R.id.textViewDataLibShowsImportFile) TextView textShowsImportFile;
    @BindView(R.id.buttonDataLibShowsImportFile) Button buttonShowsImportFile;
    @BindView(R.id.textViewDataLibListsImportFile) TextView textListsImportFile;
    @BindView(R.id.buttonDataLibListsImportFile) Button buttonListsImportFile;
    @BindView(R.id.textViewDataLibMoviesImportFile) TextView textMoviesImportFile;
    @BindView(R.id.buttonDataLibMoviesImportFile) Button buttonMoviesImportFile;

    @BindView(R.id.buttonDataLibImport) Button buttonImport;
    @BindView(R.id.progressBarDataLib) ProgressBar progressBar;
    @BindView(R.id.checkBoxDataLibFullDump) CheckBox checkBoxFullDump;

    @Nullable private Integer type;
    private AsyncTask<Void, Integer, Integer> dataLibTask;
    private Unbinder unbinder;

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
        View view = inflater.inflate(R.layout.fragment_data_liberation, container, false);
        unbinder = ButterKnife.bind(this, view);

        progressBar.setVisibility(View.GONE);

        // setup listeners
        checkBoxShows.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateImportButtonEnabledState());
        checkBoxLists.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateImportButtonEnabledState());
        checkBoxMovies.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateImportButtonEnabledState());
        buttonImport.setOnClickListener(v -> doDataLiberationAction(REQUEST_CODE_IMPORT));

        // note: selecting custom backup files is only supported on KitKat and up
        // as we use Storage Access Framework in this case
        buttonShowsExportFile.setOnClickListener(
                v -> DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_SHOWS,
                        REQUEST_CODE_SHOWS_EXPORT_URI));
        buttonShowsImportFile.setOnClickListener(
                v -> DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                        REQUEST_CODE_SHOWS_IMPORT_URI));

        buttonListsExportFile.setOnClickListener(
                v -> DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_LISTS,
                        REQUEST_CODE_LISTS_EXPORT_URI));
        buttonListsImportFile.setOnClickListener(
                v -> DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                        REQUEST_CODE_LISTS_IMPORT_URI));

        buttonMoviesExportFile.setOnClickListener(
                v -> DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                        JsonExportTask.EXPORT_JSON_FILE_MOVIES,
                        REQUEST_CODE_MOVIES_EXPORT_URI));
        buttonMoviesImportFile.setOnClickListener(
                v -> DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                        REQUEST_CODE_MOVIES_IMPORT_URI));
        updateFileViews();

        return view;
    }

    private void updateImportButtonEnabledState() {
        if (checkBoxShows.isChecked()
                || checkBoxLists.isChecked()
                || checkBoxMovies.isChecked()) {
            buttonImport.setEnabled(true);
        } else {
            buttonImport.setEnabled(false);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // restore UI state
        if (dataLibTask != null && dataLibTask.getStatus() != AsyncTask.Status.FINISHED) {
            setProgressLock(true);
        }
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

        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        if (dataLibTask != null && dataLibTask.getStatus() != AsyncTask.Status.FINISHED) {
            dataLibTask.cancel(true);
        }
        dataLibTask = null;

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
        if (isLocked) {
            buttonImport.setEnabled(false);
        } else {
            updateImportButtonEnabledState();
        }
        progressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        checkBoxFullDump.setEnabled(!isLocked);
        buttonShowsExportFile.setEnabled(!isLocked);
        buttonShowsImportFile.setEnabled(!isLocked);
        buttonListsExportFile.setEnabled(!isLocked);
        buttonListsImportFile.setEnabled(!isLocked);
        buttonMoviesExportFile.setEnabled(!isLocked);
        buttonMoviesImportFile.setEnabled(!isLocked);
        checkBoxShows.setEnabled(!isLocked);
        checkBoxLists.setEnabled(!isLocked);
        checkBoxMovies.setEnabled(!isLocked);
    }

    private void doDataLiberationAction(int requestCode) {
        if (requestCode == REQUEST_CODE_EXPORT) {
            setProgressLock(true);

            dataLibTask = new JsonExportTask(getContext(), DataLiberationFragment.this,
                    checkBoxFullDump.isChecked(), false, type);
            dataLibTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (requestCode == REQUEST_CODE_IMPORT) {
            setProgressLock(true);

            dataLibTask = new JsonImportTask(getContext(),
                    checkBoxShows.isChecked(), checkBoxLists.isChecked(),
                    checkBoxMovies.isChecked());
            Utils.executeInOrder(dataLibTask);
        }
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
                getContext().getContentResolver()
                        .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (SecurityException e) {
                Timber.e(e, "Could not persist r/w permission for backup file URI.");
            }

            if (requestCode == REQUEST_CODE_SHOWS_EXPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_SHOWS_EXPORT_URI, uri);
                type = JsonExportTask.BACKUP_SHOWS;
                doDataLiberationAction(REQUEST_CODE_EXPORT);
            } else if (requestCode == REQUEST_CODE_LISTS_EXPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_LISTS_EXPORT_URI, uri);
                type = JsonExportTask.BACKUP_LISTS;
                doDataLiberationAction(REQUEST_CODE_EXPORT);
            } else if (requestCode == REQUEST_CODE_MOVIES_EXPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_MOVIES_EXPORT_URI,
                        uri);
                type = JsonExportTask.BACKUP_MOVIES;
                doDataLiberationAction(REQUEST_CODE_EXPORT);
            } else if (requestCode == REQUEST_CODE_SHOWS_IMPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_SHOWS_IMPORT_URI, uri);
            } else if (requestCode == REQUEST_CODE_LISTS_IMPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_LISTS_IMPORT_URI, uri);
            } else {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_MOVIES_IMPORT_URI,
                        uri);
            }
            updateFileViews();
        }
    }

    private void updateFileViews() {
        setUriOrPlaceholder(textShowsExportFile, BackupSettings.getFileUri(getContext(),
                BackupSettings.KEY_SHOWS_EXPORT_URI));
        setUriOrPlaceholder(textShowsImportFile, BackupSettings.getFileUri(getContext(),
                BackupSettings.KEY_SHOWS_IMPORT_URI));
        setUriOrPlaceholder(textListsExportFile, BackupSettings.getFileUri(getContext(),
                BackupSettings.KEY_LISTS_EXPORT_URI));
        setUriOrPlaceholder(textListsImportFile, BackupSettings.getFileUri(getContext(),
                BackupSettings.KEY_LISTS_IMPORT_URI));
        setUriOrPlaceholder(textMoviesExportFile, BackupSettings.getFileUri(getContext(),
                BackupSettings.KEY_MOVIES_EXPORT_URI));
        setUriOrPlaceholder(textMoviesImportFile, BackupSettings.getFileUri(getContext(),
                BackupSettings.KEY_MOVIES_IMPORT_URI));
    }

    private void setUriOrPlaceholder(TextView textView, Uri uri) {
        textView.setText(uri == null ? getString(R.string.no_file_selected) : uri.toString());
    }
}
