package com.battlelancer.seriesguide.extensions;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ExtensionsAdapter;
import com.battlelancer.seriesguide.loaders.AvailableExtensionsLoader;
import com.battlelancer.seriesguide.util.Utils;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Provides tools to display all installed extensions and enable or disable them.
 */
public class ExtensionsConfigurationFragment extends Fragment
        implements AdapterView.OnItemClickListener {

    public static final int EXTENSION_LIMIT_FREE = 10;

    private static final String TAG = "Extension Configuration";

    @BindView(R.id.listViewExtensionsConfiguration) DragSortListView listView;

    private ExtensionsAdapter adapter;
    private PopupMenu addExtensionPopupMenu;
    private Unbinder unbinder;

    private List<ExtensionManager.Extension> disabledExtensions = new ArrayList<>();
    private List<ComponentName> enabledNames;

    @SuppressLint("ClickableViewAccessibility") // ordering not supported if non-touch
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_extensions_configuration, container, false);
        unbinder = ButterKnife.bind(this, v);

        final ExtensionsDragSortController dragSortController = new ExtensionsDragSortController();
        listView.setFloatViewManager(dragSortController);
        listView.setOnTouchListener(dragSortController);
        listView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                ComponentName extension = enabledNames.remove(from);
                enabledNames.add(to, extension);
                saveExtensions();
            }
        });
        listView.setOnItemClickListener(this);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ExtensionsAdapter(getActivity());
        listView.setAdapter(adapter);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        getLoaderManager().restartLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID, null,
                loaderCallbacks);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (BuildConfig.DEBUG) {
            // add debug options to enable/disable all extensions
            inflater.inflate(R.menu.extensions_configuration_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_extensions_enable) {
            List<ExtensionManager.Extension> extensions = ExtensionManager.get()
                    .queryAllAvailableExtensions(getContext());
            List<ComponentName> enabledExtensions = new ArrayList<>();
            for (ExtensionManager.Extension extension : extensions) {
                enabledExtensions.add(extension.componentName);
            }
            ExtensionManager.get()
                    .setEnabledExtensions(getContext(), enabledExtensions);
            Toast.makeText(getActivity(), "Enabled all available extensions", Toast.LENGTH_LONG)
                    .show();
            return true;
        }
        if (itemId == R.id.menu_action_extensions_disable) {
            ExtensionManager.get()
                    .setEnabledExtensions(getContext(), new ArrayList<ComponentName>());
            Toast.makeText(getActivity(), "Disabled all available extensions", Toast.LENGTH_LONG)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    private LoaderManager.LoaderCallbacks<List<ExtensionManager.Extension>> loaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<ExtensionManager.Extension>>() {
        @Override
        public Loader<List<ExtensionManager.Extension>> onCreateLoader(int id, Bundle args) {
            return new AvailableExtensionsLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<ExtensionManager.Extension>> loader,
                @NonNull List<ExtensionManager.Extension> all) {
            if (all.size() == 0) {
                Timber.d("Did not find any extension");
            } else {
                Timber.d("Found %d extensions", all.size());
            }

            // find all disabled extensions
            List<ComponentName> enabledNames = ExtensionManager.get()
                    .getEnabledExtensions(getContext());
            List<ExtensionManager.Extension> disabled = new ArrayList<>();
            Map<ComponentName, ExtensionManager.Extension> enabledByComponent = new HashMap<>();
            for (ExtensionManager.Extension extension : all) {
                if (enabledNames.contains(extension.componentName)) {
                    enabledByComponent.put(extension.componentName, extension);
                } else {
                    disabled.add(extension);
                }
            }

            // list enabled extensions in order dictated by extension manager
            List<ExtensionManager.Extension> enabled = new ArrayList<>();
            for (ComponentName component : enabledNames) {
                enabled.add(enabledByComponent.get(component));
            }

            ExtensionsConfigurationFragment.this.enabledNames = enabledNames;
            disabledExtensions = disabled;

            // force re-creation of extension add menu
            if (addExtensionPopupMenu != null) {
                addExtensionPopupMenu.dismiss();
                addExtensionPopupMenu = null;
            }

            // refresh enabled extensions list
            adapter.clear();
            adapter.addAll(enabled);
        }

        @Override
        public void onLoaderReset(Loader<List<ExtensionManager.Extension>> loader) {
            adapter.clear();
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == adapter.getCount() - 1) {
            // non-supporters only can add a few extensions
            if (adapter.getCount() - 1 == EXTENSION_LIMIT_FREE
                    && !Utils.hasAccessToX(getActivity())) {
                Utils.advertiseSubscription(getActivity());
                return;
            }
            showAddExtensionPopupMenu(view.findViewById(R.id.textViewItemExtensionAddLabel));
            Utils.trackAction(getActivity(), TAG, "Add extension");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ExtensionsAdapter.ExtensionDisableRequestEvent event) {
        enabledNames.remove(event.position);
        saveExtensions();
        Utils.trackAction(getActivity(), TAG, "Remove extension");
    }

    private void showAddExtensionPopupMenu(View anchorView) {
        if (addExtensionPopupMenu != null) {
            addExtensionPopupMenu.dismiss();
        }

        addExtensionPopupMenu = new PopupMenu(getActivity(), anchorView);
        Menu menu = addExtensionPopupMenu.getMenu();

        // sort disabled extensions alphabetically
        Collections.sort(disabledExtensions, alphabeticalComparator);
        // list of installed, but disabled extensions
        for (int i = 0; i < disabledExtensions.size(); i++) {
            ExtensionManager.Extension extension = disabledExtensions.get(i);
            menu.add(Menu.NONE, i + 1, Menu.NONE, extension.label);
        }
        // no third-party extensions supported on Amazon app store for now
        if (!Utils.isAmazonVersion()) {
            // link to get more extensions
            menu.add(Menu.NONE, 0, Menu.NONE, R.string.action_extensions_search);
        }
        addExtensionPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                if (item.getItemId() == 0) {
                    // special item: search for more extensions
                    onGetMoreExtensions();
                    return true;
                }

                // add to enabled extensions
                ExtensionManager.Extension extension = disabledExtensions.get(item.getItemId() - 1);
                enabledNames.add(extension.componentName);
                saveExtensions();
                // scroll to end of list
                listView.smoothScrollToPosition(adapter.getCount() - 1);
                return true;
            }
        });

        addExtensionPopupMenu.show();
    }

    private void onGetMoreExtensions() {
        Utils.launchWebsite(getActivity(), getString(R.string.url_extensions_search), TAG,
                "Get more extensions");
    }

    private void saveExtensions() {
        ExtensionManager.get().setEnabledExtensions(getContext(), enabledNames);
        getLoaderManager().restartLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID,
                null, loaderCallbacks);
    }

    private Comparator<ExtensionManager.Extension> alphabeticalComparator
            = new Comparator<ExtensionManager.Extension>() {
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
    };

    private class ExtensionsDragSortController extends DragSortController {

        private int mFloatViewOriginPosition;

        public ExtensionsDragSortController() {
            super(listView, R.id.drag_handle, DragSortController.ON_DOWN,
                    DragSortController.CLICK_REMOVE);
            setRemoveEnabled(false);
        }

        @Override
        public int startDragPosition(MotionEvent ev) {
            int hitPosition = super.dragHandleHitPosition(ev);
            if (hitPosition >= adapter.getCount() - 1) {
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
            final int addButtonPosition = adapter.getCount() - 1;
            final int first = listView.getFirstVisiblePosition();
            final int lvDivHeight = listView.getDividerHeight();

            if (mFloatViewHeight == -1) {
                mFloatViewHeight = floatView.getHeight();
            }

            View div = listView.getChildAt(addButtonPosition - first);

            if (touchPoint.x > listView.getWidth() / 2) {
                float scale = touchPoint.x - listView.getWidth() / 2;
                scale /= (float) (listView.getWidth() / 5);
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
