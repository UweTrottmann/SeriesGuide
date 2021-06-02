package com.battlelancer.seriesguide.ui.preferences

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences

/**
 * Just hosts a [AboutPreferencesFragment].
 */
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(SeriesGuidePreferences.THEME)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setupActionBar()
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.sgToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}