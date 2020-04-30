package com.battlelancer.seriesguide.appwidget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.text.format.DateUtils
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.os.bundleOf
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Qualified
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.WidgetSettings
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2.CalendarType
import com.battlelancer.seriesguide.ui.shows.CalendarQuery
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings
import com.battlelancer.seriesguide.util.CalendarUtils
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import timber.log.Timber

/**
 * [RemoteViewsService.RemoteViewsFactory] that supplies the actual item contents and layouts
 * to [ListWidgetService].
 */
class ListWidgetRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var dataCursor: Cursor? = null
    private var widgetType = 0
    private var isLightTheme = false
    private var isLargeFont = false

    override fun onCreate() {
        // Since dataCursor is reloaded in onDataSetChanged()
        // which gets called immediately after onCreate(),
        // there is nothing to do here.
    }

    @SuppressLint("Recycle") // Cursor close check broken for Kotlin.
    private fun onQueryForData() {
        Timber.d("onQueryForData: %d", appWidgetId)

        val widgetType = WidgetSettings.getWidgetListType(context, appWidgetId)
        val isOnlyPremieres = WidgetSettings.isOnlyPremieres(context, appWidgetId)
        val isOnlyCollected = WidgetSettings.isOnlyCollectedEpisodes(context, appWidgetId)
        val isOnlyFavorites = WidgetSettings.isOnlyFavoriteShows(context, appWidgetId)
        val isOnlyUnwatched = WidgetSettings.isHidingWatchedEpisodes(context, appWidgetId)
        val isInfinite = WidgetSettings.isInfinite(context, appWidgetId)

        val newCursor = when (widgetType) {
            WidgetSettings.Type.SHOWS -> {
                // Exclude hidden.
                val selection = StringBuilder(Shows.SELECTION_NO_HIDDEN)

                // Optionally only favorites.
                if (isOnlyFavorites) {
                    selection.append(" AND ").append(Shows.SELECTION_FAVORITES)
                }

                // With next episode.
                selection.append(" AND ").append(Shows.SELECTION_WITH_NEXT_EPISODE)

                // If next episode is in the future, exclude if too far into the future.
                val timeInAnHour = System.currentTimeMillis() + DateUtils.HOUR_IN_MILLIS
                val upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(context)
                val latestAirtime = (timeInAnHour
                        + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS)
                selection.append(" AND ")
                    .append(Shows.NEXTAIRDATEMS)
                    .append("<=")
                    .append(latestAirtime)

                // Run query, sort based on user preference.
                val sortOrder = ShowsDistillationSettings.getSortQuery(
                    WidgetSettings.getWidgetShowsSortOrderId(context, appWidgetId),
                    false,
                    DisplaySettings.isSortOrderIgnoringArticles(context)
                )
                context.contentResolver.query(
                    Shows.CONTENT_URI_WITH_NEXT_EPISODE,
                    ShowsQuery.PROJECTION,
                    selection.toString(),
                    null,
                    sortOrder
                )
            }
            WidgetSettings.Type.RECENT -> CalendarUtils.calendarQuery(
                context,
                CalendarType.RECENT,
                isOnlyPremieres,
                isOnlyCollected,
                isOnlyFavorites,
                isOnlyUnwatched,
                isInfinite
            )
            // Upcoming is the default.
            else -> CalendarUtils.calendarQuery(
                context,
                CalendarType.UPCOMING,
                isOnlyPremieres,
                isOnlyCollected,
                isOnlyFavorites,
                isOnlyUnwatched,
                isInfinite
            )
        }

        // Do NOT switch to null cursor.
        @Suppress("FoldInitializerAndIfToElvis")
        if (newCursor == null) return

        // Switch out cursor.
        val oldCursor = dataCursor
        this.dataCursor = newCursor
        this.widgetType = widgetType
        this.isLightTheme = WidgetSettings.isLightTheme(context, appWidgetId)
        this.isLargeFont = WidgetSettings.isLargeFont(context, appWidgetId)
        oldCursor?.close()
    }

    override fun onDestroy() {
        // In onDestroy() you should tear down anything that was setup for
        // your data source, eg. cursors, connections, etc.
        dataCursor?.close()
    }

    override fun getCount(): Int {
        return dataCursor?.count ?: 0
    }

    override fun getViewAt(position: Int): RemoteViews {
        // Build a remote views collection item.
        val layoutResId: Int = if (isLightTheme) {
            if (isLargeFont) R.layout.appwidget_row_light_large else R.layout.appwidget_row_light
        } else {
            if (isLargeFont) R.layout.appwidget_row_large else R.layout.appwidget_row
        }
        val rv = RemoteViews(context.packageName, layoutResId)

        // Return empty item if no data available.
        val dataCursor = this.dataCursor
        if (dataCursor == null || dataCursor.isClosed || !dataCursor.moveToPosition(position)) {
            return rv
        }

        // Set the fill-in intent for the collection item.
        val isShowQuery = widgetType == WidgetSettings.Type.SHOWS
        val mediaItemId = dataCursor.getInt(
            if (isShowQuery) ShowsQuery.SHOW_NEXT_EPISODE_ID else CalendarQuery.EPISODE_TVDB_ID
        )
        bundleOf(
            EpisodesActivity.EXTRA_EPISODE_TVDBID to mediaItemId
        ).let {
            Intent().putExtras(it)
        }.let {
            rv.setOnClickFillInIntent(R.id.appwidget_row, it)
        }

        // Set episode description.
        val seasonNumber = dataCursor.getInt(
            if (isShowQuery) ShowsQuery.EPISODE_SEASON else CalendarQuery.SEASON
        )
        val episodeNumber = dataCursor.getInt(
            if (isShowQuery) ShowsQuery.EPISODE_NUMBER else CalendarQuery.NUMBER
        )
        val title = dataCursor.getString(
            if (isShowQuery) ShowsQuery.EPISODE_TITLE else CalendarQuery.TITLE
        )
        var hideTitle = DisplaySettings.preventSpoilers(context)
        if (!isShowQuery) {
            val episodeFlag = dataCursor.getInt(CalendarQuery.WATCHED)
            hideTitle = hideTitle && EpisodeTools.isUnwatched(episodeFlag)
        }
        TextTools.getNextEpisodeString(
            context,
            seasonNumber,
            episodeNumber,
            if (hideTitle) null else title
        ).let {
            rv.setTextViewText(R.id.textViewWidgetEpisode, it)
        }

        // Set relative release time.
        val actualRelease = dataCursor.getLong(
            if (isShowQuery) ShowsQuery.EPISODE_FIRSTAIRED_MS else CalendarQuery.RELEASE_TIME_MS
        ).let {
            TimeTools.applyUserOffset(context, it)
        }
        val releaseTimeString = if (DisplaySettings.isDisplayExactDate(context)) {
            // "Fri Oct 31"
            val day = TimeTools.formatToLocalDay(actualRelease)
            val date = TimeTools.formatToLocalDateShort(context, actualRelease)
            "$day $date"
        } else {
            // "Fri 2 days ago"
            TimeTools.formatToLocalDayAndRelativeTime(context, actualRelease)
        }
        rv.setTextViewText(R.id.widgetAirtime, releaseTimeString)

        // Set absolute release time and network (if any).
        val absoluteTime = TimeTools.formatToLocalTime(context, actualRelease)
        val network = dataCursor.getString(
            if (isShowQuery) ShowsQuery.SHOW_NETWORK else CalendarQuery.SHOW_NETWORK
        )
        rv.setTextViewText(R.id.widgetNetwork, TextTools.dotSeparate(network, absoluteTime))

        // Set show name.
        dataCursor.getString(
            if (isShowQuery) ShowsQuery.SHOW_TITLE else CalendarQuery.SHOW_TITLE
        ).let {
            rv.setTextViewText(R.id.textViewWidgetShow, it)
        }

        // Set show poster.
        val posterPath: String? = dataCursor.getString(
            if (isShowQuery) ShowsQuery.SHOW_POSTER_SMALL else CalendarQuery.SHOW_POSTER_SMALL
        )
        maybeSetPoster(rv, posterPath)

        return rv
    }

    private fun maybeSetPoster(rv: RemoteViews, posterPath: String?) {
        val poster = try {
            ServiceUtils.loadWithPicasso(context, TvdbImageTools.artworkUrl(posterPath))
                .centerCrop()
                .resizeDimen(
                    if (isLargeFont) R.dimen.widget_item_width_large else R.dimen.widget_item_width,
                    if (isLargeFont) R.dimen.widget_item_height_large else R.dimen.widget_item_height
                )
                .get()
        } catch (e: Exception) {
            Timber.e(e, "maybeSetPoster: failed.")
            null
        }
        if (poster != null) {
            rv.setImageViewBitmap(R.id.widgetPoster, poster)
        } else {
            rv.setImageViewResource(R.id.widgetPoster, R.drawable.ic_photo_gray_24dp)
        }
    }

    override fun getLoadingView(): RemoteViews {
        // Create a custom loading view (returning null uses default loading view).
        return RemoteViews(
            context.packageName,
            if (isLightTheme) R.layout.appwidget_row_light else R.layout.appwidget_row
        )
    }

    override fun getViewTypeCount(): Int {
        return 2 // Different view layout for dark and light theme.
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onDataSetChanged() {
        // This is triggered when you call AppWidgetManager.notifyAppWidgetViewDataChanged()
        // on the collection view corresponding to this factory.
        // You can do heaving lifting in here, synchronously.
        // For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously.
        // The widget will remain in its current state while work is
        // being done here, so you don't need to worry about locking up the widget.
        onQueryForData()
    }

    interface ShowsQuery {
        companion object {
            val PROJECTION = arrayOf(
                Qualified.SHOWS_ID,
                Shows.TITLE,
                Shows.NETWORK,
                Shows.POSTER_SMALL,
                Shows.STATUS,
                Shows.NEXTEPISODE,
                Episodes.TITLE,
                Episodes.NUMBER,
                Episodes.SEASON,
                Episodes.FIRSTAIREDMS
            )
            const val SHOW_ID = 0
            const val SHOW_TITLE = 1
            const val SHOW_NETWORK = 2
            const val SHOW_POSTER_SMALL = 3
            const val SHOW_STATUS = 4
            const val SHOW_NEXT_EPISODE_ID = 5
            const val EPISODE_TITLE = 6
            const val EPISODE_NUMBER = 7
            const val EPISODE_SEASON = 8
            const val EPISODE_FIRSTAIRED_MS = 9
        }
    }
}
