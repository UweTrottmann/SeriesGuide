package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.SgMovieFlags
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.Utils

internal open class MovieClickListener(val context: Context) : MoviesAdapter.ItemClickListener {

    override fun onClickMovie(movieTmdbId: Int, posterView: ImageView) {
        if (movieTmdbId == -1) return

        // launch details activity
        val intent = MovieDetailsActivity.intentMovie(context, movieTmdbId)
        Utils.startActivityWithAnimation(context, intent, posterView)
    }

    override fun onClickMovieMoreOptions(movieTmdbId: Int, anchor: View) {
        if (movieTmdbId == -1) return

        // check if movie is already in database (watchlist, collection or watched)
        val movieFlags =
            SgRoomDatabase.getInstance(context).movieHelper().getMovieFlags(movieTmdbId)
                ?: SgMovieFlags() // all flags false

        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.inflate(R.menu.movies_popup_menu)
        popupMenu.menu.apply {
            findItem(R.id.menu_action_movies_set_watched).isVisible = !movieFlags.watched
            findItem(R.id.menu_action_movies_set_unwatched).isVisible = movieFlags.watched
            findItem(R.id.menu_action_movies_watchlist_add).isVisible = !movieFlags.inWatchlist
            findItem(R.id.menu_action_movies_watchlist_remove).isVisible = movieFlags.inWatchlist
            findItem(R.id.menu_action_movies_collection_add).isVisible = !movieFlags.inCollection
            findItem(R.id.menu_action_movies_collection_remove).isVisible = movieFlags.inCollection
        }
        popupMenu.setOnMenuItemClickListener { item ->
            return@setOnMenuItemClickListener when (item.itemId) {
                R.id.menu_action_movies_set_watched -> {
                    MovieTools.watchedMovie(context, movieTmdbId, movieFlags.inWatchlist)
                    true
                }
                R.id.menu_action_movies_set_unwatched -> {
                    MovieTools.unwatchedMovie(context, movieTmdbId)
                    true
                }
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
