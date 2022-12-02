package com.battlelancer.seriesguide.people

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityPeopleBinding
import com.battlelancer.seriesguide.people.PeopleFragment.OnShowPersonListener
import com.battlelancer.seriesguide.ui.BaseActivity

/**
 * Displays a list of people, and on wide enough screens person details.
 * Otherwise lets [PersonActivity] show details.
 */
class PeopleActivity : BaseActivity(), OnShowPersonListener {

    private var isTwoPane = false

    internal interface InitBundle {
        companion object {
            const val MEDIA_TYPE = "media_title"
            const val ITEM_TMDB_ID = "item_tmdb_id"
            const val PEOPLE_TYPE = "people_type"
        }
    }

    enum class MediaType(private val value: String) {
        SHOW("SHOW"),
        MOVIE("MOVIE");

        override fun toString(): String = value
    }

    internal enum class PeopleType(private val value: String) {
        CAST("CAST"),
        CREW("CREW");

        override fun toString(): String = value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPeopleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar()

        // If there is a pane shadow, this is using a two pane layout.
        isTwoPane = binding.viewPeopleShadowStart != null

        val peopleFragment: PeopleFragment
        if (savedInstanceState == null) {
            // Check if this should directly show a person.
            val personTmdbId = intent.getIntExtra(PersonFragment.ARG_PERSON_TMDB_ID, -1)
            if (personTmdbId != -1) {
                showPerson(personTmdbId)

                // If this is not a dual pane layout, allow to go back directly by removing
                // this from back stack.
                if (!isTwoPane) {
                    finish()
                    return
                }
            }

            peopleFragment = PeopleFragment()
            peopleFragment.arguments = intent.extras
            supportFragmentManager.beginTransaction()
                .add(R.id.containerPeople, peopleFragment, "people-list")
                .commit()
        } else {
            peopleFragment =supportFragmentManager.findFragmentById(R.id.containerPeople) as PeopleFragment
        }
        // In two-pane mode, list items should be activated when touched.
        if (isTwoPane) {
            peopleFragment.setActivateOnItemClick()
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        val peopleType = PeopleType.valueOf(
            intent.getStringExtra(InitBundle.PEOPLE_TYPE)!!
        )
        setTitle(if (peopleType == PeopleType.CAST) R.string.movie_cast else R.string.movie_crew)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(
                if (peopleType == PeopleType.CAST) R.string.movie_cast else R.string.movie_crew
            )
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

    override fun showPerson(tmdbId: Int) {
        if (isTwoPane) {
            // show inline
            val f = PersonFragment.newInstance(tmdbId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.containerPeoplePerson, f)
                .commit()
        } else {
            // start new activity
            val i = Intent(this, PersonActivity::class.java)
            i.putExtra(PersonFragment.ARG_PERSON_TMDB_ID, tmdbId)
            startActivity(i)
        }
    }
}