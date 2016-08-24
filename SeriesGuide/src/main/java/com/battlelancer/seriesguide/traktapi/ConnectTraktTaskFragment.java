package com.battlelancer.seriesguide.traktapi;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.battlelancer.seriesguide.util.ConnectTraktTask;

/**
 * Keeps the reference to a {@link com.battlelancer.seriesguide.util.ConnectTraktTask}.
 */
public class ConnectTraktTaskFragment extends Fragment {

    // data object we want to retain
    private ConnectTraktTask task;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void setTask(ConnectTraktTask task) {
        this.task = task;
    }

    public ConnectTraktTask getTask() {
        return task;
    }
}
