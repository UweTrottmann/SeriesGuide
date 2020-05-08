package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemDiscoverMovieBinding
import com.battlelancer.seriesguide.model.SgMovie
import com.battlelancer.seriesguide.util.ServiceUtils
import com.squareup.picasso.Picasso
import com.uwetrottmann.tmdb2.entities.BaseMovie
import java.text.DateFormat

internal class MovieViewHolder(
    val binding: ItemDiscoverMovieBinding,
    itemClickListener: MovieClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    private var movieTmdbId: Int = 0

    private val title = binding.includeMovie.textViewMovieTitle
    private val date = binding.includeMovie.textViewMovieDate
    private val poster = binding.includeMovie.imageViewMoviePoster
    private val contextMenu = binding.includeMovie.imageViewMovieItemContextMenu

    init {
        itemView.setOnClickListener {
            itemClickListener?.onClickMovie(movieTmdbId, poster)
        }
        contextMenu.setOnClickListener { v ->
            itemClickListener?.onClickMovieMoreOptions(movieTmdbId, v)
        }
    }

    fun bindTo(sgMovie: SgMovie?, dateFormatMovieReleaseDate: DateFormat, posterBaseUrl: String) {
        if (sgMovie == null) {
            movieTmdbId = -1
            title.text = ""
            date.text = ""
            Picasso.get().cancelRequest(poster)
            poster.setImageDrawable(null)
        } else {
            movieTmdbId = sgMovie.tmdbId
            title.text = sgMovie.title
            if (sgMovie.releasedMs != null) {
                date.text = dateFormatMovieReleaseDate.format(sgMovie.releasedMs)
            } else {
                date.text = ""
            }

            // poster
            // use fixed size so bitmaps can be re-used on config change
            val context = itemView.context.applicationContext
            ServiceUtils.loadWithPicasso(context, posterBaseUrl + sgMovie.poster)
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(poster)
        }
    }

    fun bindTo(
        tmdbMovie: BaseMovie?,
        context: Context,
        dateFormatMovieReleaseDate: DateFormat,
        posterBaseUrl: String
    ) {
        movieTmdbId = tmdbMovie?.id ?: 0
        title.text = tmdbMovie?.title
        val releaseDate = tmdbMovie?.release_date
        if (releaseDate != null) {
            date.text = dateFormatMovieReleaseDate.format(releaseDate)
        } else {
            date.text = ""
        }

        if (tmdbMovie != null) {
            // poster
            // use fixed size so bitmaps can be re-used on config change
            val posterUrl = tmdbMovie.poster_path
                ?.let { posterBaseUrl + it }
            ServiceUtils.loadWithPicasso(context, posterUrl)
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(poster)
        } else {
            Picasso.get().cancelRequest(poster)
        }
    }

    companion object {
        @JvmStatic
        fun inflate(parent: ViewGroup, itemClickListener: MovieClickListener?): MovieViewHolder {
            return MovieViewHolder(
                ItemDiscoverMovieBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                itemClickListener
            )
        }

        fun areContentsTheSame(oldItem: BaseMovie, newItem: BaseMovie): Boolean =
            oldItem.title == newItem.title
                    && oldItem.release_date == newItem.release_date
                    && oldItem.poster_path == newItem.poster_path
    }
}
