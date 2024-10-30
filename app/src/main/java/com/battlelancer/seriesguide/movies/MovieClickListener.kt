// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.view.View
import android.widget.ImageView

/**
 * Use with adapters for a list of movies.
 */
interface MovieClickListener {
    fun onMovieClick(movieTmdbId: Int, posterView: ImageView)

    fun onMoreOptionsClick(movieTmdbId: Int, anchor: View)
}