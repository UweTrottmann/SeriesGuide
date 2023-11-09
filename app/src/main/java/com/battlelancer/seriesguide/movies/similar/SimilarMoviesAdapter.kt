package com.battlelancer.seriesguide.movies.similar

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.battlelancer.seriesguide.movies.MovieClickListenerImpl
import com.battlelancer.seriesguide.movies.MovieViewHolder
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.uwetrottmann.tmdb2.entities.BaseMovie

class SimilarMoviesAdapter(
    private val context: Context,
) : ListAdapter<BaseMovie, MovieViewHolder>(
    MovieViewHolder.DIFF_CALLBACK_BASE_MOVIE
) {

    private val dateFormatMovieReleaseDate = MovieTools.getMovieShortDateFormat()
    private val posterBaseUrl = TmdbSettings.getPosterBaseUrl(context)
    private val itemClickListener = MovieClickListenerImpl(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        return MovieViewHolder.inflate(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bindTo(getItem(position), context, dateFormatMovieReleaseDate, posterBaseUrl)
    }

}