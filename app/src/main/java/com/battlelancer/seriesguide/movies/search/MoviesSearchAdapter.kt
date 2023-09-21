package com.battlelancer.seriesguide.movies.search

import android.content.Context
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.movies.MovieClickListener
import com.battlelancer.seriesguide.movies.MovieViewHolder
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.uwetrottmann.tmdb2.entities.BaseMovie

internal class MoviesSearchAdapter(
    private val context: Context,
    private val itemClickListener: MovieClickListener
) : PagingDataAdapter<BaseMovie, RecyclerView.ViewHolder>(MovieViewHolder.DIFF_CALLBACK_BASE_MOVIE) {

    private val dateFormatMovieReleaseDate = MovieTools.getMovieShortDateFormat()
    private val posterBaseUrl = TmdbSettings.getPosterBaseUrl(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MovieViewHolder.inflate(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MovieViewHolder).bindTo(
            getItem(position), context, dateFormatMovieReleaseDate, posterBaseUrl
        )
    }

}