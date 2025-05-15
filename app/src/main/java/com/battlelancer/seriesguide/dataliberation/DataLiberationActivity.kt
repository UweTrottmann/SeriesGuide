// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity.Companion.EXTRA_SHOW_AUTOBACKUP
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SinglePaneActivity
import com.battlelancer.seriesguide.util.commitReorderingAllowed

/**
 * Depending on [EXTRA_SHOW_AUTOBACKUP] hosts an [AutoBackupFragment] or [DataLiberationFragment].
 */
class DataLiberationActivity : BaseActivity() {

    private val showAutoBackup: Boolean
        get() = intent.getBooleanExtra(EXTRA_SHOW_AUTOBACKUP, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SinglePaneActivity.onCreateFor(this)
        setupActionBar()

        if (savedInstanceState == null) {
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
        val titleRes =
            if (showAutoBackup) R.string.pref_autobackup else R.string.backup
        setTitle(titleRes)
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
            setTitle(titleRes)
        }
    }

    companion object {
        private const val EXTRA_SHOW_AUTOBACKUP = "EXTRA_SHOW_AUTOBACKUP"

        fun intent(context: Context) = Intent(context, DataLiberationActivity::class.java)

        fun intentToShowAutoBackup(context: Context): Intent {
            return intent(context)
                .putExtra(EXTRA_SHOW_AUTOBACKUP, true)
        }
    }

}
