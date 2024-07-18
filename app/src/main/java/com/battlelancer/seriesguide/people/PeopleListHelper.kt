// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2024 Uwe Trottmann

package com.battlelancer.seriesguide.people

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.people.PeopleActivity.PeopleType
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.util.CircleTransformation
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.CrewMember
import timber.log.Timber

/**
 * Helps load a fixed number of people into a static layout.
 */
class PeopleListHelper {

    private val personImageTransform = CircleTransformation()

    /**
     * @see populateCast
     */
    fun populateShowCast(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?
    ): Boolean {
        return populateCast(context, peopleContainer, credits, PeopleActivity.MediaType.SHOW)
    }

    /**
     * @see populateCrew
     */
    fun populateShowCrew(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?
    ): Boolean {
        return populateCrew(context, peopleContainer, credits, PeopleActivity.MediaType.SHOW)
    }

    /**
     * @see populateCast
     */
    fun populateMovieCast(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?
    ): Boolean {
        return populateCast(context, peopleContainer, credits, PeopleActivity.MediaType.MOVIE)
    }

    /**
     * @see populateCrew
     */
    fun populateMovieCrew(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?
    ): Boolean {
        return populateCrew(context, peopleContainer, credits, PeopleActivity.MediaType.MOVIE)
    }

    /**
     * Add views for at most three cast members to the given [android.view.ViewGroup] and a
     * "Show all" link if there are more.
     *
     * @return `false` if no views were added to the [peopleContainer].
     */
    private fun populateCast(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?,
        mediaType: PeopleActivity.MediaType
    ): Boolean {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCast: container reference gone, aborting")
            return false
        }
        if (credits == null) return false
        val itemTmdbId = credits.id ?: return false
        val cast = credits.cast
        if (cast.isNullOrEmpty()) return false

        peopleContainer.removeAllViews()
        val inflater = LayoutInflater.from(peopleContainer.context)
        var added = 0
        for (person in cast) {
            if (added == 3) {
                break // show at most 3
            }
            val personId = person.id
                ?: continue // missing required values
            val name = person.name
                ?: continue // missing required values

            val personView = createPersonView(
                context,
                inflater,
                peopleContainer,
                name,
                person.character,
                person.profile_path
            )
            personView.setOnClickListener(
                OnPersonClickListener(
                    context,
                    mediaType,
                    itemTmdbId,
                    PeopleType.CAST,
                    personId
                )
            )
            peopleContainer.addView(personView)
            added++
        }
        if (cast.size > 3) {
            addShowAllView(
                inflater, peopleContainer,
                OnPersonClickListener(context, mediaType, itemTmdbId, PeopleType.CAST)
            )
        }
        return added > 0
    }

    private fun CrewMember.shouldInclude(mediaType: PeopleActivity.MediaType): Boolean {
        return when (mediaType) {
            PeopleActivity.MediaType.SHOW -> job == "Creator"
            PeopleActivity.MediaType.MOVIE -> job == "Director" || department == "Writing"
        }
    }

    /**
     * Add views for three (or few more if of interest) crew members to the given
     * [android.view.ViewGroup] and a show all link if there are more.
     *
     * @return `false` if no views were added to the [peopleContainer].
     */
    private fun populateCrew(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?,
        mediaType: PeopleActivity.MediaType
    ): Boolean {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCrew: container reference gone, aborting")
            return false
        }
        if (credits == null) return false
        val itemTmdbId = credits.id ?: return false
        val crew = credits.crew
        if (crew.isNullOrEmpty()) return false

        peopleContainer.removeAllViews()

        val inflater = LayoutInflater.from(peopleContainer.context)
        var added = 0
        for (person in crew) {
            // Show at most 3, or more if of interest
            if (added >= 3) {
                if (!person.shouldInclude(mediaType)) {
                    break
                }
            }
            val personId = person.id
                ?: continue // missing required values
            val name = person.name
                ?: continue // missing required values

            val personView = createPersonView(
                context,
                inflater,
                peopleContainer,
                name,
                person.job,
                person.profile_path
            )
            personView.setOnClickListener(
                OnPersonClickListener(
                    context,
                    mediaType,
                    itemTmdbId,
                    PeopleType.CREW,
                    personId
                )
            )
            peopleContainer.addView(personView)
            added++
        }

        if (crew.size > added) {
            addShowAllView(
                inflater, peopleContainer,
                OnPersonClickListener(context, mediaType, itemTmdbId, PeopleType.CREW)
            )
        }
        return added > 0
    }

    private fun createPersonView(
        context: Context,
        inflater: LayoutInflater,
        peopleContainer: ViewGroup,
        name: String,
        description: String?,
        profilePath: String?
    ): View {
        val personView = inflater.inflate(R.layout.item_person, peopleContainer, false)
            .apply {
                // use clickable instead of activatable background
                setBackgroundResource(
                    ThemeUtils.resolveAttributeToResourceId(
                        peopleContainer.context.theme,
                        androidx.appcompat.R.attr.selectableItemBackground
                    )
                )
                // support keyboard nav
                isFocusable = true
            }

        ImageTools.loadWithPicasso(
            context,
            TmdbTools.buildProfileImageUrl(context, profilePath, TmdbTools.ProfileImageSize.W185)
        )
            .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
            .centerCrop()
            .transform(personImageTransform)
            .placeholder(R.drawable.ic_account_circle_black_24dp)
            .error(R.drawable.ic_account_circle_black_24dp)
            .into(personView.findViewById<View>(R.id.imageViewPerson) as ImageView)

        personView.findViewById<TextView>(R.id.textViewPerson).text = name
        personView.findViewById<TextView>(R.id.textViewPersonDescription).text = description ?: ""
        return personView
    }

    private fun addShowAllView(
        inflater: LayoutInflater,
        peopleContainer: ViewGroup,
        clickListener: View.OnClickListener
    ) {
        val showAllView =
            inflater.inflate(R.layout.item_action_add, peopleContainer, false) as TextView
        showAllView.setText(R.string.action_display_all)
        showAllView.setOnClickListener(clickListener)
        peopleContainer.addView(showAllView)
    }

    /**
     * Listener that will show cast or crew members for the given TMDb entity.
     *
     * Set [personTmdbId] to pre-select a specific cast or crew member.
     */
    private class OnPersonClickListener(
        private val context: Context,
        private val mediaType: PeopleActivity.MediaType,
        private val itemTmdbId: Int,
        private val peopleType: PeopleType,
        private val personTmdbId: Int = -1
    ) : View.OnClickListener {

        override fun onClick(v: View) {
            val i = Intent(v.context, PeopleActivity::class.java)
            i.putExtra(PeopleActivity.InitBundle.ITEM_TMDB_ID, itemTmdbId)
            i.putExtra(PeopleActivity.InitBundle.PEOPLE_TYPE, peopleType.toString())
            i.putExtra(PeopleActivity.InitBundle.MEDIA_TYPE, mediaType.toString())
            if (personTmdbId != -1) {
                // showing a specific person
                i.putExtra(PersonFragment.ARG_PERSON_TMDB_ID, personTmdbId)
            }
            Utils.startActivityWithAnimation(context, i, v)
        }
    }
}
