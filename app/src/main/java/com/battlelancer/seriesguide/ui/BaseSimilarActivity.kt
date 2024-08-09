// SPDX-License-Identifier: Apache-2.0
// Copyright 2023-2024 Uwe Trottmann

package com.battlelancer.seriesguide.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.commitReorderingAllowed

/**
 * Is a [SinglePaneActivity].
 */
abstract class BaseSimilarActivity : BaseActivity() {

    abstract val liftOnScrollTargetViewId: Int
    abstract val titleStringRes: Int
    abstract fun createFragment(tmdbId: Int, title: String?): Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = SinglePaneActivity.onCreateFor(this)
        binding.sgAppBarLayout.sgAppBarLayout.liftOnScrollTargetViewId =
            liftOnScrollTargetViewId
        setupActionBar()

        val tmdbId = intent.getIntExtra(EXTRA_TMDB_ID, 0)
        if (tmdbId <= 0) {
            finish()
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE)
        if (savedInstanceState == null) {
            addFragment(tmdbId, title)
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(titleStringRes)
    }


    fun addFragment(
        tmdbId: Int,
        title: String?,
        addToBackStack: Boolean = false
    ) {
        val fragment = createFragment(tmdbId, title)
        supportFragmentManager.commitReorderingAllowed {
            if (addToBackStack) {
                replace(R.id.content_frame, fragment)
                addToBackStack(null)
            } else {
                add(R.id.content_frame, fragment)
            }
        }
    }

    companion object {
        private const val EXTRA_TMDB_ID = "EXTRA_TMDB_ID"
        private const val EXTRA_TITLE = "EXTRA_TITLE"

        fun Intent.putExtras(showTmdbId: Int, title: String?): Intent {
            return this
                .putExtra(EXTRA_TMDB_ID, showTmdbId)
                .putExtra(EXTRA_TITLE, title)
        }
    }

}