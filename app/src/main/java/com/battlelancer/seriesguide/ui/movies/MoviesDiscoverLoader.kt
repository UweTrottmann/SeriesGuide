package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.tmdb2.enumerations.ReleaseType
import java.util.Calendar
import java.util.Date

/**
 * Loads a list of movies from TMDb.
 *
 * If a query is given, will load search results for that query. Otherwise will load a list of
 * movies based on the given link.
 */
class MoviesDiscoverLoader(context: Context) :
    GenericSimpleLoader<MoviesDiscoverLoader.Result>(context) {

    class Result(
        /** If loading failed, is null. Empty if no results.  */
        var results: List<BaseMovie>?,
        var emptyText: String
    )

    private val tmdb: Tmdb = SgApp.getServicesComponent(context).tmdb()

    private val dateNow: TmdbDate
        get() = TmdbDate(Date())

    private val dateOneMonthAgo: TmdbDate
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -30)
            return TmdbDate(calendar.time)
        }

    override fun loadInBackground(): Result {
        val languageCode = DisplaySettings.getMoviesLanguage(context)
        val regionCode = DisplaySettings.getMoviesRegion(context)

        val action = "get now playing movies"
        val call = tmdb.discoverMovie()
            .with_release_type(
                DiscoverFilter(
                    DiscoverFilter.Separator.OR,
                    ReleaseType.THEATRICAL, ReleaseType.THEATRICAL_LIMITED
                )
            )
            .release_date_lte(dateNow)
            .release_date_gte(dateOneMonthAgo)
            .language(languageCode)
            .region(regionCode)
            .build()

        val response = try {
            call.execute()
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            // Not checking for connection until here to allow hitting the response cache.
            return if (AndroidUtils.isNetworkConnected(context)) {
                buildErrorResult()
            } else {
                Result(null, context.getString(R.string.offline))
            }
        }

        return if (response.isSuccessful) {
            // Filter null items (a few users affected).
            Result(
                response.body()?.results?.filterNotNull(),
                context.getString(R.string.no_results)
            )
        } else {
            Errors.logAndReport(action, response)
            buildErrorResult()
        }
    }

    private fun buildErrorResult(): Result = Result(
        null, context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
    )
}
