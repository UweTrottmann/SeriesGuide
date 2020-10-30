package com.battlelancer.seriesguide.ui.people

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.enumerations.ExternalSource
import com.uwetrottmann.tmdb2.services.FindService
import com.uwetrottmann.tmdb2.services.TvService
import dagger.Lazy
import timber.log.Timber
import javax.inject.Inject

/**
 * Create a show credit [android.support.v4.content.Loader]. Supports show ids from TVDb
 * or TMDb.
 *
 * @param findTmdbId If true, the loader assumes the passed id is from TVDb id and will try to
 */
class ShowCreditsLoader(context: Context, private var showId: Int, private val findTmdbId: Boolean)
    : GenericSimpleLoader<Credits?>(context) {

    @Inject
    lateinit var findService: Lazy<FindService>
    @Inject
    lateinit var tvService: Lazy<TvService>

    init {
        SgApp.getServicesComponent(context).inject(this)
    }

    override fun loadInBackground(): Credits? {
        if (findTmdbId && !findShowTmdbId()) {
            return null // failed to find the show on TMDb
        }

        if (showId < 0) {
            return null // do not have a valid id, abort
        }

        // get credits for that show
        try {
            val response = tvService.get().credits(showId, null).execute()
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

    private fun findShowTmdbId(): Boolean {
        try {
            val response = findService.get()
                    .find(showId.toString(), ExternalSource.TVDB_ID, null)
                    .execute()
            if (response.isSuccessful) {
                val tvResults = response.body()?.tv_results
                if (!tvResults.isNullOrEmpty()) {
                    val showId = tvResults[0].id
                    showId?.let {
                        this.showId = showId
                        return true // found it!
                    }
                }
                Timber.d("Downloading show credits failed: show not on TMDb")
            } else {
                Errors.logAndReport("find tvdb show", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("find tvdb show", e)
        }

        return false
    }
}
