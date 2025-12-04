// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2019 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter

/**
 * Binds pages of [UiMovie] to [MovieViewHolder].
 */
class MoviesAdapter(
    val itemClickListener: MovieClickListener
) : PagingDataAdapter<UiMovie, MovieViewHolder>(
    UiMovie.DIFF_CALLBACK
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        return MovieViewHolder.inflate(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        // Note that "movie" is a placeholder if it's null.
        holder.bindTo(getItem(position))
    }
}