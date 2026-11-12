// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.diagnostics

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityDebugLogBinding
import com.battlelancer.seriesguide.getSgAppContainer
import com.battlelancer.seriesguide.ui.BaseThemeActivity
import com.battlelancer.seriesguide.ui.widgets.SgFastScroller
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.tryLaunch
import kotlinx.coroutines.launch

/**
 * Displays debug log from [DebugLogBuffer], allows to save it to a file.
 */
class DebugLogActivity : BaseThemeActivity() {

    private val model by viewModels<DebugLogActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDebugLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        setupActionBar()

        initViews(binding)

        addMenuProvider(
            optionsMenuProvider,
            this,
            Lifecycle.State.RESUMED
        )
    }

    override fun setupActionBar() {
        super.setupActionBar()
        setTitle(R.string.title_debug_log)
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initViews(binding: ActivityDebugLogBinding) {
        ThemeUtils.applyBottomPaddingForNavigationBar(binding.recyclerViewDebugLog)

        val recyclerView = binding.recyclerViewDebugLog
        val adapter = DebugLogAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        SgFastScroller(this, recyclerView)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.logEntries.collect {
                    // Like a terminal, keep showing the latest entries at the bottom, but only if
                    // the list is already scrolled to the bottom (or is empty, so on first load).
                    val isAtBottom = !recyclerView.canScrollVertically(1)
                    adapter.submitList(it) {
                        val itemCount = adapter.itemCount
                        if (isAtBottom && itemCount > 0) {
                            recyclerView.scrollToPosition(itemCount - 1)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect {
                    binding.frameLayoutDebugLogProgress.isGone = !it.isSaving
                    if (it.userMessage != null) {
                        Toast.makeText(this@DebugLogActivity, it.userMessage, Toast.LENGTH_SHORT)
                            .show()
                        model.userMessageShown()
                    }
                }
            }
        }
    }

    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.debug_log_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_action_debug_log_save -> {
                    createDebugLogFile()
                    return true
                }

                else -> return false
            }
        }
    }

    fun createDebugLogFile() {
        // Note: instead of saving the file in internal app storage and sharing it, let the user
        // save the file and decide how to send it (via email, forum, ...).
        createDebugFileCallback.tryLaunch(getSgAppContainer().debugLogBuffer.logFileName, this)
    }

    /**
     * Input is used as suggested file name.
     */
    class CreateDebugLogFileContract : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT)
                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TITLE, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            if (resultCode != Activity.RESULT_OK) {
                return null
            }
            return intent?.data
        }
    }

    private val createDebugFileCallback =
        registerForActivityResult(CreateDebugLogFileContract()) { uri ->
            if (uri != null) {
                model.saveDebugLogToFile(uri)
            }
        }

    companion object {
        fun intent(context: Context) = Intent(context, DebugLogActivity::class.java)
    }

}