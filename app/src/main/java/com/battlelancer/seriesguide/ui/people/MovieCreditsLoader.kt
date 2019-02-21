package com.battlelancer.seriesguide.ui.people

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.tmdb2.entities.Credits

/**
 * Loads movie credits from TMDb.
 */
class MovieCreditsLoader(context: Context, private val tmdbId: Int)
    : GenericSimpleLoader<Credits?>(context) {

    override fun loadInBackground(): Credits? {
        val moviesService = SgApp.getServicesComponent(context).moviesService()
        try {
            val response = moviesService.credits(tmdbId).execute()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport("get movie credits", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get movie credits", e)
        }

        return null
    }
}
