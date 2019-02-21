package com.battlelancer.seriesguide.ui.people

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.tmdb2.entities.Person
import retrofit2.Response

/**
 * Loads details of a crew or cast member from TMDb.
 */
internal class PersonLoader(context: Context, private val tmdbId: Int) :
        GenericSimpleLoader<Person?>(context) {

    override fun loadInBackground(): Person? {
        val peopleService = SgApp.getServicesComponent(context).peopleService()
        val response: Response<Person?>
        try {
            response = peopleService.summary(tmdbId, "en-US").execute()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport("get person summary", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get person summary", e)
        }

        return null
    }
}
