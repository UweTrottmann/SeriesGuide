package com.battlelancer.seriesguide.shows

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.shows.FirstRunView.FirstRunClickListener

/**
 * [RecyclerView.ViewHolder] holding a [FirstRunView].
 */
class FirstRunViewHolder(itemView: FirstRunView) : RecyclerView.ViewHolder(itemView) {

    fun bind() {
        (itemView as FirstRunView).bind()
    }

    companion object {

        fun create(parent: ViewGroup, firstRunClickListener: FirstRunClickListener): FirstRunViewHolder {
            val firstRunView = LayoutInflater.from(parent.context).inflate(
                R.layout.item_first_run,
                parent,
                false
            ) as FirstRunView
            firstRunView.clickListener = firstRunClickListener
            return FirstRunViewHolder(firstRunView)
        }

    }

}