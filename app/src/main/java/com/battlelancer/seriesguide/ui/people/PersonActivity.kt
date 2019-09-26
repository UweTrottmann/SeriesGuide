package com.battlelancer.seriesguide.ui.people

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity

/**
 * Hosts a [PersonFragment], only used on handset devices. On
 * tablet-size devices a two-pane layout inside [PeopleActivity]
 * is used.
 */
class PersonActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instead of Window.FEATURE_ACTION_BAR_OVERLAY as indicated by AppCompatDelegate warning.
        supportRequestWindowFeature(AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR_OVERLAY)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person)
        setupActionBar()

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
            it.setBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.background_actionbar_gradient)
            )
            it.setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
