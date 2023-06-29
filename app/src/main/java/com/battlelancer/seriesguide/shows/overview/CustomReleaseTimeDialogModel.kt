package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.TimeTools.atDeviceZone
import com.battlelancer.seriesguide.util.TimeTools.formatToDayAndTime
import com.battlelancer.seriesguide.util.TimeTools.formatWithDeviceZoneToDayAndTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZonedDateTime
import java.util.TimeZone
import kotlin.math.absoluteValue

class CustomReleaseTimeDialogModel(application: Application, private val showId: Long) :
    AndroidViewModel(application) {

    val customTimeDataWithStrings = MutableStateFlow<CustomTimeDataWithStrings?>(null)

    init {
        loadCustomTimeInfo(showId)
    }

    private fun loadCustomTimeInfo(showId: Long) {
        val context: Context = getApplication()
        viewModelScope.launch(Dispatchers.IO) {
            val show = SgRoomDatabase.getInstance(context).sgShow2Helper().getShow(showId)
            if (show != null) {
                // Note: if there is no official time, uses a reasonable default.
                val officialTime = TimeTools.getShowReleaseDateTime(
                    context,
                    show.releaseTimeOrDefault,
                    0,
                    show.releaseWeekDayOrDefault,
                    show.releaseTimeZone,
                    show.releaseCountry,
                    show.network,
                    applyCorrections = true
                ).atDeviceZone()

                val customReleaseTime = show.customReleaseTimeOrDefault
                val customTime = if (customReleaseTime == SgShow2.CUSTOM_RELEASE_TIME_NOT_SET) {
                    // If there is no custom time configured, take the official time.
                    officialTime.toLocalTime()
                } else {
                    // As the device time zone may be different from the time zone the custom time
                    // was saved in, always take the saved custom time and time zone and convert it
                    // to the device time zone.
                    TimeTools.getShowReleaseTime(customReleaseTime)
                        .atDate(LocalDate.now())
                        .atZone(TimeTools.getDateTimeZone(show.customReleaseTimeZone))
                        .atDeviceZone().toLocalTime()
                }
                customTimeDataWithStrings.value = CustomTimeDataWithStrings.make(
                    CustomTimeData(
                        officialTime,
                        show.releaseWeekDayOrDefault,
                        customTime,
                        show.customReleaseDayOffsetOrDefault
                    ),
                    context
                )
            }
            // If show == null do nothing, dialog will be blocked, no data is changed.
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        updateCustomTimeIfNotNull {
            it.updateTime(hour, minute)
        }
    }

    fun increaseDayOffset() {
        updateCustomTimeIfNotNull {
            it.increaseDayOffset()
        }
    }

    fun decreaseDayOffset() {
        updateCustomTimeIfNotNull {
            it.decreaseDayOffset()
        }
    }

    private fun updateCustomTimeIfNotNull(modifier: (CustomTimeData) -> CustomTimeData) {
        viewModelScope.launch(Dispatchers.IO) {
            customTimeDataWithStrings.value?.also {
                customTimeDataWithStrings.value = CustomTimeDataWithStrings.make(
                    modifier.invoke(it.customTimeData),
                    getApplication()
                )
            }
        }
    }

    fun saveToDatabase() {
        val customTimeInfo = customTimeDataWithStrings.value?.customTimeData ?: return
        val customReleaseTime =
            customTimeInfo.customTime.hour * 100 + customTimeInfo.customTime.minute
        val customDayOffset = customTimeInfo.customDayOffset
        // Always use the current device time zone.
        val customTimeZone = TimeZone.getDefault().id
        SgApp.getServicesComponent(getApplication()).showTools()
            .storeCustomReleaseTime(showId, customReleaseTime, customDayOffset, customTimeZone)
    }

    fun resetToOfficialAndSave() {
        SgApp.getServicesComponent(getApplication()).showTools()
            .storeCustomReleaseTime(
                showId,
                SgShow2.CUSTOM_RELEASE_TIME_NOT_SET,
                SgShow2.CUSTOM_RELEASE_DAY_OFFSET_NOT_SET,
                SgShow2.CUSTOM_RELEASE_TIME_ZONE_NOT_SET
            )
    }

    companion object {

        val SHOW_ID_KEY = object : CreationExtras.Key<Long> {}

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = extras[APPLICATION_KEY]!!
                val showId = extras[SHOW_ID_KEY]!!

                return CustomReleaseTimeDialogModel(application, showId) as T
            }
        }
    }
}

data class CustomTimeData(
    /** Official time converted to device time zone. */
    val officialTime: ZonedDateTime,
    val officialWeekDay: Int,
    val customTime: LocalTime,
    val customDayOffset: Int
) {
    fun updateTime(hour: Int, minute: Int): CustomTimeData {
        return copy(
            customTime = LocalTime.of(hour, minute)
        )
    }

    fun increaseDayOffset(): CustomTimeData {
        return copy(
            customDayOffset = (customDayOffset + 1)
                .coerceAtMost(SgShow2.MAX_CUSTOM_DAY_OFFSET)
        )
    }

    fun decreaseDayOffset(): CustomTimeData {
        return copy(
            customDayOffset = (customDayOffset - 1)
                .coerceAtLeast(-SgShow2.MAX_CUSTOM_DAY_OFFSET)
        )
    }
}

data class CustomTimeDataWithStrings(
    val customTimeData: CustomTimeData,
    val officialTimeString: String,
    val customTimeString: String,
    val customDayOffsetString: String,
    val customDayOffsetDirectionString: String
) {
    companion object {
        fun make(data: CustomTimeData, context: Context): CustomTimeDataWithStrings {
            // For formatting, never use "daily" but always use the current day.
            val weekDayNeverDaily =
                if (data.officialWeekDay == TimeTools.RELEASE_WEEKDAY_DAILY) {
                    -1
                } else data.officialWeekDay
            val customDayOffsetString =
                (if (data.customDayOffset != SgShow2.CUSTOM_RELEASE_DAY_OFFSET_NOT_SET) {
                    data.customDayOffset.absoluteValue
                } else {
                    0
                }).let { context.resources.getQuantityString(R.plurals.days_plural, it, it) }
            return CustomTimeDataWithStrings(
                data,
                officialTimeString = data.officialTime.formatToDayAndTime(
                    context,
                    weekDayNeverDaily
                ),
                customTimeString = TimeTools.getShowReleaseDateTime(
                    context,
                    data.customTime.hour * 100 + data.customTime.minute,
                    data.customDayOffset,
                    data.officialWeekDay,
                    TimeZone.getDefault().id,
                    null, null,
                    applyCorrections = false
                ).formatWithDeviceZoneToDayAndTime(context, weekDayNeverDaily),
                customDayOffsetString,
                customDayOffsetDirectionString = when {
                    data.customDayOffset < 0 -> context.getString(R.string.custom_release_time_earlier)
                    else -> context.getString(R.string.custom_release_time_later)
                }
            )
        }
    }
}
