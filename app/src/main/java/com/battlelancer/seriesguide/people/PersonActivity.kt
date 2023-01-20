package com.battlelancer.seriesguide.people

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * Hosts a [PersonFragment], only used on handset devices. On
 * tablet-size devices a two-pane layout inside [PeopleActivity]
 * is used.
 */
class PersonActivity : BaseActivity() {

    override fun configureEdgeToEdge() {
        // Always using a toolbar with dark to transparent gradient background,
        // so always use a status bar for dark backgrounds.
        ThemeUtils.configureEdgeToEdge(window, forceDarkStatusBars = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person)
        ThemeUtils.configureForEdgeToEdge(findViewById(R.id.rootLayoutPerson))
        setupActionBar()

        // fitsSystemWindows="true" also adds padding for the navigation bar,
        // so manually readjust padding for just the status bar.
        val toolbar = findViewById<Toolbar>(R.id.sgToolbar)
        val toolbarPaddingTop = toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePaddingRelative(top = toolbarPaddingTop + statusBarInsets.top)
            insets
        }

        if (savedInstanceState == null) {
            val f = PersonFragment.newInstance(
                intent.getIntExtra(PersonFragment.ARG_PERSON_TMDB_ID, 0)
            )
            supportFragmentManager.beginTransaction()
                .add(R.id.containerPerson, f)
                .commit()
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.also {
            it.setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
