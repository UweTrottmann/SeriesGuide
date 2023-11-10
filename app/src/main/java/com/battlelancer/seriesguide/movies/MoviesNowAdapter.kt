// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.movies

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.shows.history.HistoryViewHolder
import com.battlelancer.seriesguide.shows.history.NowAdapter

/**
 * An adapted version of [NowAdapter] with a special layout for movies.
 */
internal class MoviesNowAdapter(context: Context, listener: ItemClickListener) :
    NowAdapter(context, listener) {

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (viewHolder is HistoryViewHolder) {
            val item = getItem(position)
            viewHolder.bindToMovie(context, item, drawableWatched, drawableCheckin)
        } else {
            super.onBindViewHolder(viewHolder, position)
        }
    }
}