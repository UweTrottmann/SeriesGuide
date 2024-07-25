// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SinglePaneActivity
import com.battlelancer.seriesguide.util.commitReorderingAllowed

/**
 * Hosts a [DataLiberationFragment].
 */
class DataLiberationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SinglePaneActivity.onCreateFor(this)
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
            supportFragmentManager.commitReorderingAllowed {
                add(R.id.content_frame, f)
            }
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
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
