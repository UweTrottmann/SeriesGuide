// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2023 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.base

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.battlelancer.seriesguide.movies.MovieClickListenerImpl
import com.battlelancer.seriesguide.movies.MovieViewHolder
import com.battlelancer.seriesguide.movies.UiMovie

/**
 * Binds a list of [UiMovie] to [MovieViewHolder].
 */
class BaseMovieListAdapter(
    context: Context
) : ListAdapter<UiMovie, MovieViewHolder>(
    UiMovie.DIFF_CALLBACK
) {

    private val itemClickListener = MovieClickListenerImpl(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        return MovieViewHolder.inflate(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

}