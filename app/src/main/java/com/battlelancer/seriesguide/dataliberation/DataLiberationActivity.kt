package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.content.Intent
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
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
        }
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
        private const val EXTRA_SHOW_AUTOBACKUP = "EXTRA_SHOW_AUTOBACKUP"

        @JvmStatic
        fun intentToShowAutoBackup(context: Context): Intent {
            return Intent(context, DataLiberationActivity::class.java)
                .putExtra(EXTRA_SHOW_AUTOBACKUP, true)
        }
    }

}
