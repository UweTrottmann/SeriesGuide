// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.diagnostics

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityDebugLogBinding
import com.battlelancer.seriesguide.ui.BaseThemeActivity
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * Displays debug log from [DebugLogBuffer], allows to save it to a file.
 */
class DebugLogActivity : BaseThemeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDebugLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        setupActionBar()

        initViews(binding)
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initViews(binding: ActivityDebugLogBinding) {
        ThemeUtils.applyBottomPaddingForNavigationBar(binding.recyclerViewDebugLogs)

        val adapter = DebugLogAdapter()
        binding.recyclerViewDebugLogs.adapter = adapter
        binding.recyclerViewDebugLogs.layoutManager = LinearLayoutManager(this)

        adapter.setData(DebugLogBuffer.getInstance(this).bufferedLogs())
    }

}