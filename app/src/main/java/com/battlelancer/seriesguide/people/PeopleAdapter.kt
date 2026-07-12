// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2014-2025 Uwe Trottmann

package com.battlelancer.seriesguide.people

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
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
    private val placeholderDrawable =
        AppCompatResources.getDrawable(context, R.drawable.ic_account_circle_control_24dp)!!

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(LAYOUT, parent, false)
            viewHolder = ViewHolder(view, placeholderDrawable)
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

    class ViewHolder(
        view: View,
        private val placeholderDrawable: Drawable
    ) {
        private val name: TextView = view.findViewById(R.id.textViewPerson)
        private val description: TextView = view.findViewById(R.id.textViewPersonDescription)
        private val picture: ImageView = view.findViewById(R.id.imageViewPerson)

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
                    // Note: dimensions should match placeholder drawable, see notes in its file
                    .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                    .centerCrop()
                    .transform(personImageTransform)
                    .placeholder(placeholderDrawable)
                    .error(placeholderDrawable)
                    .into(picture)
            }
        }
    }

    companion object {
        private val LAYOUT = R.layout.item_person
    }
}
