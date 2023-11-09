// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.extensions

import android.os.Bundle
import android.view.MenuItem
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SinglePaneActivity

/**
 * Just hosting a [com.battlelancer.seriesguide.extensions.ExtensionsConfigurationFragment].
 *
 * Is exported so it can be launched by extension.
 *
 * Test with 'adb shell am start -n com.battlelancer.seriesguide/com.battlelancer.seriesguide.extensions.ExtensionsConfigurationActivity'.
 */
class ExtensionsConfigurationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SinglePaneActivity.onCreateFor(this)
        setupActionBar()

        if (savedInstanceState == null) {
            val f = if (
                intent.hasExtra(SeriesGuideExtension.EXTRA_FROM_SERIESGUIDE_SETTINGS)
            ) {
                // Launch Amazon extension settings instead.
                AmazonConfigurationFragment()
            } else {
                ExtensionsConfigurationFragment()
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.content_frame, f)
                .commit()
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
        }
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

    companion object {
        const val LOADER_ACTIONS_ID = 100
    }
}
