package com.battlelancer.seriesguide.appwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import android.widget.RemoteViews
import androidx.core.app.TaskStackBuilder
import androidx.core.content.getSystemService
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.WidgetSettings
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import timber.log.Timber
import java.util.Random

/**
 * [AppWidgetProvider] for the list widget handles general widget layout and theme
 * and scheduling updates.
 *
 * Useful commands for testing:
 *
 * Dump alarms: `adb shell dumpsys alarm > alarms.txt`
 *
 * Note: Doze mode and App Standby should have no effect on the alarm. Instead turn off the
 * device screen.
 *
 * https://developer.android.com/training/monitoring-device-state/doze-standby.html
 */
class ListWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            // Check for null as super.onReceive does not,
            // only guard here as other methods are only called by super.onReceive.
            return
        }

        if (ACTION_DATA_CHANGED == intent.action) {
            // Trigger refresh of list widgets.
            Timber.d("onReceive: widget DATA_CHANGED action.")

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ListWidgetProvider::class.java)
            ) ?: return
            if (appWidgetIds.isEmpty()) {
                return

            }
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view)
            scheduleWidgetUpdate(context)
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        // Remove the update alarm if the last widget is gone.
        val am: AlarmManager? = context.getSystemService()
        if (am != null) {
            val pendingIntent = buildDataChangedPendingIntent(context)
            am.cancel(pendingIntent)
            Timber.d("onDisabled: canceled widget UPDATE alarm.")
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all added list widgets.
        for (appWidgetId in appWidgetIds) {
            onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, null)
        }
        scheduleWidgetUpdate(context)
    }

    private fun scheduleWidgetUpdate(context: Context) {
        // Set an alarm to update widgets every x mins if the device is awake.
        // Use one-shot alarm as repeating alarms get batched while the device is asleep
        // and are then *all* delivered.
        val am: AlarmManager? = context.getSystemService()
        if (am != null) {
            val pendingIntent = buildDataChangedPendingIntent(context)
            am.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + REPETITION_INTERVAL,
                pendingIntent
            )
            Timber.d("scheduled widget UPDATE alarm.")
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        val rv = buildRemoteViews(context, appWidgetManager, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, rv)
    }

    private fun buildDataChangedPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            buildDataChangedIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val ACTION_DATA_CHANGED = "com.battlelancer.seriesguide.appwidget.UPDATE"
        const val REQUEST_CODE = 195

        private const val REPETITION_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS
        private const val DIP_THRESHOLD_COMPACT_LAYOUT = 80

        private val USE_NONCE_WORKAROUND =
            Build.VERSION.SDK_INT == Build.VERSION_CODES.O && "Huawei" == Build.MANUFACTURER
        private val random = Random()

        /**
         * Send broadcast to update lists of all list widgets.
         */
        @JvmStatic
        fun notifyDataChanged(context: Context) {
            context.applicationContext.sendBroadcast(buildDataChangedIntent(context))
        }

        fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): RemoteViews {
            // Setup intent pointing to RemoteViewsService providing the views for the collection.
            val intent = Intent(context, ListWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            /*
            Huawei EMUI 8.0 devices seem to have broken caching for widgets with collections.
            This leads to the widget not updating after a while. Adding a changing Intent when updating
            the widget seems to fix the issue as the remote adapter appears to change every time.
            This causes increased battery usage as the widget redraws even if no data has changed,
            so only enable this on Huawei devices running Android 8.0 (EMUI 8.0).
            https://github.com/UweTrottmann/SeriesGuide/issues/549
             */
            if (USE_NONCE_WORKAROUND) {
                intent.putExtra("nonce", random.nextInt())
            }
            // When intents are compared, the extras are ignored, so embed
            // the extras into the data so if extras change intents will not be equal.
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

            // Determine layout (current size) and theme (user pref).
            val isCompactLayout = isCompactLayout(appWidgetManager, appWidgetId)
            val isLightTheme = WidgetSettings.isLightTheme(context, appWidgetId)
            val layoutResId = when {
                isLightTheme && isCompactLayout -> R.layout.appwidget_v11_light_compact
                isLightTheme && !isCompactLayout -> R.layout.appwidget_v11_light
                !isLightTheme && isCompactLayout -> R.layout.appwidget_v11_compact
                else -> R.layout.appwidget_v11
            }

            // Build widget views.
            val rv = RemoteViews(context.packageName, layoutResId).also {
                it.setRemoteAdapter(R.id.list_view, intent)
                // The empty view is displayed when the collection has no items. It
                // should be a sibling of the collection view.
                it.setEmptyView(R.id.list_view, R.id.empty_view)

                // Set the background colors of the whole widget.
                val bgColor =
                    WidgetSettings.getWidgetBackgroundColor(context, appWidgetId, isLightTheme)
                it.setInt(R.id.container, "setBackgroundColor", bgColor)
            }

            // Determine type specific values.
            val widgetType = WidgetSettings.getWidgetListType(context, appWidgetId)
            val showsTabIndex: Int
            val titleResId: Int
            val emptyResId: Int
            when (widgetType) {
                WidgetSettings.Type.SHOWS -> {
                    // Shows.
                    showsTabIndex = ShowsActivity.InitBundle.INDEX_TAB_SHOWS
                    titleResId = R.string.shows
                    emptyResId = R.string.no_nextepisode
                }
                WidgetSettings.Type.RECENT -> {
                    showsTabIndex = ShowsActivity.InitBundle.INDEX_TAB_RECENT
                    titleResId = R.string.recent
                    emptyResId = R.string.norecent
                }
                else -> {
                    // Upcoming is the default.
                    showsTabIndex = ShowsActivity.InitBundle.INDEX_TAB_UPCOMING
                    titleResId = R.string.upcoming
                    emptyResId = R.string.noupcoming
                }
            }

            // Change title and empty view based on type.
            rv.setTextViewText(R.id.empty_view, context.getString(emptyResId))
            if (!isCompactLayout) {
                // Note: compact layout has no title.
                rv.setTextViewText(R.id.widgetTitle, context.getString(titleResId))
            }

            // Set up app launch button.
            val appLaunchIntent = Intent(context, ShowsActivity::class.java)
                .putExtra(ShowsActivity.InitBundle.SELECTED_TAB, showsTabIndex)
            TaskStackBuilder.create(context)
                .addNextIntent(appLaunchIntent)
                .getPendingIntent(appWidgetId, PendingIntent.FLAG_UPDATE_CURRENT).let {
                    rv.setOnClickPendingIntent(R.id.widget_title, it)
                }

            // Set up item intent template, launches episode detail view.
            TaskStackBuilder.create(context).run {
                addNextIntent(appLaunchIntent)
                addNextIntent(Intent(context, EpisodesActivity::class.java))
                getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT)
            }.let {
                rv.setPendingIntentTemplate(R.id.list_view, it)
            }

            // Set up settings button.
            Intent(context, ListWidgetPreferenceActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }.let {
                rv.setOnClickPendingIntent(
                    R.id.widget_settings,
                    PendingIntent.getActivity(
                        context, appWidgetId, it, PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }

            return rv
        }

        /**
         * Based on the widget size determines whether to use a compact layout.
         */
        private fun isCompactLayout(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): Boolean {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            return minHeight < DIP_THRESHOLD_COMPACT_LAYOUT
        }

        private fun buildDataChangedIntent(context: Context): Intent {
            // Use explicit intent to work around implicit broadcast restrictions on O+.
            return Intent(context, ListWidgetProvider::class.java).apply {
                action = ACTION_DATA_CHANGED
            }
        }
    }
}