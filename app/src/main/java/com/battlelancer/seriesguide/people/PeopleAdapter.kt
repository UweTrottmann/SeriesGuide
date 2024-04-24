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

/**
 * Shows a list of people in rows with headshots, name and description.
 */
internal class PeopleAdapter(context: Context) : ArrayAdapter<Person?>(context, LAYOUT) {

    private val personImageTransform = CircleTransformation()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(parent.context)
                .inflate(LAYOUT, parent, false)

            viewHolder = ViewHolder()
            viewHolder.name = view.findViewById(R.id.textViewPerson)
            viewHolder.description = view.findViewById(
                R.id.textViewPersonDescription
            )
            viewHolder.headshot = view.findViewById(R.id.imageViewPerson)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as ViewHolder
        }

        val person = getItem(position) ?: return view

        // name and description
        viewHolder.name!!.text = person.name
        viewHolder.description!!.text = person.description

        // load headshot
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
            .into(viewHolder.headshot)

        return view
    }

    /**
     * Replace the data in this [android.widget.ArrayAdapter] with the given list.
     */
    fun setData(data: List<Person?>?) {
        clear()
        data?.let { addAll(it) }
    }

    internal class ViewHolder {
        var name: TextView? = null
        var description: TextView? = null
        var headshot: ImageView? = null
    }

    companion object {
        private val LAYOUT = R.layout.item_person
    }
}
