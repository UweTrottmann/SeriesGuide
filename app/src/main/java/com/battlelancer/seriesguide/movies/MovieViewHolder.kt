// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2019 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemMovieBinding
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ViewTools.setContextAndLongClickListener
import com.squareup.picasso.Picasso
import com.uwetrottmann.tmdb2.entities.BaseMovie

class MovieViewHolder(
    binding: ItemMovieBinding,
    private val itemClickListener: MovieClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    private var movieTmdbId: Int = 0

    private val title = binding.includeMovie.textViewMovieTitle
    private val date = binding.includeMovie.textViewMovieDate
    private val poster = binding.includeMovie.imageViewMoviePoster
    private val moreOptions = binding.includeMovie.imageViewMovieMoreOptions

    init {
        itemView.setOnClickListener {
            itemClickListener?.onMovieClick(movieTmdbId, poster)
        }
        itemView.setContextAndLongClickListener {
            onMoreOptionsClick()
        }
        moreOptions.also {
            TooltipCompat.setTooltipText(it, it.contentDescription)
            it.setOnClickListener {
                onMoreOptionsClick()
            }
        }
    }

    private fun onMoreOptionsClick() {
        itemClickListener?.onMoreOptionsClick(movieTmdbId, moreOptions)
    }

    fun bindTo(movie: UiMovie?) {
        if (movie == null) {
            movieTmdbId = -1
            title.text = ""
            date.text = ""
            Picasso.get().cancelRequest(poster)
            poster.setImageDrawable(null)
        } else {
            movieTmdbId = movie.tmdbId
            title.text = movie.title
            date.text = movie.date

            // poster
            // use fixed size so bitmaps can be re-used on config change
            ImageTools.loadWithPicasso(
                itemView.context.applicationContext,
                movie.posterUrl
            )
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(poster)
        }
    }

    companion object {

        @JvmStatic
        fun inflate(parent: ViewGroup, itemClickListener: MovieClickListener?): MovieViewHolder {
            return MovieViewHolder(
                ItemMovieBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                itemClickListener
            )
        }

    }
}

data class UiMovie(
    val tmdbId: Int,
    val title: String,
    val date: String,
    val posterUrl: String?
) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UiMovie>() {
            override fun areItemsTheSame(oldItem: UiMovie, newItem: UiMovie): Boolean =
                oldItem.tmdbId == newItem.tmdbId

            // Is a data class so equals compares all properties
            override fun areContentsTheSame(oldItem: UiMovie, newItem: UiMovie): Boolean =
                oldItem == newItem
        }
    }
}

/**
 * Builds [UiMovie] instances, caches things like date format and poster base URL.
 */
class UiMovieBuilder(context: Context) {

    private val dateFormatMovieReleaseDate = MovieTools.getMovieShortDateFormat()
    private val posterBaseUrl = TmdbSettings.getPosterBaseUrl(context)

    fun buildFrom(sgMovie: SgMovie): UiMovie {
        return UiMovie(
            sgMovie.tmdbId,
            sgMovie.title.orEmpty(),
            if (sgMovie.releasedMsOrDefault != Long.MAX_VALUE) {
                dateFormatMovieReleaseDate.format(sgMovie.releasedMsOrDefault)
            } else {
                ""
            },
            buildPosterUrl(sgMovie.poster)
        )
    }

    fun buildFrom(baseMovie: BaseMovie) = UiMovie(
        baseMovie.id!!,
        baseMovie.title.orEmpty(),
        baseMovie.release_date?.let { dateFormatMovieReleaseDate.format(it) }.orEmpty(),
        buildPosterUrl(baseMovie.poster_path)
    )

    private fun buildPosterUrl(posterPath: String?): String? {
        return if (!posterPath.isNullOrEmpty()) {
            "$posterBaseUrl$posterPath"
        } else null
    }
}
