
package com.battlelancer.seriesguide.dataliberation;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.OnExportTaskFinishedListener;
import com.uwetrottmann.seriesguide.R;

/**
 * One button export or import of the show database using a JSON file on
 * external storage.
 */
public class DataLiberationFragment extends SherlockFragment implements
        OnExportTaskFinishedListener {

    private Button mButtonExport;
    private ProgressBar mProgressBar;
    private JsonExportTask mTaskExport;

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

        mButtonExport = (Button) v.findViewById(R.id.buttonExport);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getActivity().getApplicationContext();
        mButtonExport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mButtonExport.setEnabled(false);
                mProgressBar.setVisibility(View.VISIBLE);

                mTaskExport = new JsonExportTask(context, DataLiberationFragment.this);
                mTaskExport.execute();
            }
        });
    }

    @Override
    public void onDestroy() {
        if (mTaskExport != null && mTaskExport.getStatus() != AsyncTask.Status.FINISHED) {
            mTaskExport.cancel(true);
        }
        mTaskExport = null;

        super.onDestroy();
    }

    @Override
    public void onExportTaskFinished() {
        mButtonExport.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

}
