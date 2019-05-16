package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import androidx.paging.PositionalDataSource
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2ViewModel.CalendarItem
import com.battlelancer.seriesguide.util.TimeTools
import java.util.Calendar
import java.util.LinkedList

class CalendarDataSource(
    private val context: Context,
    private val source: PositionalDataSource<EpisodeWithShow>
) : PositionalDataSource<CalendarItem>() {

    override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        source.addInvalidatedCallback(onInvalidatedCallback)
    }

    override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        source.removeInvalidatedCallback(onInvalidatedCallback)
    }

    override fun invalidate() {
        source.invalidate()
    }

    override fun isInvalid(): Boolean {
        return source.isInvalid
    }

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<CalendarItem>
    ) {
        source.loadInitial(
            params,
            object : PositionalDataSource.LoadInitialCallback<EpisodeWithShow>() {
                override fun onResult(data: List<EpisodeWithShow>, position: Int, totalCount: Int) {
                    // not using placeholders, can not know the number of headers in advance
                    callback.onResult(convert(data), position)
                }

                override fun onResult(data: List<EpisodeWithShow>, position: Int) {
                    throw IllegalStateException("onResult called by delegate data source.")
                }
            })
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<CalendarItem>) {
        // TODO reduce params.startPosition by amount of previously added headers?
        source.loadRange(
            params,
            object : PositionalDataSource.LoadRangeCallback<EpisodeWithShow>() {
                override fun onResult(data: List<EpisodeWithShow>) {
                    val converted = convert(data)
                    if (converted.size > params.loadSize) {
                        val cutData = LinkedList<CalendarItem>()
                        for (i in 0 until params.loadSize) {
                            cutData.add(converted[i])
                        }
                        callback.onResult(cutData)
                    } else {
                        callback.onResult(converted)
                    }
                }
            })
    }

    // TODO would also need item before this list if not at position 0
    private fun convert(episodes: List<EpisodeWithShow>): MutableList<CalendarItem> {
        val mapped = LinkedList<CalendarItem>()

        val calendar = Calendar.getInstance()
        var previousHeaderTime: Long = 0
        episodes.forEachIndexed { index, episode ->
            // insert header if first item or previous item has different header time
            val headerTime = calculateHeaderTime(
                context,
                calendar,
                episode.episode_firstairedms
            )
            if (index == 0 || headerTime != previousHeaderTime) {
                mapped.add(CalendarItem(headerTime, null))
            }
            previousHeaderTime = headerTime

            mapped.add(CalendarItem(headerTime, episode))
        }

        return mapped
    }

    private fun calculateHeaderTime(context: Context, calendar: Calendar, releaseTime: Long): Long {
        val actualRelease = TimeTools.applyUserOffset(context, releaseTime)

        calendar.time = actualRelease
        // not midnight because upcoming->recent is delayed 1 hour
        // so header would display wrong relative time close to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }
}