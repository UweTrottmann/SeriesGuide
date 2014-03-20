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
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.PopupMenu;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ExtensionsAdapter;
import com.battlelancer.seriesguide.loaders.AvailableActionsLoader;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.List;
import timber.log.Timber;

/**
 * Provides tools to display all installed extensions and enable or disable them.
 */
public class ExtensionsConfigurationFragment extends SherlockFragment
        implements AdapterView.OnItemClickListener {

    @InjectView(R.id.listViewExtensionsConfiguration) DragSortListView mListView;

    private ExtensionsAdapter mAdapter;
    private PopupMenu mAddExtensionPopupMenu;

    private List<ExtensionManager.Extension> mAvailableExtensions;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_extensions_configuration, null);
        ButterKnife.inject(this, v);

        final ExtensionsDragSortController dragSortController = new ExtensionsDragSortController();
        mListView.setFloatViewManager(dragSortController);
        mListView.setOnTouchListener(dragSortController);
        mListView.setOnItemClickListener(this);

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
        super.onCreateOptionsMenu(menu, inflater);
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

            mAvailableExtensions = data;

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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == mAdapter.getCount() - 1) {
            showAddExtensionPopupMenu(view.findViewById(R.id.textViewItemExtensionAddLabel));
        }
    }

    private void showAddExtensionPopupMenu(View anchorView) {
        if (mAddExtensionPopupMenu != null) {
            mAddExtensionPopupMenu.dismiss();
        }

        mAddExtensionPopupMenu = new PopupMenu(getActivity(), anchorView);
        for (int i = 0; i < mAvailableExtensions.size(); i++) {
            ExtensionManager.Extension extension = mAvailableExtensions.get(i);
            String label = extension.label;
            if (TextUtils.isEmpty(label)) {
                label = extension.componentName.flattenToShortString();
            }
            mAddExtensionPopupMenu.getMenu().add(Menu.NONE, i, Menu.NONE, label);
        }

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
            // short vibrate to signal drag has started
            Vibrator v = (Vibrator) mListView.getContext()
                    .getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(10);
            mFloatViewOriginPosition = position;
            return mAdapter.getView(position, null, mListView);
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

        @Override
        public void onDestroyFloatView(View floatView) {
            //do nothing; block super from crashing
        }
    }
}
