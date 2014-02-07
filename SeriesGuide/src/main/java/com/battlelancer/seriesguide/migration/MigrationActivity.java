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

package com.battlelancer.seriesguide.migration;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask;
import com.battlelancer.seriesguide.dataliberation.JsonImportTask;
import com.battlelancer.seriesguide.dataliberation.OnTaskFinishedListener;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

/**
 * Helps users migrate their show database to the free version of SeriesGuide. When using
 * SeriesGuide X a backup assistant and install+launch the free version assistant is shown. When
 * using any other version an import assistant is shown.
 */
public class MigrationActivity extends BaseActivity
        implements JsonExportTask.OnTaskProgressListener, OnTaskFinishedListener {

    private static final String PACKAGE_SERIESGUIDE = "com.battlelancer.seriesguide";

    private ProgressBar mProgressBar;

    private Button mButtonBackup;

    private AsyncTask<Void, Integer, Integer> mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_migration);

        setupActionBar();
        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupViews() {
        mProgressBar = (ProgressBar) findViewById(R.id.progressBarMigration);

        mButtonBackup = (Button) findViewById(R.id.buttonMigrationExport);
        mButtonBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // import shows
                mTask = new JsonImportTask(MigrationActivity.this, MigrationActivity.this,
                        false);
                mTask.execute();

                mProgressBar.setVisibility(View.VISIBLE);

                preventUserInput(true);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void preventUserInput(boolean isLockdown) {
        // toggle buttons enabled state
        mButtonBackup.setEnabled(!isLockdown);
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
        mProgressBar.setVisibility(View.GONE);
        preventUserInput(false);
    }

}
