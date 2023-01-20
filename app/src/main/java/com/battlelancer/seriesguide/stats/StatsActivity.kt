package com.battlelancer.seriesguide.stats

import android.os.Bundle
import android.view.View
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.util.ThemeUtils

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
            val f = StatsFragment()
            val ft = supportFragmentManager.beginTransaction()
            ft.add(R.id.content_frame, f)
            ft.commit()
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