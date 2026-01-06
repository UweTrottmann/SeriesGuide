// Apache-2.0
// Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.people

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.util.CircleTransformation
import com.battlelancer.seriesguide.util.ImageTools
import com.squareup.picasso.Transformation

interface PeopleAdapterHost {
    fun searchResultIsEmpty(isEmpty: Boolean)
}

class PeopleAdapter(
    private val host: PeopleAdapterHost,
    private val placeholderDrawable: Drawable?
) : BaseAdapter() {

    private val personImageTransform = CircleTransformation()

    private var originalData = emptyList<Person>()
    private var data = emptyList<Person>()

    fun setData(data: List<Person>) {
        originalData = data
        this.data = originalData
        notifyDataSetChanged()
    }

    override fun getCount() = data.count()

    override fun getItem(position: Int) = data.getOrNull(position)

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view: View
        val viewHolder: ViewHolder
        val context = parent?.context ?: throw IllegalStateException("Context missing")

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_person, parent, false)
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

    class ViewHolder(
        view: View,
        private val placeholderDrawable: Drawable?
    ) {
        private val name: TextView = view.findViewById(R.id.textViewPerson)
        private val description: TextView = view.findViewById(R.id.textViewPersonDescription)
        private val picture: ImageView = view.findViewById(R.id.imageViewPerson)

        fun bind(context: Context, personImageTransform: Transformation, person: Person?) {
            // name and description
            name.text = person?.name ?: ""
            description.text = person?.description ?: ""

            // load profile picture
            if (person != null && placeholderDrawable != null) {
                ImageTools.loadWithPicasso(
                    context, TmdbTools.buildProfileImageUrl(
                        context, person.profilePath, TmdbTools.ProfileImageSize.W185
                    )
                )
                    // Note: dimensions should match placeholder drawable, see notes in its file
                    .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                    .centerCrop().transform(personImageTransform)
                    .placeholder(placeholderDrawable)
                    .error(placeholderDrawable).into(picture)
            }
        }
    }

    fun filter(query: String) {
        data = originalData.filter { person ->
            if (person.description != null) {
                person.name.contains(query, ignoreCase = true) ||
                        person.description.contains(query, ignoreCase = true)
            } else {
                person.name.contains(query, ignoreCase = true)
            }
        }
        host.searchResultIsEmpty(data.isEmpty())
        notifyDataSetChanged()
    }
}
