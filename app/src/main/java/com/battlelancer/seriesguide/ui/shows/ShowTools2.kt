package com.battlelancer.seriesguide.ui.shows

import android.content.ContentValues
import android.content.Context
import android.widget.Toast
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.Utils
import com.uwetrottmann.seriesguide.backend.shows.model.Show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShowTools2(val showTools: ShowTools, val context: Context) {

    /**
     * Saves new favorite flag to the local database and, if signed in, up into the cloud as well.
     */
    fun storeIsFavorite(showTvdbId: Int, isFavorite: Boolean) {
        GlobalScope.launch {
            storeIsFavoriteAsync(showTvdbId, isFavorite)
        }
    }

    private suspend fun storeIsFavoriteAsync(showTvdbId: Int, isFavorite: Boolean) {
        // send to cloud
        val isCloudButNotConnected = withContext(Dispatchers.Default) {
            if (HexagonSettings.isEnabled(context)) {
                if (Utils.isNotConnected(context)) {
                    return@withContext true
                }
                val show = Show()
                show.tvdbId = showTvdbId
                show.isFavorite = isFavorite
                withContext(Dispatchers.Main) {
                    showTools.uploadShowAsync(show)
                }
            }
            return@withContext false
        }
        if (isCloudButNotConnected) return

        // save to local database
        withContext(Dispatchers.IO) {
            val values = ContentValues()
            values.put(SeriesGuideContract.Shows.FAVORITE, if (isFavorite) 1 else 0)
            context.contentResolver.update(
                SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null
            )

            // also notify URIs used by search and lists
            context.contentResolver
                .notifyChange(SeriesGuideContract.Shows.CONTENT_URI_FILTER, null)
            context.contentResolver
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null)
        }

        // display info toast
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, context.getString(
                    if (isFavorite)
                        R.string.favorited
                    else
                        R.string.unfavorited
                ), Toast.LENGTH_SHORT
            ).show()
        }
    }

}