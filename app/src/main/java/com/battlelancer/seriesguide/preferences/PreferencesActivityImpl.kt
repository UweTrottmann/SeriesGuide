// SPDX-License-Identifier: Apache-2.0
// Copyright 2022-2024 Uwe Trottmann

package com.battlelancer.seriesguide.preferences

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentTransaction
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseThemeActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.commitReorderingAllowed

/**
 * Allows tweaking of various SeriesGuide settings. Does NOT inherit
 * from [com.battlelancer.seriesguide.ui.BaseActivity] to avoid
 * handling actions which might be confusing while adjusting settings.
 */
open class PreferencesActivityImpl : BaseThemeActivity() {

    class UpdateSummariesEvent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        ThemeUtils.configureForEdgeToEdge(findViewById(R.id.rootLayoutSettings))
        setupActionBar()

        if (savedInstanceState == null) {
            supportFragmentManager.commitReorderingAllowed {
                add(R.id.containerSettings, SgPreferencesFragment())
            }

            // open a sub settings screen if requested
            val settingsScreen = intent.getStringExtra(EXTRA_SETTINGS_SCREEN)
            if (settingsScreen != null) {
                switchToSettings(settingsScreen)
            }
        }

        // Because the multi-screen support built into preferences library is not used,
        // need to pop fragments manually
        onBackPressedDispatcher.addCallback(onBackPressedPopBackStack)
        updateOnBackPressedPopBackStack()
    }

    private val onBackPressedPopBackStack = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    private fun updateOnBackPressedPopBackStack() {
        onBackPressedPopBackStack.isEnabled = supportFragmentManager.backStackEntryCount > 0
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun switchToSettings(settingsId: String) {
        val f = SgPreferencesFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_SETTINGS_SCREEN, settingsId)
            }
        }
        supportFragmentManager.commitReorderingAllowed {
            replace(R.id.containerSettings, f)
            // Keep transition for predictive back animation
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            addToBackStack(null)
        }
        updateOnBackPressedPopBackStack()
    }

    companion object {
        const val EXTRA_SETTINGS_SCREEN = "settingsScreen"
    }

}