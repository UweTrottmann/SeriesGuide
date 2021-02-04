package com.battlelancer.seriesguide.ui.people

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.tmdb2.entities.Credits
import timber.log.Timber

/**
 * Create a show credit [android.support.v4.content.Loader]. Supports show ids from TVDb
 * or TMDb.
 *
 * @param findTmdbId If true, the loader assumes the passed id is from TVDb id and will try to
 */
@Deprecated("Use TmdbTools2 instead.")
class ShowCreditsLoader(
    context: Context,
    private var showId: Int,
    private val findTmdbId: Boolean
) : GenericSimpleLoader<Credits?>(context) {

    override fun loadInBackground(): Credits? {
        if (findTmdbId) {
            val tmdbId = TmdbTools2().findShowTmdbId(context, showId)
            showId = if (tmdbId != null && tmdbId > 0) {
                tmdbId
            } else {
                Timber.d("Downloading show credits failed: show not on TMDb")
                return null // Failed to find the show on TMDb.
            }
        }

        if (showId <= 0) {
            return null // Do not have a valid id, abort.
        }

        // get credits for that show
        try {
            val response = SgApp.getServicesComponent(context).tmdb().tvService()
                .credits(showId, null)
                .execute()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport("get show credits", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get show credits", e)
        }

        return null
    }

}
