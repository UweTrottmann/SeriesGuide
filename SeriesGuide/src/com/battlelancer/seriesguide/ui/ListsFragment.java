/*
 * Copyright 2012 Uwe Trottmann
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

package com.battlelancer.seriesguide.ui;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListFragment;
import com.battlelancer.seriesguide.beta.R;

/**
 * Displays one user created list which includes a mixture of shows, seasons and
 * episodes.
 */
public class ListsFragment extends SherlockListFragment {

    public static ListsFragment newInstance(String list_id) {
        ListsFragment f = new ListsFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.LIST_ID, list_id);
        f.setArguments(args);

        return f;
    }

    interface InitBundle {
        String LIST_ID = "list_id";
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String list_id = getArguments().getString(InitBundle.LIST_ID);

        setEmptyText(getString(R.string.list_empty));
        
        setListShown(true);
    }

}
