// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.ui

import android.content.Context
import android.content.Intent
import com.battlelancer.seriesguide.shows.overview.OverviewActivityImpl

/**
 * Shell to avoid breaking AndroidManifest.xml and shortcuts.xml and other references to this name.
 * Implementation moved to feature-specific package.
 */
class OverviewActivity : OverviewActivityImpl() {

    companion object {
        /**
         * After opening, switches to overview tab (only if not multi-pane).
         */
        @JvmStatic
        fun intentShowByTmdbId(context: Context, showTmdbId: Int): Intent {
            return Intent(context, OverviewActivity::class.java)
                .putExtra(EXTRA_INT_SHOW_TMDBID, showTmdbId)
        }

        /**
         * After opening, switches to overview tab (only if not multi-pane).
         */
        @JvmStatic
        fun intentShow(context: Context, showRowId: Long): Intent {
            return Intent(context, OverviewActivity::class.java)
                .putExtra(EXTRA_LONG_SHOW_ROWID, showRowId)
        }

        /**
         * After opening, switches to seasons tab (only if not multi-pane).
         */
        fun intentSeasons(context: Context, showRowId: Long): Intent {
            return intentShow(context, showRowId).putExtra(EXTRA_BOOLEAN_DISPLAY_SEASONS, true)
        }
    }

}