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

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.TraktUpload;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Provides a tool to upload shows to trakt (e.g. after first connecting).
 */
public class TraktSyncActivity extends BaseActivity {

    private static final String TAG = "Trakt Upload";

    private TraktUpload mUploadTask;

    @InjectView(R.id.checkBoxSyncUnseen) CheckBox mUploadUnwatchedEpisodes;

    @InjectView(R.id.buttonSyncToTrakt) Button mUploadButton;

    @InjectView(R.id.progressBarSyncToTrakt) ProgressBar mUploadProgressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trakt_sync);

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupViews() {
        ButterKnife.inject(this);

        // Sync to trakt button
        mUploadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadShowsToTrakt();
            }
        });
        mUploadProgressIndicator.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // restore settings
        mUploadUnwatchedEpisodes.setChecked(TraktSettings.isSyncingUnwatchedEpisodes(this));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // save settings
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(TraktSettings.KEY_SYNC_UNWATCHED_EPISODES,
                        mUploadUnwatchedEpisodes.isChecked())
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // cleanup running task
        if (mUploadTask != null && mUploadTask.getStatus() == AsyncTask.Status.RUNNING) {
            mUploadTask.cancel(true);
            mUploadTask = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return false;
    }

    private void uploadShowsToTrakt() {
        // abort if task is still running
        if (mUploadTask != null && mUploadTask.getStatus() != AsyncTask.Status.FINISHED) {
            return;
        }

        mUploadTask = (TraktUpload) new TraktUpload(this, mUploadButton, mUploadProgressIndicator,
                mUploadUnwatchedEpisodes.isChecked())
                .execute();
        Utils.trackAction(TraktSyncActivity.this, TAG, "Upload to trakt");
    }
}
