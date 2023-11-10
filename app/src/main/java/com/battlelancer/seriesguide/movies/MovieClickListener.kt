// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.movies

import android.view.View
import android.widget.ImageView

/**
 * Use with adapters for a list of movies.
 */
interface MovieClickListener {
    fun onClickMovie(movieTmdbId: Int, posterView: ImageView)

    fun onClickMovieMoreOptions(movieTmdbId: Int, anchor: View)
}