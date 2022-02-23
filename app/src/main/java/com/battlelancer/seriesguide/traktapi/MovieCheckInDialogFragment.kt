package com.battlelancer.seriesguide.traktapi

import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.util.safeShow

/**
 * Allows to check into a movie. Launching activities should subscribe
 * to [TraktTask.TraktActionCompleteEvent] to display status toasts.
 */
class MovieCheckInDialogFragment : GenericCheckInDialogFragment() {

    override fun checkInTrakt(message: String) {
        TraktTask(context).checkInMovie(
            requireArguments().getInt(ARG_MOVIE_TMDB_ID),
            requireArguments().getString(ARG_ITEM_TITLE),
            message
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    companion object {
        fun show(fragmentManager: FragmentManager, movieTmdbId: Int, movieTitle: String): Boolean {
            val f = MovieCheckInDialogFragment()
            val args = Bundle()
            args.putString(ARG_ITEM_TITLE, movieTitle)
            args.putInt(ARG_MOVIE_TMDB_ID, movieTmdbId)
            f.arguments = args
            return f.safeShow(fragmentManager, "movieCheckInDialog")
        }
    }
}