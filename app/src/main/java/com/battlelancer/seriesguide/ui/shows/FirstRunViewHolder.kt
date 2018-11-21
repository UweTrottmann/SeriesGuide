package com.battlelancer.seriesguide.ui.shows

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R

/**
 * [RecyclerView.ViewHolder] holding a [FirstRunView].
 */
class FirstRunViewHolder(itemView: FirstRunView) : RecyclerView.ViewHolder(itemView) {

    companion object {

        fun create(parent: ViewGroup): FirstRunViewHolder {
            val firstRunView = LayoutInflater.from(parent.context).inflate(
                R.layout.item_first_run,
                parent,
                false
            ) as FirstRunView
            return FirstRunViewHolder(firstRunView)
        }

    }

}