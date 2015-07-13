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

import android.content.ComponentName;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.PopupMenu;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ExtensionsAdapter;
import com.battlelancer.seriesguide.loaders.AvailableExtensionsLoader;
import com.battlelancer.seriesguide.util.Utils;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import timber.log.Timber;

/**
 * Provides tools to display all installed extensions and enable or disable them.
 */
public class ExtensionsConfigurationFragment extends Fragment
        implements AdapterView.OnItemClickListener {

    public static final int EXTENSION_LIMIT_FREE = 2;

    private static final String TAG = "Extension Configuration";

    @Bind(R.id.listViewExtensionsConfiguration) DragSortListView mListView;

    private ExtensionsAdapter mAdapter;
    private PopupMenu mAddExtensionPopupMenu;

    private List<ExtensionManager.Extension> mAvailableExtensions = new ArrayList<>();
    private List<ComponentName> mEnabledExtensions;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_extensions_configuration, container, false);
        ButterKnife.bind(this, v);

        final ExtensionsDragSortController dragSortController = new ExtensionsDragSortController();
        mListView.setFloatViewManager(dragSortController);
        mListView.setOnTouchListener(dragSortController);
        mListView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                ComponentName extension = mEnabledExtensions.remove(from);
                mEnabledExtensions.add(to, extension);
                getLoaderManager().restartLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID,
                        null, mExtensionsLoaderCallbacks);
            }
        });
        mListView.setOnItemClickListener(this);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new ExtensionsAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        getLoaderManager().initLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID, null,
                mExtensionsLoaderCallbacks);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
        if (mEnabledExtensions != null) { // might not have finished loading, yet
            ExtensionManager.getInstance(getActivity()).setEnabledExtensions(mEnabledExtensions);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.extensions_configuration_menu, menu);
        if (Utils.isAmazonVersion()) {
            // no third-party extensions supported on Amazon app store for now
            menu.findItem(R.id.menu_action_extensions_search).setVisible(false);
            menu.findItem(R.id.menu_action_extensions_search).setEnabled(false);
        }
        if (!BuildConfig.DEBUG) {
            menu.findItem(R.id.menu_action_extensions_enable).setVisible(false);
            menu.findItem(R.id.menu_action_extensions_disable).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_extensions_search) {
            Utils.launchWebsite(getActivity(), getString(R.string.url_extensions_search), TAG,
                    "Get more extensions");
            return true;
        }
        if (itemId == R.id.menu_action_extensions_enable) {
            List<ExtensionManager.Extension> extensions = ExtensionManager.getInstance(
                    getActivity()).queryAllAvailableExtensions();
            List<ComponentName> enabledExtensions = new ArrayList<>();
            for (ExtensionManager.Extension extension : extensions) {
                enabledExtensions.add(extension.componentName);
            }
            ExtensionManager.getInstance(getActivity()).setEnabledExtensions(enabledExtensions);
            Toast.makeText(getActivity(), "Enabled all available extensions", Toast.LENGTH_LONG)
                    .show();
            return true;
        }
        if (itemId == R.id.menu_action_extensions_disable) {
            ExtensionManager.getInstance(getActivity())
                    .setEnabledExtensions(new ArrayList<ComponentName>());
            Toast.makeText(getActivity(), "Disabled all available extensions", Toast.LENGTH_LONG)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.unbind(this);
    }

    private LoaderManager.LoaderCallbacks<List<ExtensionManager.Extension>>
            mExtensionsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<ExtensionManager.Extension>>() {
        @Override
        public Loader<List<ExtensionManager.Extension>> onCreateLoader(int id, Bundle args) {
            return new AvailableExtensionsLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<ExtensionManager.Extension>> loader,
                List<ExtensionManager.Extension> availableExtensions) {
            if (availableExtensions == null || availableExtensions.size() == 0) {
                Timber.d("Did not find any extension");
            } else {
                Timber.d("Found " + availableExtensions.size() + " extensions");
            }

            if (mEnabledExtensions == null) {
                mEnabledExtensions = ExtensionManager.getInstance(getActivity())
                        .getEnabledExtensions();
            }
            Set<ComponentName> enabledExtensions = new HashSet<>(mEnabledExtensions);

            // find all extensions not yet enabled
            mAvailableExtensions.clear();
            Map<ComponentName, ExtensionManager.Extension> enabledExtensionsMap = new HashMap<>();
            for (ExtensionManager.Extension extension : availableExtensions) {
                if (enabledExtensions.contains(extension.componentName)) {
                    // extension is already enabled
                    enabledExtensionsMap.put(extension.componentName, extension);
                    continue;
                }

                mAvailableExtensions.add(extension);
            }

            // sort available extensions alphabetically
            Collections.sort(mAvailableExtensions, new Comparator<ExtensionManager.Extension>() {
                @Override
                public int compare(ExtensionManager.Extension extension1,
                        ExtensionManager.Extension extension2) {
                    String title1 = createTitle(extension1);
                    String title2 = createTitle(extension2);
                    return title1.compareToIgnoreCase(title2);
                }

                private String createTitle(ExtensionManager.Extension extension) {
                    String title = extension.label;
                    if (TextUtils.isEmpty(title)) {
                        title = extension.componentName.flattenToShortString();
                    }
                    return title;
                }
            });

            // force re-creation of extension add menu
            if (mAddExtensionPopupMenu != null) {
                mAddExtensionPopupMenu.dismiss();
                mAddExtensionPopupMenu = null;
            }

            // list enabled extensions in order dictated by extension manager
            List<ExtensionManager.Extension> enabledExtensionsList = new ArrayList<>();
            List<ComponentName> enabledExtensionNames = new ArrayList<>(mEnabledExtensions);
            for (ComponentName extensionName : enabledExtensionNames) {
                ExtensionManager.Extension extension = enabledExtensionsMap.get(extensionName);
                if (extension == null) {
                    // filter out any unavailable/uninstalled extensions
                    mEnabledExtensions.remove(extensionName);
                    continue;
                }
                enabledExtensionsList.add(extension);
            }

            // refresh enabled extensions list
            mAdapter.clear();
            mAdapter.addAll(enabledExtensionsList);
        }

        @Override
        public void onLoaderReset(Loader<List<ExtensionManager.Extension>> loader) {
            mAdapter.clear();
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == mAdapter.getCount() - 1) {
            // non-supporters only can add a few extensions
            if (mAdapter.getCount() - 1 == EXTENSION_LIMIT_FREE
                    && !Utils.hasAccessToX(getActivity())) {
                Utils.advertiseSubscription(getActivity());
                return;
            }
            showAddExtensionPopupMenu(view.findViewById(R.id.textViewItemExtensionAddLabel));
            Utils.trackAction(getActivity(), TAG, "Add extension");
        }
    }

    public void onEventMainThread(ExtensionsAdapter.ExtensionDisableRequestEvent event) {
        mEnabledExtensions.remove(event.position);
        getLoaderManager().restartLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID, null,
                mExtensionsLoaderCallbacks);
        Utils.trackAction(getActivity(), TAG, "Remove extension");
    }

    private void showAddExtensionPopupMenu(View anchorView) {
        if (mAddExtensionPopupMenu != null) {
            mAddExtensionPopupMenu.dismiss();
        }

        mAddExtensionPopupMenu = new PopupMenu(getActivity(), anchorView);
        for (int i = 0; i < mAvailableExtensions.size(); i++) {
            ExtensionManager.Extension extension = mAvailableExtensions.get(i);
            mAddExtensionPopupMenu.getMenu().add(Menu.NONE, i, Menu.NONE, extension.label);
        }
        mAddExtensionPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                // add to enabled extensions
                ExtensionManager.Extension extension = mAvailableExtensions.get(item.getItemId());
                mEnabledExtensions.add(extension.componentName);
                // re-populate extension list
                getLoaderManager().restartLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID,
                        null, mExtensionsLoaderCallbacks);
                // scroll to end of list
                mListView.smoothScrollToPosition(mAdapter.getCount() - 1);
                return true;
            }
        });

        mAddExtensionPopupMenu.show();
    }

    private class ExtensionsDragSortController extends DragSortController {

        private int mFloatViewOriginPosition;

        public ExtensionsDragSortController() {
            super(mListView, R.id.drag_handle, DragSortController.ON_DOWN,
                    DragSortController.CLICK_REMOVE);
            setRemoveEnabled(false);
        }

        @Override
        public int startDragPosition(MotionEvent ev) {
            int hitPosition = super.dragHandleHitPosition(ev);
            if (hitPosition >= mAdapter.getCount() - 1) {
                return DragSortController.MISS;
            }

            return hitPosition;
        }

        @Override
        public View onCreateFloatView(int position) {
            mFloatViewOriginPosition = position;
            return super.onCreateFloatView(position);
        }

        private int mFloatViewHeight = -1; // cache height

        @Override
        public void onDragFloatView(View floatView, Point floatPoint, Point touchPoint) {
            final int addButtonPosition = mAdapter.getCount() - 1;
            final int first = mListView.getFirstVisiblePosition();
            final int lvDivHeight = mListView.getDividerHeight();

            if (mFloatViewHeight == -1) {
                mFloatViewHeight = floatView.getHeight();
            }

            View div = mListView.getChildAt(addButtonPosition - first);

            if (touchPoint.x > mListView.getWidth() / 2) {
                float scale = touchPoint.x - mListView.getWidth() / 2;
                scale /= (float) (mListView.getWidth() / 5);
                ViewGroup.LayoutParams lp = floatView.getLayoutParams();
                lp.height = Math.max(mFloatViewHeight, (int) (scale * mFloatViewHeight));
                floatView.setLayoutParams(lp);
            }

            if (div != null) {
                if (mFloatViewOriginPosition > addButtonPosition) {
                    // don't allow floating View to go above
                    // section divider
                    final int limit = div.getBottom() + lvDivHeight;
                    if (floatPoint.y < limit) {
                        floatPoint.y = limit;
                    }
                } else {
                    // don't allow floating View to go below
                    // section divider
                    final int limit = div.getTop() - lvDivHeight - floatView.getHeight();
                    if (floatPoint.y > limit) {
                        floatPoint.y = limit;
                    }
                }
            }
        }
    }
}
