// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.shows.history.HistoryViewHolder
import com.battlelancer.seriesguide.shows.history.ShowsHistoryAdapter

/**
 * An adapted version of [ShowsHistoryAdapter] with a special layout for movies.
 */
internal class MoviesHistoryAdapter(context: Context, listener: ItemClickListener) :
    ShowsHistoryAdapter(context, listener) {

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (viewHolder is HistoryViewHolder) {
            val item = getItem(position)
            viewHolder.bindToMovie(context, item, drawableWatched, drawableCheckin)
        } else {
            super.onBindViewHolder(viewHolder, position)
        }
    }
}