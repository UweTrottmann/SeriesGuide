package com.battlelancer.seriesguide.extensions;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortController;
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortListView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

/**
 * Provides tools to display all installed extensions and enable or disable them.
 */
public class ExtensionsConfigurationFragment extends Fragment {

    @BindView(R.id.listViewExtensionsConfiguration) DragSortListView listView;

    private ExtensionsAdapter adapter;
    private PopupMenu addExtensionPopupMenu;
    private Unbinder unbinder;

    private List<Extension> disabledExtensions = new ArrayList<>();
    private List<ComponentName> enabledNames;

    @SuppressLint("ClickableViewAccessibility") // ordering not supported if non-touch
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_extensions_configuration, container, false);
        unbinder = ButterKnife.bind(this, v);

        final ExtensionsDragSortController dragSortController = new ExtensionsDragSortController();
        listView.setFloatViewManager(dragSortController);
        listView.setOnTouchListener(dragSortController);
        listView.setDropListener((from, to) -> {
            ComponentName extension = enabledNames.remove(from);
            enabledNames.add(to, extension);
            saveExtensions();
        });
        // allow focusing menu buttons with a remote/d-pad
        listView.setItemsCanFocus(true);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ExtensionsAdapter(getActivity(), onItemClickListener);
        listView.setAdapter(adapter);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        LoaderManager.getInstance(this)
                .restartLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID, null,
                        loaderCallbacks);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
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
            List<Extension> extensions = ExtensionManager.get(requireContext())
                    .queryAllAvailableExtensions(requireContext());
            List<ComponentName> enabledExtensions = new ArrayList<>();
            for (Extension extension : extensions) {
                enabledExtensions.add(extension.componentName);
            }
            ExtensionManager.get(requireContext())
                    .setEnabledExtensions(requireContext(), enabledExtensions);
            Toast.makeText(getActivity(), "Enabled all available extensions", Toast.LENGTH_LONG)
                    .show();
            return true;
        }
        if (itemId == R.id.menu_action_extensions_disable) {
            ExtensionManager.get(requireContext())
                    .setEnabledExtensions(requireContext(), new ArrayList<>());
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

    private final LoaderManager.LoaderCallbacks<List<Extension>> loaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<Extension>>() {
        @Override
        public Loader<List<Extension>> onCreateLoader(int id, Bundle args) {
            return new AvailableExtensionsLoader(requireContext());
        }

        @Override
        public void onLoadFinished(@NonNull Loader<List<Extension>> loader,
                @NonNull List<Extension> all) {
            if (all.size() == 0) {
                Timber.d("Did not find any extension");
            } else {
                Timber.d("Found %d extensions", all.size());
            }

            // find all disabled extensions
            List<ComponentName> enabledNames = ExtensionManager.get(getContext())
                    .getEnabledExtensions(getContext());
            List<Extension> disabled = new ArrayList<>();
            Map<ComponentName, Extension> enabledByComponent = new HashMap<>();
            for (Extension extension : all) {
                if (enabledNames.contains(extension.componentName)) {
                    enabledByComponent.put(extension.componentName, extension);
                } else {
                    disabled.add(extension);
                }
            }

            // list enabled extensions in order dictated by extension manager
            List<Extension> enabled = new ArrayList<>();
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
        public void onLoaderReset(@NonNull Loader<List<Extension>> loader) {
            adapter.clear();
        }
    };

    private ExtensionsAdapter.OnItemClickListener onItemClickListener =
            new ExtensionsAdapter.OnItemClickListener() {
                @Override
                public void onExtensionMenuButtonClick(View anchor,
                        Extension extension, int position) {
                    showExtensionPopupMenu(anchor, extension, position);
                }

                @Override
                public void onAddExtensionClick(View anchor) {
                    showAddExtensionPopupMenu(anchor);
                }
            };

    private void showExtensionPopupMenu(View anchor, Extension extension,
            int position) {
        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        popupMenu.getMenuInflater().inflate(R.menu.extension_menu, popupMenu.getMenu());
        if (extension.settingsActivity == null) {
            MenuItem item = popupMenu.getMenu()
                    .findItem(R.id.menu_action_extension_settings);
            item.setVisible(false);
            item.setEnabled(false);
        }
        popupMenu.setOnMenuItemClickListener(
                new OverflowItemClickListener(extension.settingsActivity, position));
        popupMenu.show();
    }

    private class OverflowItemClickListener implements PopupMenu.OnMenuItemClickListener {

        @Nullable private final ComponentName settingsActivity;
        private final int position;

        OverflowItemClickListener(@Nullable ComponentName settingsActivity, int position) {
            this.settingsActivity = settingsActivity;
            this.position = position;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_action_extension_settings:
                    // launch settings activity
                    Utils.tryStartActivity(requireContext(), new Intent()
                                    .setComponent(settingsActivity)
                                    .putExtra(SeriesGuideExtension.EXTRA_FROM_SERIESGUIDE_SETTINGS,
                                            true),
                            true
                    );
                    ExtensionManager.get(getContext()).clearActionsCache();
                    return true;
                case R.id.menu_action_extension_disable:
                    enabledNames.remove(position);
                    saveExtensions();
                    return true;
            }
            return false;
        }
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
            Extension extension = disabledExtensions.get(i);
            menu.add(Menu.NONE, i + 1, Menu.NONE, extension.label);
        }
        // no third-party extensions supported on Amazon app store for now
        if (!Utils.isAmazonVersion()) {
            // link to get more extensions
            menu.add(Menu.NONE, 0, Menu.NONE, R.string.action_extensions_search);
        }
        addExtensionPopupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 0) {
                // special item: search for more extensions
                onGetMoreExtensions();
                return true;
            }

            // add to enabled extensions
            Extension extension = disabledExtensions.get(item.getItemId() - 1);
            enabledNames.add(extension.componentName);
            saveExtensions();
            // scroll to end of list
            listView.smoothScrollToPosition(adapter.getCount() - 1);
            return true;
        });

        addExtensionPopupMenu.show();
    }

    private void onGetMoreExtensions() {
        Utils.launchWebsite(getActivity(), getString(R.string.url_extensions_search));
    }

    private void saveExtensions() {
        ExtensionManager.get(getContext()).setEnabledExtensions(getContext(), enabledNames);
        LoaderManager.getInstance(this)
                .restartLoader(ExtensionsConfigurationActivity.LOADER_ACTIONS_ID, null,
                        loaderCallbacks);
    }

    private Comparator<Extension> alphabeticalComparator
            = new Comparator<Extension>() {
        @Override
        public int compare(Extension extension1,
                Extension extension2) {
            String title1 = createTitle(extension1);
            String title2 = createTitle(extension2);
            return title1.compareToIgnoreCase(title2);
        }

        private String createTitle(Extension extension) {
            String title = extension.label;
            if (TextUtils.isEmpty(title)) {
                title = extension.componentName.flattenToShortString();
            }
            return title;
        }
    };

    private class ExtensionsDragSortController extends DragSortController {

        private int floatViewOriginPosition;

        ExtensionsDragSortController() {
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
            floatViewOriginPosition = position;
            return super.onCreateFloatView(position);
        }

        private int floatViewHeight = -1; // cache height

        @Override
        public void onDragFloatView(View floatView, Point floatPoint, Point touchPoint) {
            final int addButtonPosition = adapter.getCount() - 1;
            final int first = listView.getFirstVisiblePosition();
            final int lvDivHeight = listView.getDividerHeight();

            if (floatViewHeight == -1) {
                floatViewHeight = floatView.getHeight();
            }

            View div = listView.getChildAt(addButtonPosition - first);

            if (touchPoint.x > listView.getWidth() / 2) {
                float scale = touchPoint.x - listView.getWidth() / 2;
                scale /= (float) (listView.getWidth() / 5);
                ViewGroup.LayoutParams lp = floatView.getLayoutParams();
                lp.height = Math.max(floatViewHeight, (int) (scale * floatViewHeight));
                floatView.setLayoutParams(lp);
            }

            if (div != null) {
                if (floatViewOriginPosition > addButtonPosition) {
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
