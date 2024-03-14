// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2020, 2022-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.util.ImageTools
import java.text.DateFormat
import java.util.Date

class MoviesCursorAdapter(
    context: Context,
    private val movieClickListener: MovieClickListenerImpl,
    private val uniqueId: Int
) : CursorAdapter(context, null, 0) {

    private val tmdbImageBaseUrl: String
    private val dateFormatMovieReleaseDate = MovieTools.getMovieShortDateFormat()

    init {
        // figure out which size of posters to load based on screen density
        val baseUrl = TmdbSettings.getImageBaseUrl(context)
        tmdbImageBaseUrl = if (DisplaySettings.isVeryHighDensityScreen(context)) {
            "$baseUrl${TmdbSettings.POSTER_SIZE_SPEC_W342}"
        } else {
            "$baseUrl${TmdbSettings.POSTER_SIZE_SPEC_W154}"
        }
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        // do not use parent layout params to avoid padding issues
        @SuppressLint("InflateParams") val v =
            LayoutInflater.from(parent.context).inflate(R.layout.item_movie, null)
        ViewHolder(v, movieClickListener)
        return v
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val holder = view.tag as ViewHolder
        holder.bind(context, cursor, dateFormatMovieReleaseDate, tmdbImageBaseUrl, uniqueId)
    }

    class ViewHolder(itemView: View, clickListener: MovieClickListenerImpl) {
        var title: TextView
        var releaseDate: TextView
        var poster: ImageView
        var contextMenu: View
        private var movieTmdbId = 0

        init {
            itemView.tag = this

            title = itemView.findViewById(R.id.textViewMovieTitle)
            releaseDate = itemView.findViewById(R.id.textViewMovieDate)
            poster = itemView.findViewById(R.id.imageViewMoviePoster)
            contextMenu = itemView.findViewById(R.id.imageViewMovieItemContextMenu)

            itemView.setOnClickListener {
                clickListener.onClickMovie(movieTmdbId, poster)
            }
            // context menu
            contextMenu.setOnClickListener { v: View? ->
                clickListener.onClickMovieMoreOptions(movieTmdbId, v!!)
            }
        }

        fun bind(
            context: Context, cursor: Cursor, dateFormat: DateFormat,
            tmdbImageBaseUrl: String, uniqueId: Int
        ) {
            movieTmdbId = cursor.getInt(MoviesQuery.TMDB_ID)

            // title
            title.text = cursor.getString(MoviesQuery.TITLE)

            // release date
            val released = cursor.getLong(MoviesQuery.RELEASED_UTC_MS)
            if (released != Long.MAX_VALUE) {
                releaseDate.text = dateFormat.format(Date(released))
            } else {
                releaseDate.text = ""
            }

            // load poster, cache on external storage
            val posterPath = cursor.getString(MoviesQuery.POSTER)
            // use fixed size so bitmaps can be re-used on config change
            ImageTools.loadWithPicasso(
                context,
                if (TextUtils.isEmpty(posterPath)) null else tmdbImageBaseUrl + posterPath
            )
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(poster)

            // set unique transition names
            poster.transitionName = "moviesCursorAdapterPoster_${uniqueId}_$movieTmdbId"
        }
    }

    interface MoviesQuery {
        companion object {
            val PROJECTION = arrayOf(
                Movies._ID,
                Movies.TMDB_ID,
                Movies.TITLE,
                Movies.POSTER,
                Movies.RELEASED_UTC_MS
            )
            const val ID = 0
            const val TMDB_ID = 1
            const val TITLE = 2
            const val POSTER = 3
            const val RELEASED_UTC_MS = 4
        }
    }
}
