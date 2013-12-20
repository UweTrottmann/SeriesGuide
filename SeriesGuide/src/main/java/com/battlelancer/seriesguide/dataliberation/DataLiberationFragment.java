/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.dataliberation;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
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

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.OnTaskProgressListener;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.uwetrottmann.seriesguide.R;

/**
 * One button export or import of the show database using a JSON file on
 * external storage.
 */
public class DataLiberationFragment extends SherlockFragment implements OnTaskFinishedListener,
        OnTaskProgressListener {

    private Button mButtonExport;
    private Button mButtonImport;
    private Button mButtonImportAutoBackup;
    private ProgressBar mProgressBar;
    private CheckBox mCheckBoxFullDump;
    private CheckBox mCheckBoxImportWarning;
    private AsyncTask<Void, Integer, Integer> mTask;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.data_liberation_fragment, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBarDataLiberation);
        mButtonExport = (Button) v.findViewById(R.id.buttonExport);
        mButtonImport = (Button) v.findViewById(R.id.buttonImport);
        mButtonImportAutoBackup = (Button) v.findViewById(R.id.buttonBackupRestoreAutoBackup);
        mCheckBoxFullDump = (CheckBox) v.findViewById(R.id.checkBoxFullDump);
        mCheckBoxImportWarning = (CheckBox) v.findViewById(R.id.checkBoxImportWarning);

        // display backup path
        TextView backuppath = (TextView) v.findViewById(R.id.textViewBackupPath);
        String path = JsonExportTask.getExportPath(false).toString();
        backuppath.setText(getString(R.string.backup_path) + ": " + path);

        // display current db version
        TextView dbVersion = (TextView) v.findViewById(R.id.textViewBackupDatabaseVersion);
        dbVersion.setText(getString(R.string.backup_version) + ": "
                + SeriesGuideDatabase.DATABASE_VERSION);

        // display last auto-backup date
        TextView lastAutoBackup = (TextView) v.findViewById(R.id.textViewBackupLastAutoBackup);
        long lastAutoBackupTime = AdvancedSettings.getLastAutoBackupTime(getActivity());
        lastAutoBackup
                .setText(getString(R.string.last_auto_backup,
                        DateUtils.getRelativeDateTimeString(getActivity(),
                                lastAutoBackupTime, DateUtils.SECOND_IN_MILLIS,
                                DateUtils.DAY_IN_MILLIS, 0)));

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getActivity().getApplicationContext();
        mButtonExport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgressLock(true);

                mTask = new JsonExportTask(context, DataLiberationFragment.this,
                        DataLiberationFragment.this,
                        mCheckBoxFullDump.isChecked(), false);
                mTask.execute();
            }
        });
        mCheckBoxImportWarning.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mButtonImport.setEnabled(isChecked);
                mButtonImportAutoBackup.setEnabled(isChecked);
            }
        });
        mButtonImport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgressLock(true);

                mTask = new JsonImportTask(context, DataLiberationFragment.this, false);
                mTask.execute();
            }
        });
        mButtonImportAutoBackup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgressLock(true);

                mTask = new JsonImportTask(context, DataLiberationFragment.this, true);
                mTask.execute();
            }
        });

        // restore UI state
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            setProgressLock(true);
        }
    }

    @Override
    public void onDestroy() {
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }
        mTask = null;

        super.onDestroy();
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
        setProgressLock(false);
    }

    private void setProgressLock(boolean isLocked) {
        if (isLocked) {
            mButtonImport.setEnabled(false);
            mButtonImportAutoBackup.setEnabled(false);
        } else {
            mButtonImport.setEnabled(mCheckBoxImportWarning.isChecked());
            mButtonImportAutoBackup.setEnabled(mCheckBoxImportWarning.isChecked());
        }
        mButtonExport.setEnabled(!isLocked);
        mProgressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        mCheckBoxFullDump.setEnabled(!isLocked);
        mCheckBoxImportWarning.setEnabled(!isLocked);
    }

}
