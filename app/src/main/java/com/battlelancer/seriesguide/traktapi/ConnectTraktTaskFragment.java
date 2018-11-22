package com.battlelancer.seriesguide.traktapi;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

/**
 * Keeps the reference to a {@link ConnectTraktTask}.
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

    void setTask(ConnectTraktTask task) {
        this.task = task;
    }

    ConnectTraktTask getTask() {
        return task;
    }
}
