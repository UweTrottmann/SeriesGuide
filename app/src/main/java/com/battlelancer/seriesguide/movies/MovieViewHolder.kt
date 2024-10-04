// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemMovieBinding
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ViewTools.setContextAndLongClickListener
import com.squareup.picasso.Picasso
import com.uwetrottmann.tmdb2.entities.BaseMovie
import java.text.DateFormat

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
        moreOptions.setOnClickListener {
            onMoreOptionsClick()
        }
    }

    private fun onMoreOptionsClick() {
        itemClickListener?.onMoreOptionsClick(movieTmdbId, moreOptions)
    }

    @SuppressLint("SetTextI18n")
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
            if (sgMovie.releasedMsOrDefault != Long.MAX_VALUE) {
                date.text = dateFormatMovieReleaseDate.format(sgMovie.releasedMsOrDefault)
            } else {
                date.text = ""
            }

            // poster
            // use fixed size so bitmaps can be re-used on config change
            val context = itemView.context.applicationContext
            ImageTools.loadWithPicasso(context, posterBaseUrl + sgMovie.poster)
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(poster)
        }
    }

    @SuppressLint("SetTextI18n")
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
            ImageTools.loadWithPicasso(context, posterUrl)
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(poster)
        } else {
            Picasso.get().cancelRequest(poster)
        }
    }

    companion object {

        val DIFF_CALLBACK_BASE_MOVIE = object : DiffUtil.ItemCallback<BaseMovie>() {
            override fun areItemsTheSame(oldItem: BaseMovie, newItem: BaseMovie): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BaseMovie, newItem: BaseMovie): Boolean =
                oldItem.title == newItem.title
                        && oldItem.release_date == newItem.release_date
                        && oldItem.poster_path == newItem.poster_path
        }

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
