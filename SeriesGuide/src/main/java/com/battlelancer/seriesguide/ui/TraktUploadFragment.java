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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.interfaces.OnTaskFinishedListener;
import com.battlelancer.seriesguide.interfaces.OnTaskProgressListener;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.TraktUpload;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Provides tool to upload shows in the local database to trakt (e.g. after first connecting to
 * trakt).
 */
public class TraktUploadFragment extends SherlockFragment implements OnTaskFinishedListener,
        OnTaskProgressListener {

    private static final String TAG = "Trakt Upload";

    @InjectView(R.id.checkBoxTraktUploadUnwatched) CheckBox mUploadUnwatchedEpisodes;

    @InjectView(R.id.buttonTraktUpload) Button mUploadButton;

    @InjectView(R.id.progressBarTraktUpload) ProgressBar mProgressIndicator;

    private TraktUpload mUploadTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Try to keep the fragment around on config changes so the upload task
         * does not have to be finished.
         */
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_trakt_upload, container, false);
        ButterKnife.inject(this, v);

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadShowsToTrakt();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // lock UI if task is still running
        if (mUploadTask != null && mUploadTask.getStatus() != AsyncTask.Status.FINISHED) {
            setProgressLock(true);
        } else {
            setProgressLock(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // restore settings
        mUploadUnwatchedEpisodes.setChecked(
                TraktSettings.isSyncingUnwatchedEpisodes(getActivity()));
    }

    @Override
    public void onPause() {
        super.onPause();

        // save settings
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putBoolean(TraktSettings.KEY_SYNC_UNWATCHED_EPISODES,
                        mUploadUnwatchedEpisodes.isChecked())
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    @Override
    public void onDestroy() {
        // clean up running task
        if (mUploadTask != null && mUploadTask.getStatus() != AsyncTask.Status.FINISHED) {
            mUploadTask.cancel(true);
        }
        mUploadTask = null;

        super.onDestroy();
    }

    private void uploadShowsToTrakt() {
        setProgressLock(true);

        mUploadTask = new TraktUpload(getActivity(), this, this,
                mUploadUnwatchedEpisodes.isChecked());
        mUploadTask.execute();
        Utils.trackAction(getActivity(), TAG, "Upload to trakt");
    }

    @Override
    public void onTaskFinished() {
        setProgressLock(false);
    }

    private void setProgressLock(boolean isEnabled) {
        mUploadButton.setEnabled(!isEnabled);
        mUploadUnwatchedEpisodes.setEnabled(!isEnabled);
        mProgressIndicator.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onProgressUpdate(Integer... values) {
        if (mProgressIndicator == null) {
            return;
        }
        mProgressIndicator.setMax(values[0]);
        mProgressIndicator.setProgress(values[1]);
    }
}
