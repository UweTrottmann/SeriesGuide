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
import timber.log.Timber

/**
 * Helps load a fixed number of people into a static layout.
 */
class PeopleListHelper {

    private val personImageTransform = CircleTransformation()

    /**
     * @see populateCredits
     */
    fun populateShowCast(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?
    ): Boolean {
        return populateCredits(
            context,
            peopleContainer,
            credits?.cast,
            credits?.tmdbId,
            PeopleActivity.MediaType.SHOW,
            PeopleType.CAST
        )
    }

    /**
     * @see populateCredits
     */
    fun populateShowCrew(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?
    ): Boolean {
        return populateCredits(
            context,
            peopleContainer,
            credits?.crew,
            credits?.tmdbId,
            PeopleActivity.MediaType.SHOW,
            PeopleType.CREW
        )
    }

    /**
     * @see populateCredits
     */
    fun populateMovieCast(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?
    ): Boolean {
        return populateCredits(
            context,
            peopleContainer,
            credits?.cast,
            credits?.tmdbId,
            PeopleActivity.MediaType.MOVIE,
            PeopleType.CAST
        )
    }

    /**
     * @see populateCredits
     */
    fun populateMovieCrew(
        context: Context,
        peopleContainer: ViewGroup?,
        credits: Credits?
    ): Boolean {
        return populateCredits(
            context,
            peopleContainer,
            credits?.crew,
            credits?.tmdbId,
            PeopleActivity.MediaType.MOVIE,
            PeopleType.CREW
        )
    }

    /**
     * Add views for three (or few more if of interest) people to the given
     * [android.view.ViewGroup] and a show all link if there are more.
     *
     * @return `false` if no views were added to the [peopleContainer].
     */
    private fun populateCredits(
        context: Context,
        peopleContainer: ViewGroup?,
        personList: List<Person>?,
        itemTmdbId: Int?,
        mediaType: PeopleActivity.MediaType,
        peopleType: PeopleType
    ): Boolean {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCredits: container reference gone, aborting")
            return false
        }
        if (itemTmdbId == null) return false
        if (personList.isNullOrEmpty()) return false

        peopleContainer.removeAllViews()

        val inflater = LayoutInflater.from(peopleContainer.context)
        var added = 0
        for (person in personList) {
            if (added == 3) {
                break // Show at most 3
            }

            val personView = createPersonView(
                context,
                inflater,
                peopleContainer,
                person.name,
                person.description,
                person.profilePath
            )
            personView.setOnClickListener(
                OnPersonClickListener(
                    context,
                    mediaType,
                    itemTmdbId,
                    peopleType,
                    person.tmdbId
                )
            )
            peopleContainer.addView(personView)
            added++
        }

        if (personList.size > added) {
            addShowAllView(
                inflater, peopleContainer,
                OnPersonClickListener(context, mediaType, itemTmdbId, peopleType)
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
