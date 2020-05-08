package com.battlelancer.seriesguide.ui.movies

import android.view.View
import android.widget.ImageView

/**
 * Use with adapters for a list of movies.
 */
interface MovieClickListener {
    fun onClickMovie(movieTmdbId: Int, posterView: ImageView)

    fun onClickMovieMoreOptions(movieTmdbId: Int, anchor: View)
}