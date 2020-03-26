package com.battlelancer.seriesguide.ui.preferences

import android.os.Bundle
import android.view.View
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityMoreOptionsBinding
import com.battlelancer.seriesguide.ui.BaseTopActivity

/**
 * Displays accounts, links to unlock all features, settings and help
 * and if the app does no longer receive updates.
 */
class MoreOptionsActivity : BaseTopActivity() {

    private lateinit var binding: ActivityMoreOptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar()
        setupBottomNavigation(R.id.navigation_item_more)
    }

    override fun getSnackbarParentView(): View {
        return binding.coordinatorLayoutMoreOptions
    }
}
