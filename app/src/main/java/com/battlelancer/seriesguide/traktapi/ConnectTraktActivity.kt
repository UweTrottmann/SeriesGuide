package com.battlelancer.seriesguide.traktapi

import android.os.Bundle
import android.view.MenuItem
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity

/**
 * Hosts a [ConnectTraktCredentialsFragment].
 */
class ConnectTraktActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singlepane)
        setupActionBar()

        if (savedInstanceState == null) {
            val f = ConnectTraktCredentialsFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.content_frame, f)
                .commit()
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
