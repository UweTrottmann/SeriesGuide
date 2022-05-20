package com.battlelancer.seriesguide.ui

import android.content.Context
import android.content.Intent
import com.battlelancer.seriesguide.shows.ShowsActivityImpl

/**
 * Shell to avoid breaking AndroidManifest.xml and shortcuts.xml and other references to this name.
 * Implementation moved to feature-specific package.
 */
class ShowsActivity : ShowsActivityImpl() {

    companion object {
        fun newIntent(context: Context, showsTabIndex: Int): Intent {
            return Intent(context, ShowsActivity::class.java)
                .putExtra(EXTRA_SELECTED_TAB, showsTabIndex)
        }
    }

}