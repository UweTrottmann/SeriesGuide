package com.battlelancer.seriesguide.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * Sets the user defined theme, supports setting up a toolbar as the action bar,
 * enables up navigation.
 */
abstract class BaseThemeActivity : AppCompatActivity() {

    protected open fun getCustomTheme(): Int {
        // Set a theme based on user preference.
        return SeriesGuidePreferences.THEME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.setTheme(this, getCustomTheme())
        super.onCreate(savedInstanceState)
    }

    /**
     * Call this in [onCreate] after [setContentView] to use a toolbar with id sgToolbar
     * as the action bar.
     *
     * If setting a title, might also want to supply a title to the
     * activity with [setTitle] for better accessibility.
     */
    protected open fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.sgToolbar)
        setSupportActionBar(toolbar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val upIntent = NavUtils.getParentActivityIntent(this)!!
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                        // Add all of this activity's parents to the back stack
                        .addNextIntentWithParentStack(upIntent)
                        // Navigate up to the closest parent
                        .startActivities()
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}