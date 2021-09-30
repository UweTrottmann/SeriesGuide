package com.battlelancer.seriesguide.appwidget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.os.bundleOf
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.provider.SgEpisode2WithShow
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgShow2ForLists
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.WidgetSettings
import com.battlelancer.seriesguide.settings.WidgetSettings.WidgetTheme
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import timber.log.Timber
import java.util.Date

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
    private var shows = mutableListOf<SgShow2ForLists>()
    private var episodesWithShow = mutableListOf<SgEpisode2WithShow>()
    private var widgetType = 0
    private var theme = WidgetTheme.SYSTEM
    private var isLargeFont = false
    private var isHideWatchButton = false

    override fun onCreate() {
        // Since onQueryForData() is called in onDataSetChanged()
        // which gets called immediately after onCreate(),
        // there is nothing to do here.
    }

    @SuppressLint("Recycle") // Cursor close check broken for Kotlin.
    private fun onQueryForData() {
        Timber.d("onQueryForData: %d", appWidgetId)

        // Clear any existing data.
        shows.clear()
        episodesWithShow.clear()

        val widgetType = WidgetSettings.getWidgetListType(context, appWidgetId)
        this.widgetType = widgetType
        this.theme = WidgetSettings.getTheme(context, appWidgetId)
        this.isLargeFont = WidgetSettings.isLargeFont(context, appWidgetId)
        this.isHideWatchButton = WidgetSettings.isHideWatchButton(context, appWidgetId)

        when (widgetType) {
            WidgetSettings.Type.SHOWS -> {
                // Exclude hidden and without next episode.
                val selection = StringBuilder(SgShow2Columns.SELECTION_NO_HIDDEN)
                    .append(" AND ").append(SgShow2Columns.SELECTION_HAS_NEXT_EPISODE)

                // Optionally only favorites.
                if (WidgetSettings.isOnlyFavoriteShows(context, appWidgetId)) {
                    selection.append(" AND ").append(SgShow2Columns.SELECTION_FAVORITES)
                }

                // If next episode is in the future and upcoming range is not all,
                // exclude if too far into the future.
                val timeInAnHour = System.currentTimeMillis() + DateUtils.HOUR_IN_MILLIS
                val upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(context)
                if (upcomingLimitInDays != -1) {
                    val maxReleaseDate =
                        (timeInAnHour + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS)
                    selection.append(" AND ")
                        .append(SgShow2Columns.NEXTAIRDATEMS)
                        .append("<=")
                        .append(maxReleaseDate)
                }

                // Sort based on user preference.
                val orderClause = ShowsDistillationSettings.getSortQuery2(
                    WidgetSettings.getWidgetShowsSortOrderId(context, appWidgetId),
                    false,
                    DisplaySettings.isSortOrderIgnoringArticles(context)
                )

                // Run query
                val query = "SELECT * FROM ${Tables.SG_SHOW} WHERE $selection ORDER BY $orderClause"
                val results = SgRoomDatabase.getInstance(context).sgShow2Helper()
                    .getShows(SimpleSQLiteQuery(query))
                shows.addAll(results)
            }
            WidgetSettings.Type.RECENT -> getUpcomingElseRecentEpisodes(false)
            WidgetSettings.Type.UPCOMING -> getUpcomingElseRecentEpisodes(true)
            else -> throw UnsupportedOperationException("Widget type not supported")
        }
    }

    private fun getUpcomingElseRecentEpisodes(isUpcomingElseRecent: Boolean) {
        val query = SgEpisode2WithShow.buildEpisodesWithShowQuery(
            context,
            isUpcomingElseRecent,
            isInfiniteCalendar = WidgetSettings.isInfinite(context, appWidgetId),
            isOnlyFavorites = WidgetSettings.isOnlyFavoriteShows(context, appWidgetId),
            isOnlyUnwatched = WidgetSettings.isHidingWatchedEpisodes(context, appWidgetId),
            isOnlyCollected = WidgetSettings.isOnlyCollectedEpisodes(context, appWidgetId),
            isOnlyPremieres = WidgetSettings.isOnlyPremieres(context, appWidgetId)
        )
        // In addition limit results for widget to reduce memory consumption.
        val results = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .getEpisodesWithShow(SimpleSQLiteQuery("$query LIMIT 100"))
        episodesWithShow.addAll(results)
    }

    override fun onDestroy() {
        // In onDestroy() you should tear down anything that was setup for
        // your data source, eg. cursors, connections, etc.
        // Note: Do nothing, not even clearing existing data as it might still be displayed.
    }

    override fun getCount(): Int {
        return if (widgetType == WidgetSettings.Type.SHOWS) {
            shows.size
        } else if (widgetType == WidgetSettings.Type.RECENT
            || widgetType == WidgetSettings.Type.UPCOMING) {
            episodesWithShow.size
        } else {
            throw UnsupportedOperationException("Invalid widget type")
        }
    }

    private fun getRowLayoutResId(): Int {
        return when (theme) {
            WidgetTheme.DARK -> if (isLargeFont) R.layout.appwidget_row_dark_large else R.layout.appwidget_row_dark
            WidgetTheme.LIGHT -> if (isLargeFont) R.layout.appwidget_row_light_large else R.layout.appwidget_row_light
            WidgetTheme.SYSTEM -> if (isLargeFont) R.layout.appwidget_row_day_night_large else R.layout.appwidget_row_day_night
        }
    }

    override fun getViewAt(position: Int): RemoteViews {
        // Build a remote views collection item.
        val rv = RemoteViews(context.packageName, getRowLayoutResId())

        if (widgetType == WidgetSettings.Type.SHOWS) {
            val show = shows.getOrNull(position) ?: return rv // No data: empty item.
            val hasNextEpisode = show.nextText.isNotEmpty()
            val episodeDescription = show.nextText
            val actualRelease =
                if (hasNextEpisode) TimeTools.applyUserOffset(context, show.nextAirdateMs) else null
            return bindViewAt(
                rv,
                show.nextEpisode?.toLongOrNull(),
                episodeDescription,
                actualRelease,
                show.network,
                show.title,
                show.posterSmall,
                EpisodeFlags.UNWATCHED // next episode always not watched
            )
        } else {
            val episode = episodesWithShow.getOrNull(position) ?: return rv // No data: empty item.
            val titleOrNull = if (DisplaySettings.preventSpoilers(context)
                && EpisodeTools.isUnwatched(episode.watched)) {
                null
            } else {
                episode.episodetitle
            }
            val episodeDescription = TextTools.getNextEpisodeString(
                context,
                episode.season,
                episode.episodenumber,
                titleOrNull
            )
            return bindViewAt(
                rv,
                episode.id,
                episodeDescription,
                actualRelease = TimeTools.applyUserOffset(context, episode.episode_firstairedms),
                episode.network,
                episode.seriestitle,
                episode.series_poster_small,
                episode.watched
            )
        }
    }

    private fun bindViewAt(
        rv: RemoteViews,
        episodeId: Long?,
        episodeDescription: String,
        actualRelease: Date?,
        network: String?,
        showTitle: String,
        posterPath: String?,
        episodeFlag: Int
    ): RemoteViews {
        // Set the fill-in intents for the collection item.
        if (episodeId != null) {
            // Display details
            bundleOf(
                ListWidgetProvider.EXTRA_EPISODE_ID to episodeId
            ).let {
                Intent().putExtras(it)
            }.let {
                rv.setOnClickFillInIntent(R.id.appwidget_row, it)
            }

            // Change watched flag
            val newEpisodeFlag = if (episodeFlag == EpisodeFlags.WATCHED) {
                EpisodeFlags.UNWATCHED
            } else {
                EpisodeFlags.WATCHED
            }
            bundleOf(
                ListWidgetProvider.EXTRA_EPISODE_ID to episodeId,
                ListWidgetProvider.EXTRA_EPISODE_FLAG to newEpisodeFlag
            ).let {
                Intent().putExtras(it)
            }.let {
                rv.setOnClickFillInIntent(R.id.widgetWatchedButton, it)
            }
        }

        // Set watched button image based on watched state
        val isWatched = EpisodeTools.isWatched(episodeFlag)
        rv.setImageViewResource(
            R.id.widgetWatchedButton,
            if (isWatched) R.drawable.ic_watched_24dp else R.drawable.ic_watch_black_24dp
        )
        rv.setContentDescription(
            R.id.widgetWatchedButton,
            context.getString(if (isWatched) R.string.action_unwatched else R.string.action_watched)
        )
        if (isHideWatchButton) {
            rv.setViewVisibility(R.id.widgetWatchedButton, View.GONE)
            rv.setViewPadding(
                R.id.relativeLayoutWidgetText,
                0,
                0,
                context.resources.getDimensionPixelSize(R.dimen.large_padding),
                0
            )
        } else {
            rv.setViewVisibility(R.id.widgetWatchedButton, View.VISIBLE)
            rv.setViewPadding(R.id.relativeLayoutWidgetText, 0, 0, 0, 0)
        }

        // Set episode description.
        rv.setTextViewText(R.id.textViewWidgetEpisode, episodeDescription)

        // Set relative release time.
        val releaseTime = when {
            actualRelease == null -> ""
            DisplaySettings.isDisplayExactDate(context) -> {
                // "Fri Oct 31"
                val day = TimeTools.formatToLocalDay(actualRelease)
                val date = TimeTools.formatToLocalDateShort(context, actualRelease)
                "$day $date"
            }
            else -> {
                // "Fri 2 days ago"
                TimeTools.formatToLocalDayAndRelativeTime(context, actualRelease)
            }
        }
        rv.setTextViewText(R.id.widgetAirtime, releaseTime)

        // Set absolute release time and network (if any).
        val absoluteTime = if (actualRelease != null) {
            TimeTools.formatToLocalTime(context, actualRelease)
        } else {
            ""
        }
        rv.setTextViewText(R.id.widgetNetwork, TextTools.dotSeparate(network, absoluteTime))

        // Set show name.
        rv.setTextViewText(R.id.textViewWidgetShow, showTitle)

        // Set show poster.
        maybeSetPoster(rv, posterPath)

        return rv
    }

    private fun maybeSetPoster(rv: RemoteViews, posterPath: String?) {
        val poster = try {
            ServiceUtils.loadWithPicasso(
                context,
                ImageTools.tmdbOrTvdbPosterUrl(posterPath, context)
            )
                .centerCrop()
                .resizeDimen(
                    if (isLargeFont) R.dimen.widget_poster_width_large else R.dimen.widget_poster_width,
                    if (isLargeFont) R.dimen.widget_poster_height_large else R.dimen.widget_poster_height
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

    // Create a custom loading view (returning null uses default loading view).
    override fun getLoadingView(): RemoteViews =
        RemoteViews(context.packageName, getRowLayoutResId())

    override fun getViewTypeCount(): Int = 2 // Different view layout for dark and light theme.

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    // This is triggered when you call AppWidgetManager.notifyAppWidgetViewDataChanged()
    // on the collection view corresponding to this factory.
    // You can do heaving lifting in here, synchronously.
    // For example, if you need to process an image, fetch something
    // from the network, etc., it is ok to do it here, synchronously.
    // The widget will remain in its current state while work is
    // being done here, so you don't need to worry about locking up the widget.
    override fun onDataSetChanged() = onQueryForData()

}
