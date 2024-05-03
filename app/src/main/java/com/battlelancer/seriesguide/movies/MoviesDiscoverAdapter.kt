// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2024 Uwe Trottmann
package com.battlelancer.seriesguide.movies

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.movies.MovieViewHolder.Companion.inflate
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.uwetrottmann.tmdb2.entities.BaseMovie
import java.text.DateFormat

/**
 * [RecyclerView.Adapter] that displays a number of [links] and after a header
 * a small number of [movies].
 */
class MoviesDiscoverAdapter(
    private val context: Context,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface ItemClickListener : MovieClickListener {
        fun onClickLink(link: MoviesDiscoverLink, anchor: View)
    }

    private val dateFormatMovieReleaseDate: DateFormat = MovieTools.getMovieShortDateFormat()
    private val posterBaseUrl = TmdbSettings.getPosterBaseUrl(context)
    private val movies: MutableList<BaseMovie> = ArrayList()

    private val links: List<MoviesDiscoverLink> = listOf(
        MoviesDiscoverLink.POPULAR,
        MoviesDiscoverLink.DIGITAL,
        MoviesDiscoverLink.DISC,
        MoviesDiscoverLink.UPCOMING
    )

    override fun getItemViewType(position: Int): Int {
        val linksCount = links.size
        if (position < linksCount) {
            return VIEW_TYPE_LINK
        }
        if (position == positionHeader()) {
            return VIEW_TYPE_HEADER
        }
        return VIEW_TYPE_MOVIE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_LINK) {
            return LinkViewHolder.inflate(parent, itemClickListener)
        }
        if (viewType == VIEW_TYPE_HEADER) {
            return HeaderViewHolder.inflate(parent)
        }
        if (viewType == VIEW_TYPE_MOVIE) {
            return inflate(parent, itemClickListener)
        }
        throw IllegalArgumentException("Unknown view type $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LinkViewHolder -> {
                val link = getLink(position)
                holder.link = link
                holder.title.setText(link.titleRes)
            }

            is HeaderViewHolder -> {
                holder.header.setText(DISCOVER_LINK_DEFAULT.titleRes)
            }

            is MovieViewHolder -> {
                val movie = getMovie(position)
                holder.bindTo(movie, context, dateFormatMovieReleaseDate, posterBaseUrl)
            }
        }
    }

    // No need for incremental updates/animations
    @SuppressLint("NotifyDataSetChanged")
    fun updateMovies(newMovies: List<BaseMovie>?) {
        movies.clear()
        if (newMovies != null) {
            movies.addAll(newMovies)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = links.size + 1 /* header */ + movies.size

    private fun getLink(position: Int): MoviesDiscoverLink = links[position]

    private fun getMovie(position: Int): BaseMovie = movies[position - links.size - 1]

    private fun positionHeader(): Int = links.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val header: TextView = itemView.findViewById(R.id.textViewDiscoverHeader)

        companion object {
            fun inflate(parent: ViewGroup): HeaderViewHolder {
                return HeaderViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_discover_movies_header, parent, false)
                )
            }
        }
    }

    class LinkViewHolder private constructor(
        itemView: View,
        itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(itemView) {

        var link: MoviesDiscoverLink? = null
        val title: TextView = itemView.findViewById(R.id.textViewDiscoverLink)

        init {
            itemView.setOnClickListener {
                link?.let {
                    itemClickListener.onClickLink(it, this@LinkViewHolder.itemView)
                }
            }
        }

        companion object {
            fun inflate(
                parent: ViewGroup,
                itemClickListener: ItemClickListener
            ): LinkViewHolder {
                return LinkViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_discover_movies_link, parent, false),
                    itemClickListener
                )
            }
        }
    }

    companion object {
        val VIEW_TYPE_LINK: Int = R.layout.item_discover_movies_link
        val VIEW_TYPE_HEADER: Int = R.layout.item_discover_movies_header
        val VIEW_TYPE_MOVIE: Int = R.layout.item_movie

        val DISCOVER_LINK_DEFAULT: MoviesDiscoverLink = MoviesDiscoverLink.IN_THEATERS
    }
}
