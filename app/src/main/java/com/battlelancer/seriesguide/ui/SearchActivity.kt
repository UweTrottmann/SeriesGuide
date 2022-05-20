package com.battlelancer.seriesguide.ui

import android.content.Context
import android.content.Intent
import com.battlelancer.seriesguide.shows.search.SearchActivityImpl

/**
 * Shell to avoid breaking AndroidManifest.xml and shortcuts.xml and other references to this name.
 * Implementation moved to feature-specific package.
 */
class SearchActivity : SearchActivityImpl() {

    companion object {

        /**
         * Intent which opens activity with search tab active.
         */
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, SearchActivity::class.java)
                .putExtra(EXTRA_DEFAULT_TAB, TAB_POSITION_SEARCH)
        }
    }

}
