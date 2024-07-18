// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.backend

import android.os.Bundle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SinglePaneActivity

/**
 * Hosts [CloudSetupFragment] to setup hexagon.
 */
class CloudSetupActivity : BaseActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SinglePaneActivity.onCreateFor(this)
        setupActionBar()

        if (savedInstanceState == null) {
            val f = CloudSetupFragment()
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

}
