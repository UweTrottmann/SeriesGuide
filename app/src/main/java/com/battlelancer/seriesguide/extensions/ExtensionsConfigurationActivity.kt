// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2024 Uwe Trottmann

package com.battlelancer.seriesguide.extensions

import android.os.Bundle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SinglePaneActivity
import com.battlelancer.seriesguide.util.commitReorderingAllowed

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
            supportFragmentManager.commitReorderingAllowed {
                add(R.id.content_frame, f)
            }
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    companion object {
        const val LOADER_ACTIONS_ID = 100
    }
}
