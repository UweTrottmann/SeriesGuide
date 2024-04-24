// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2024 Uwe Trottmann

package com.battlelancer.seriesguide.people

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.util.CircleTransformation
import com.battlelancer.seriesguide.util.ImageTools
import com.squareup.picasso.Transformation

/**
 * Shows a list of people in rows with headshots, name and description.
 */
internal class PeopleAdapter(context: Context) : ArrayAdapter<Person>(context, LAYOUT) {

    private val personImageTransform = CircleTransformation()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(LAYOUT, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as ViewHolder
        }

        val person = getItem(position)
        viewHolder.bind(context, personImageTransform, person)
        return view
    }

    /**
     * Replace the data in this [android.widget.ArrayAdapter] with the given list.
     */
    fun setData(data: List<Person>) {
        clear()
        addAll(data)
    }

    class ViewHolder(view: View) {
        private val name: TextView
        private val description: TextView
        private val picture: ImageView

        init {
            name = view.findViewById(R.id.textViewPerson)
            description = view.findViewById(R.id.textViewPersonDescription)
            picture = view.findViewById(R.id.imageViewPerson)
        }

        fun bind(context: Context, personImageTransform: Transformation, person: Person?) {
            // name and description
            name.text = person?.name ?: ""
            description.text = person?.description ?: ""

            // load profile picture
            if (person != null) {
                ImageTools.loadWithPicasso(
                    context,
                    TmdbTools.buildProfileImageUrl(
                        context, person.profilePath,
                        TmdbTools.ProfileImageSize.W185
                    )
                )
                    .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                    .centerCrop()
                    .transform(personImageTransform)
                    .error(R.drawable.ic_account_circle_black_24dp)
                    .into(picture)
            }
        }
    }

    companion object {
        private val LAYOUT = R.layout.item_person
    }
}
