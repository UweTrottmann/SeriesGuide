
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.TraktSync;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class TraktSyncActivity extends BaseActivity {

    private TraktSync mSyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trakt_sync);

        final View container = findViewById(R.id.syncbuttons);

        final CheckBox syncUnseenEpisodes = (CheckBox) findViewById(R.id.checkBoxSyncUnseen);
        final Button syncToDeviceButton = (Button) findViewById(R.id.syncToDeviceButton);
        syncToDeviceButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (mSyncTask == null
                        || (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.FINISHED)) {
                    mSyncTask = (TraktSync) new TraktSync(TraktSyncActivity.this, container, false,
                            syncUnseenEpisodes.isChecked()).execute();
                }
            }
        });

        final Button syncToTraktButton = (Button) findViewById(R.id.syncToTraktButton);
        syncToTraktButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mSyncTask == null
                        || (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.FINISHED)) {
                    mSyncTask = (TraktSync) new TraktSync(TraktSyncActivity.this, container, true,
                            syncUnseenEpisodes.isChecked()).execute();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSyncTask != null && mSyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mSyncTask.cancel(true);
            mSyncTask = null;
        }
    }
}
