package com.battlelancer.seriesguide.ui.preferences

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.battlelancer.seriesguide.R

/**
 * Just hosts a [AboutPreferencesFragment].
 */
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
    }
}