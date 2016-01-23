/*
 * Copyright 2016 Uwe Trottmann
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
