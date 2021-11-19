package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.uwetrottmann.tmdb2.entities.BaseMovie

internal class MoviesSearchAdapter(
    private val context: Context,
    private val itemClickListener: MovieClickListener
) : PagingDataAdapter<BaseMovie, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

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

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BaseMovie>() {
            override fun areItemsTheSame(oldItem: BaseMovie, newItem: BaseMovie) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BaseMovie, newItem: BaseMovie): Boolean =
                MovieViewHolder.areContentsTheSame(oldItem, newItem)
        }
    }

}