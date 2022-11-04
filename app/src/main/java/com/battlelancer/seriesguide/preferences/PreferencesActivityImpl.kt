package com.battlelancer.seriesguide.preferences

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseThemeActivity
import com.battlelancer.seriesguide.util.ThemeUtils

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
        ThemeUtils.configureAppBarForContentBelow(this)
        setupActionBar()

        if (savedInstanceState == null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.add(R.id.containerSettings, SgPreferencesFragment())
            ft.commit()

            // open a sub settings screen if requested
            val settingsScreen = intent.getStringExtra(EXTRA_SETTINGS_SCREEN)
            if (settingsScreen != null) {
                switchToSettings(settingsScreen)
            }
        }

        onBackPressedDispatcher.addCallback {
            // Because the multi-screen support built into preferences library is not used,
            // need to pop fragments manually
            if (!supportFragmentManager.popBackStackImmediate()) {
                finish()
            }
        }
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

    fun switchToSettings(settingsId: String) {
        val f = SgPreferencesFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_SETTINGS_SCREEN, settingsId)
            }
        }
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.containerSettings, f)
        ft.addToBackStack(null)
        ft.commit()
    }

    companion object {
        const val EXTRA_SETTINGS_SCREEN = "settingsScreen"
    }

}