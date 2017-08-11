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
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.AsyncTaskCompat;
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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.BackupSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * One button export or import of the show database using a JSON file on external storage.
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
                        android.support.design.R.id.snackbar_text);
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

    @BindView(R.id.buttonDataLibExport) Button buttonExport;
    @BindView(R.id.buttonDataLibImport) Button buttonImport;
    @BindView(R.id.progressBarDataLib) ProgressBar progressBar;
    @BindView(R.id.checkBoxDataLibFullDump) CheckBox checkBoxFullDump;
    @BindView(R.id.checkBoxDataLibImportWarning) CheckBox checkBoxImportWarning;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_data_liberation, container, false);
        unbinder = ButterKnife.bind(this, v);

        progressBar.setVisibility(View.GONE);

        // setup listeners
        buttonExport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tryDataLiberationAction(REQUEST_CODE_EXPORT);
            }
        });
        checkBoxImportWarning.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateImportButtonEnabledState();
            }
        });
        checkBoxShows.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateImportButtonEnabledState();
            }
        });
        checkBoxLists.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateImportButtonEnabledState();
            }
        });
        checkBoxMovies.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateImportButtonEnabledState();
            }
        });
        buttonImport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tryDataLiberationAction(REQUEST_CODE_IMPORT);
            }
        });

        // selecting custom backup files is only supported on KitKat and up
        // as we use Storage Access Framework in this case
        if (AndroidUtils.isKitKatOrHigher()) {
            buttonShowsExportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                            JsonExportTask.EXPORT_JSON_FILE_SHOWS,
                            REQUEST_CODE_SHOWS_EXPORT_URI);
                }
            });
            buttonShowsImportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                            REQUEST_CODE_SHOWS_IMPORT_URI);
                }
            });
            buttonListsExportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                            JsonExportTask.EXPORT_JSON_FILE_LISTS,
                            REQUEST_CODE_LISTS_EXPORT_URI);
                }
            });
            buttonListsImportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                            REQUEST_CODE_LISTS_IMPORT_URI);
                }
            });
            buttonMoviesExportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DataLiberationTools.selectExportFile(DataLiberationFragment.this,
                            JsonExportTask.EXPORT_JSON_FILE_MOVIES,
                            REQUEST_CODE_MOVIES_EXPORT_URI);
                }
            });
            buttonMoviesImportFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DataLiberationTools.selectImportFile(DataLiberationFragment.this,
                            REQUEST_CODE_MOVIES_IMPORT_URI);
                }
            });
        } else {
            buttonShowsExportFile.setVisibility(View.GONE);
            buttonShowsImportFile.setVisibility(View.GONE);
            buttonListsExportFile.setVisibility(View.GONE);
            buttonListsImportFile.setVisibility(View.GONE);
            buttonMoviesExportFile.setVisibility(View.GONE);
            buttonMoviesImportFile.setVisibility(View.GONE);
        }
        updateFileViews();

        return v;
    }

    private void updateImportButtonEnabledState() {
        if (checkBoxShows.isChecked()
                || checkBoxLists.isChecked()
                || checkBoxMovies.isChecked()) {
            buttonImport.setEnabled(checkBoxImportWarning.isChecked());
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
        if (AndroidUtils.isKitKatOrHigher()) {
            updateFileViews();
        }
        setProgressLock(false);
    }

    private void setProgressLock(boolean isLocked) {
        if (isLocked) {
            buttonImport.setEnabled(false);
        } else {
            updateImportButtonEnabledState();
        }
        buttonExport.setEnabled(!isLocked);
        progressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        checkBoxFullDump.setEnabled(!isLocked);
        checkBoxImportWarning.setEnabled(!isLocked);
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
                || requestCode == REQUEST_CODE_IMPORT) {
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

            dataLibTask = new JsonExportTask(getContext(), DataLiberationFragment.this,
                    checkBoxFullDump.isChecked(), false);
            AsyncTaskCompat.executeParallel(dataLibTask);
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

        if (requestCode == REQUEST_CODE_SHOWS_EXPORT_URI
                || requestCode == REQUEST_CODE_SHOWS_IMPORT_URI
                || requestCode == REQUEST_CODE_LISTS_EXPORT_URI
                || requestCode == REQUEST_CODE_LISTS_IMPORT_URI
                || requestCode == REQUEST_CODE_MOVIES_EXPORT_URI
                || requestCode == REQUEST_CODE_MOVIES_IMPORT_URI) {
            Uri uri = data.getData();

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
            } else if (requestCode == REQUEST_CODE_SHOWS_IMPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_SHOWS_IMPORT_URI, uri);
            } else if (requestCode == REQUEST_CODE_LISTS_EXPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_LISTS_EXPORT_URI, uri);
            } else if (requestCode == REQUEST_CODE_LISTS_IMPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_LISTS_IMPORT_URI, uri);
            } else if (requestCode == REQUEST_CODE_MOVIES_EXPORT_URI) {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_MOVIES_EXPORT_URI,
                        uri);
            } else {
                BackupSettings.storeFileUri(getContext(), BackupSettings.KEY_MOVIES_IMPORT_URI,
                        uri);
            }
            updateFileViews();
        }
    }

    private void updateFileViews() {
        if (AndroidUtils.isKitKatOrHigher()) {
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
        } else {
            String path = JsonExportTask.getExportPath(false).toString();
            String showsFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_SHOWS;
            textShowsExportFile.setText(showsFilePath);
            textShowsImportFile.setText(showsFilePath);
            String listsFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_LISTS;
            textListsExportFile.setText(listsFilePath);
            textListsImportFile.setText(listsFilePath);
            String moviesFilePath = path + "/" + JsonExportTask.EXPORT_JSON_FILE_MOVIES;
            textMoviesExportFile.setText(moviesFilePath);
            textMoviesImportFile.setText(moviesFilePath);
        }
    }

    private void setUriOrPlaceholder(TextView textView, Uri uri) {
        textView.setText(uri == null ? getString(R.string.no_file_selected) : uri.toString());
    }
}
