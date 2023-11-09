// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.preferences

import android.os.Bundle
import android.view.MenuItem
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseThemeActivity
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * Just hosts a [AboutPreferencesFragment].
 */
class AboutActivity : BaseThemeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        ThemeUtils.configureForEdgeToEdge(findViewById(R.id.rootLayoutAbout))
        setupActionBar()
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}