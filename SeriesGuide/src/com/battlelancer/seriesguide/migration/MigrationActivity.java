package com.battlelancer.seriesguide.migration;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ProgressBar;

import com.battlelancer.seriesguide.dataliberation.JsonExportTask;
import com.battlelancer.seriesguide.dataliberation.OnTaskFinishedListener;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.uwetrottmann.seriesguide.R;

/**
 * Helps users migrate their show database to the free version of SeriesGuide.
 */
public class MigrationActivity extends BaseActivity implements JsonExportTask.OnTaskProgressListener, OnTaskFinishedListener {

    private static final String KEY_MIGRATION_OPT_OUT = "com.battlelancer.seriesguide.migration.optout";

    public static boolean hasOptedOutOfMigration(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_MIGRATION_OPT_OUT,
                false);
    }

    private JsonExportTask mTask;
    private View mButtonBackup;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_migration);

        setupViews();
    }

    private void setupViews() {
        mProgressBar = (ProgressBar) findViewById(R.id.progressBarMigration);

        mButtonBackup = findViewById(R.id.buttonMigrationExport);
        mButtonBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // backup shows
                mTask = new JsonExportTask(MigrationActivity.this, MigrationActivity.this,
                        MigrationActivity.this,
                        true, false);
                mTask.execute();

                mProgressBar.setVisibility(View.VISIBLE);

                preventUserInput(true);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // TODO check if backup already exists

        // TODO check if SeriesGuide is already installed
    }

    @Override
    protected void onDestroy() {
        // clean up backup task
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }
        mTask = null;

        super.onDestroy();
    }

    private void preventUserInput(boolean isLockdown) {
        // TODO toggle buttons enable state
        mButtonBackup.setEnabled(!isLockdown);
    }

    @Override
    public void onProgressUpdate(Integer... values) {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setIndeterminate(values[0] == values[1]);
        mProgressBar.setMax(values[0]);
        mProgressBar.setProgress(values[1]);
    }

    @Override
    public void onTaskFinished() {
        mProgressBar.setVisibility(View.GONE);
        preventUserInput(false);
    }
}
