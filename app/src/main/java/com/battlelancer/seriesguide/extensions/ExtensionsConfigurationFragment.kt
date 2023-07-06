package com.battlelancer.seriesguide.extensions

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.databinding.FragmentExtensionsConfigurationBinding
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.WebTools
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortController
import com.uwetrottmann.seriesguide.widgets.dragsortview.DragSortListView
import timber.log.Timber
import java.util.Collections

/**
 * Provides tools to display all installed extensions and enable or disable them.
 */
class ExtensionsConfigurationFragment : Fragment() {

    private var binding: FragmentExtensionsConfigurationBinding? = null
    private lateinit var adapter: ExtensionsAdapter
    private var addExtensionPopupMenu: PopupMenu? = null

    private var disabledExtensions: List<Extension> = ArrayList()
    private var enabledNames: MutableList<ComponentName>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentExtensionsConfigurationBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    @SuppressLint("ClickableViewAccessibility") // ordering not supported if non-touch
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return

        ThemeUtils.applyBottomPaddingForNavigationBar(binding.listViewExtensionsConfig)
        binding.listViewExtensionsConfig.isNestedScrollingEnabled = true

        adapter = ExtensionsAdapter(requireContext(), onItemClickListener)

        val dragSortController =
            ExtensionsDragSortController(binding.listViewExtensionsConfig, adapter)
        binding.listViewExtensionsConfig.also {
            it.setFloatViewManager(dragSortController)
            it.setOnTouchListener(dragSortController)
            it.setDropListener { from: Int, to: Int ->
                val enabledNames = enabledNames ?: return@setDropListener
                val extension = enabledNames.removeAt(from)
                enabledNames.add(to, extension)
                saveExtensions()
            }
            // allow focusing menu buttons with a remote/d-pad
            it.itemsCanFocus = true
            it.adapter = adapter
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onResume() {
        super.onResume()
        LoaderManager.getInstance(this)
            .restartLoader(
                ExtensionsConfigurationActivity.LOADER_ACTIONS_ID, null,
                loaderCallbacks
            )
    }

    private val optionsMenuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            if (BuildConfig.DEBUG) {
                // add debug options to enable/disable all extensions
                menuInflater.inflate(R.menu.extensions_configuration_menu, menu)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val itemId = menuItem.itemId
            if (itemId == R.id.menu_action_extensions_enable) {
                val extensions = ExtensionManager.get(requireContext())
                    .queryAllAvailableExtensions(requireContext())
                val enabledExtensions: MutableList<ComponentName> = ArrayList()
                for (extension in extensions) {
                    enabledExtensions.add(extension.componentName)
                }
                ExtensionManager.get(requireContext())
                    .setEnabledExtensions(requireContext(), enabledExtensions)
                Toast.makeText(activity, "Enabled all available extensions", Toast.LENGTH_LONG)
                    .show()
                return true
            }
            if (itemId == R.id.menu_action_extensions_disable) {
                ExtensionManager.get(requireContext())
                    .setEnabledExtensions(requireContext(), ArrayList())
                Toast.makeText(
                    activity, "Disabled all available extensions",
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
            return false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private val loaderCallbacks: LoaderManager.LoaderCallbacks<MutableList<Extension>> =
        object : LoaderManager.LoaderCallbacks<MutableList<Extension>> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<MutableList<Extension>> {
                return AvailableExtensionsLoader(requireContext())
            }

            override fun onLoadFinished(
                loader: Loader<MutableList<Extension>>,
                all: MutableList<Extension>
            ) {
                if (all.size == 0) {
                    Timber.d("Did not find any extension")
                } else {
                    Timber.d("Found %d extensions", all.size)
                }

                // find all disabled extensions
                val enabledNames = ExtensionManager.get(context)
                    .getEnabledExtensions(context)
                val disabled: MutableList<Extension> = ArrayList()
                val enabledByComponent: MutableMap<ComponentName, Extension> = HashMap()
                for (extension in all) {
                    if (enabledNames.contains(extension.componentName)) {
                        enabledByComponent[extension.componentName] = extension
                    } else {
                        disabled.add(extension)
                    }
                }

                // list enabled extensions in order dictated by extension manager
                val enabled: MutableList<Extension> = ArrayList()
                for (component in enabledNames) {
                    enabled.add(enabledByComponent[component]!!)
                }

                this@ExtensionsConfigurationFragment.enabledNames = enabledNames
                disabledExtensions = disabled

                // force re-creation of extension add menu
                addExtensionPopupMenu?.dismiss()
                addExtensionPopupMenu = null

                // refresh enabled extensions list
                adapter.clear()
                adapter.addAll(enabled)
            }

            override fun onLoaderReset(loader: Loader<MutableList<Extension>>) {
                adapter.clear()
            }
        }
    private val onItemClickListener: ExtensionsAdapter.OnItemClickListener =
        object : ExtensionsAdapter.OnItemClickListener {
            override fun onExtensionMenuButtonClick(
                anchor: View,
                extension: Extension, position: Int
            ) {
                showExtensionPopupMenu(anchor, extension, position)
            }

            override fun onAddExtensionClick(anchor: View) {
                showAddExtensionPopupMenu(anchor)
            }
        }

    private fun showExtensionPopupMenu(anchor: View, extension: Extension, position: Int) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.extension_menu, popupMenu.menu)
        if (extension.settingsActivity == null) {
            val item = popupMenu.menu.findItem(R.id.menu_action_extension_settings)
            item.isVisible = false
            item.isEnabled = false
        }
        popupMenu.setOnMenuItemClickListener(
            OverflowItemClickListener(extension.settingsActivity, position)
        )
        popupMenu.show()
    }

    private inner class OverflowItemClickListener(
        private val settingsActivity: ComponentName?,
        private val position: Int
    ) : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            val itemId = item.itemId
            if (itemId == R.id.menu_action_extension_settings) {
                // launch settings activity
                Utils.tryStartActivity(
                    requireContext(),
                    Intent()
                        .setComponent(settingsActivity)
                        .putExtra(SeriesGuideExtension.EXTRA_FROM_SERIESGUIDE_SETTINGS, true),
                    true
                )
                ExtensionManager.get(context).clearActionsCache()
                return true
            } else if (itemId == R.id.menu_action_extension_disable) {
                enabledNames?.removeAt(position)
                saveExtensions()
                return true
            }
            return false
        }
    }

    private fun showAddExtensionPopupMenu(anchorView: View) {
        addExtensionPopupMenu?.dismiss()
        val popupMenu = PopupMenu(activity, anchorView)
            .also { addExtensionPopupMenu = it }
        val menu = popupMenu.menu

        // sort disabled extensions alphabetically
        Collections.sort(disabledExtensions, alphabeticalComparator)
        // list of installed, but disabled extensions
        for (i in disabledExtensions.indices) {
            val extension = disabledExtensions[i]
            menu.add(Menu.NONE, i + 1, Menu.NONE, extension.label)
        }
        // no third-party extensions supported on Amazon app store for now
        if (!Utils.isAmazonVersion()) {
            // link to get more extensions
            menu.add(Menu.NONE, 0, Menu.NONE, R.string.action_extensions_search)
        }
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == 0) {
                // special item: search for more extensions
                WebTools.openInApp(
                    requireContext(),
                    getString(R.string.url_extensions_search)
                )
                return@setOnMenuItemClickListener true
            }

            // add to enabled extensions
            val extension = disabledExtensions[item.itemId - 1]
            enabledNames?.add(extension.componentName)
            saveExtensions()
            // scroll to end of list
            binding?.listViewExtensionsConfig?.smoothScrollToPosition(adapter.count - 1)
            true
        }
        popupMenu.show()
    }

    private fun saveExtensions() {
        val enabledNames = enabledNames ?: return
        ExtensionManager.get(context).setEnabledExtensions(context, enabledNames)
        LoaderManager.getInstance(this)
            .restartLoader(
                ExtensionsConfigurationActivity.LOADER_ACTIONS_ID, null,
                loaderCallbacks
            )
    }

    private val alphabeticalComparator: Comparator<Extension> = object : Comparator<Extension> {
        override fun compare(extension1: Extension, extension2: Extension): Int {
            val title1 = createTitle(extension1)
            val title2 = createTitle(extension2)
            return title1.compareTo(title2, ignoreCase = true)
        }

        private fun createTitle(extension: Extension): String {
            var title = extension.label
            if (TextUtils.isEmpty(title)) {
                title = extension.componentName.flattenToShortString()
            }
            return title
        }
    }

    class ExtensionsDragSortController(
        private val dragSortListView: DragSortListView,
        private val adapter: ExtensionsAdapter
    ) : DragSortController(
        dragSortListView,
        R.id.drag_handle,
        ON_DOWN,
        CLICK_REMOVE
    ) {

        private var floatViewOriginPosition = 0

        init {
            isRemoveEnabled = false
        }

        override fun startDragPosition(ev: MotionEvent): Int {
            val hitPosition = super.dragHandleHitPosition(ev)
            return if (hitPosition >= adapter.count - 1) {
                MISS
            } else hitPosition
        }

        override fun onCreateFloatView(position: Int): View {
            floatViewOriginPosition = position
            return super.onCreateFloatView(position)
        }

        override fun onDragFloatView(floatView: View, floatPoint: Point, touchPoint: Point) {
            val listView = dragSortListView
            val addButtonPosition = adapter.count - 1
            val first = listView.firstVisiblePosition
            val lvDivHeight = listView.dividerHeight

            val divider = listView.getChildAt(addButtonPosition - first)
            if (divider != null) {
                if (floatViewOriginPosition > addButtonPosition) {
                    // don't allow floating View to go above
                    // section divider
                    val limit = divider.bottom + lvDivHeight
                    if (floatPoint.y < limit) {
                        floatPoint.y = limit
                    }
                } else {
                    // don't allow floating View to go below
                    // section divider
                    val limit = divider.top - lvDivHeight - floatView.height
                    if (floatPoint.y > limit) {
                        floatPoint.y = limit
                    }
                }
            }
        }
    }
}