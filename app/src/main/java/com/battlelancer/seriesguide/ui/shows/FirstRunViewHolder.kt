package com.battlelancer.seriesguide.ui.shows

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
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