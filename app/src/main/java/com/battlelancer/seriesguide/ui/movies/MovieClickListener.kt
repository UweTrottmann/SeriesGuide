package com.battlelancer.seriesguide.ui.movies

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.Utils

internal open class MovieClickListener(val context: Context) : MoviesAdapter.ItemClickListener {

    override fun onClickMovie(movieTmdbId: Int, posterView: ImageView) {
        if (movieTmdbId == -1) return

        // launch details activity
        val intent = MovieDetailsActivity.intentMovie(context, movieTmdbId)
        Utils.startActivityWithAnimation(context, intent, posterView)
    }

    @SuppressLint("Recycle") // check can't handle ?:
    override fun onClickMovieMoreOptions(movieTmdbId: Int, anchor: View) {
        if (movieTmdbId == -1) return

        // check if movie is already in watchlist or collection
        var isInWatchlist = false
        var isInCollection = false
        val movie: Cursor? = context.contentResolver.query(
            SeriesGuideContract.Movies.buildMovieUri(movieTmdbId),
            arrayOf(
                SeriesGuideContract.Movies.IN_WATCHLIST,
                SeriesGuideContract.Movies.IN_COLLECTION
            ), null, null, null
        )
        if (movie?.moveToFirst() == true) {
            isInWatchlist = movie.getInt(0) == 1
            isInCollection = movie.getInt(1) == 1
            movie.close()
        }

        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.inflate(R.menu.movies_popup_menu)
        popupMenu.menu.apply {
            findItem(R.id.menu_action_movies_watchlist_add).isVisible = !isInWatchlist
            findItem(R.id.menu_action_movies_watchlist_remove).isVisible = isInWatchlist
            findItem(R.id.menu_action_movies_collection_add).isVisible = !isInCollection
            findItem(R.id.menu_action_movies_collection_remove).isVisible = isInCollection
        }
        popupMenu.setOnMenuItemClickListener { item ->
            return@setOnMenuItemClickListener when (item.itemId) {
                R.id.menu_action_movies_watchlist_add -> {
                    MovieTools.addToWatchlist(context, movieTmdbId)
                    true
                }
                R.id.menu_action_movies_watchlist_remove -> {
                    MovieTools.removeFromWatchlist(context, movieTmdbId)
                    true
                }
                R.id.menu_action_movies_collection_add -> {
                    MovieTools.addToCollection(context, movieTmdbId)
                    true
                }
                R.id.menu_action_movies_collection_remove -> {
                    MovieTools.removeFromCollection(context, movieTmdbId)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
}
