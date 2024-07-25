// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.stats

import android.os.Bundle
import android.view.View
import androidx.fragment.app.add
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.commitReorderingAllowed

/**
 * Hosts fragments displaying statistics.
 */
class StatsActivity : BaseTopActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        ThemeUtils.configureForEdgeToEdge(findViewById(R.id.rootLayoutStats))
        setupActionBar()
        setupBottomNavigation(R.id.navigation_item_stats)

        if (savedInstanceState == null) {
            supportFragmentManager.commitReorderingAllowed {
                add<StatsFragment>(R.id.content_frame)
            }
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setTitle(R.string.statistics)
    }

    override val snackbarParentView: View
        get() = findViewById(R.id.coordinatorLayoutStats)

    override fun onResume() {
        super.onResume()
        // prefs might have changed, update menu
        invalidateOptionsMenu()
    }
}