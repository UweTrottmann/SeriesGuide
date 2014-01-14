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

import com.google.analytics.tracking.android.EasyTracker;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.TraktSync;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * Displays information and offers tools to upload or download watched flags
 * from trakt.
 */
public class TraktSyncActivity extends BaseActivity {

    private static final int DIALOG_SELECT_SHOWS = 100;

    private static final String TAG = "Trakt Sync";

    private TraktSync mSyncTask;

    private CheckBox mSyncUnwatchedEpisodes;

    private View mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trakt_sync);

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupViews() {
        mContainer = findViewById(R.id.syncbuttons);
    
        mSyncUnwatchedEpisodes = (CheckBox) findViewById(R.id.checkBoxSyncUnseen);
    
        // Sync to SeriesGuide button
        final Button syncToDeviceButton = (Button) findViewById(R.id.syncToDeviceButton);
        syncToDeviceButton.setOnClickListener(new OnClickListener() {
    
            public void onClick(View v) {
                fireTrackerEvent("Download to SeriesGuide");
                if (mSyncTask == null
                        || (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.FINISHED)) {
                    mSyncTask = (TraktSync) new TraktSync(TraktSyncActivity.this, mContainer,
                            false, mSyncUnwatchedEpisodes.isChecked()).execute();
                }
            }
        });
    
        // Sync to trakt button
        final Button syncToTraktButton = (Button) findViewById(R.id.syncToTraktButton);
        syncToTraktButton.setOnClickListener(new OnClickListener() {
    
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_SELECT_SHOWS);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isSyncUnseenEpisodes = TraktSettings.isSyncingUnwatchedEpisodes(this);
        mSyncUnwatchedEpisodes.setChecked(isSyncUnseenEpisodes);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(TraktSettings.KEY_SYNC_UNWATCHED_EPISODES,
                mSyncUnwatchedEpisodes.isChecked()).commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mSyncTask.cancel(true);
            mSyncTask = null;
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

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_SELECT_SHOWS:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.trakt_upload);
                final Cursor shows = getContentResolver().query(Shows.CONTENT_URI, new String[] {
                        Shows._ID, Shows.TITLE, Shows.SYNCENABLED
                }, null, null, Shows.TITLE + " ASC");

                String[] showTitles = new String[shows.getCount()];
                boolean[] syncEnabled = new boolean[shows.getCount()];
                for (int i = 0; i < showTitles.length; i++) {
                    shows.moveToNext();
                    showTitles[i] = shows.getString(1);
                    syncEnabled[i] = shows.getInt(2) == 1;
                }

                builder.setMultiChoiceItems(showTitles, syncEnabled,
                        new OnMultiChoiceClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                shows.moveToFirst();
                                shows.move(which);
                                final String showId = shows.getString(0);
                                final ContentValues values = new ContentValues();
                                values.put(Shows.SYNCENABLED, isChecked);
                                getContentResolver().update(Shows.buildShowUri(showId), values,
                                        null, null);
                            }
                        });
                builder.setPositiveButton(R.string.trakt_upload,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Utils.trackAction(TraktSyncActivity.this, TAG, "Upload to trakt");
                                fireTrackerEvent("Upload to trakt");
                                if (mSyncTask == null
                                        || (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.FINISHED)) {
                                    mSyncTask = (TraktSync) new TraktSync(TraktSyncActivity.this,
                                            mContainer, true, mSyncUnwatchedEpisodes.isChecked())
                                            .execute();
                                }
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);

                return builder.create();
        }
        return null;
    }

    private void fireTrackerEvent(String label) {
        Utils.trackClick(this, TAG, label);
    }
}
