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

package com.battlelancer.seriesguide.extensions;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ExtensionsAdapter;
import com.battlelancer.seriesguide.loaders.AvailableActionsLoader;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.List;
import timber.log.Timber;

/**
 * Provides tools to display all installed extensions and enable or disable them.
 */
public class ExtensionsConfigurationFragment extends Fragment {

    @InjectView(R.id.listViewExtensionsConfiguration) ListView mListView;

    private ExtensionsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_extensions_configuration, null);
        ButterKnife.inject(this, v);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new ExtensionsAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        getLoaderManager().initLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID, null,
                mActionsLoaderCallbacks);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.extensions_configuration_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_extensions_enable) {
            List<ExtensionManager.Extension> extensions = ExtensionManager.getInstance(
                    getActivity()).queryAllAvailableExtensions();
            for (ExtensionManager.Extension extension : extensions) {
                ExtensionManager.getInstance(getActivity())
                        .enableExtension(extension.componentName);
            }
            Toast.makeText(getActivity(), "Enabled all available extensions", Toast.LENGTH_LONG)
                    .show();
            return true;
        }
        if (itemId == R.id.menu_action_extensions_disable) {
            List<ExtensionManager.Extension> extensions = ExtensionManager.getInstance(
                    getActivity()).queryAllAvailableExtensions();
            for (ExtensionManager.Extension extension : extensions) {
                ExtensionManager.getInstance(getActivity())
                        .disableExtension(extension.componentName);
            }
            Toast.makeText(getActivity(), "Disabled all available extensions", Toast.LENGTH_LONG)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    private LoaderManager.LoaderCallbacks<List<ExtensionManager.Extension>> mActionsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<ExtensionManager.Extension>>() {
        @Override
        public Loader<List<ExtensionManager.Extension>> onCreateLoader(int id, Bundle args) {
            return new AvailableActionsLoader(getActivity());
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onLoadFinished(Loader<List<ExtensionManager.Extension>> loader,
                List<ExtensionManager.Extension> data) {
            if (data == null || data.size() == 0) {
                Timber.d("Did not find any extension");
            } else {
                Timber.d("Found " + data.size() + " extensions");
            }

            mAdapter.clear();
            if (AndroidUtils.isHoneycombOrHigher()) {
                mAdapter.addAll(data);
            } else {
                for (ExtensionManager.Extension extension : data) {
                    mAdapter.add(extension);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<List<ExtensionManager.Extension>> loader) {
            mAdapter.clear();
        }
    };
}
