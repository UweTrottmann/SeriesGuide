package com.battlelancer.seriesguide.dataliberation

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity

/**
 * Hosts a [DataLiberationFragment].
 */
class DataLiberationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singlepane)
        setupActionBar()

        if (savedInstanceState == null) {
            val showAutoBackup = intent.getBooleanExtra(
                EXTRA_SHOW_AUTOBACKUP,
                false
            )
            val f: Fragment = if (showAutoBackup) {
                AutoBackupFragment()
            } else {
                DataLiberationFragment()
            }
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
            else -> false
        }
    }

    companion object {
        const val EXTRA_SHOW_AUTOBACKUP = "EXTRA_SHOW_AUTOBACKUP"
    }

}
