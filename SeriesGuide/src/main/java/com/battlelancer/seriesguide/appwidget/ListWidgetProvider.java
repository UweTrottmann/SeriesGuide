/*
 * Copyright 2012 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.battlelancer.seriesguide.appwidget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import com.battlelancer.seriesguide.settings.WidgetSettings;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.UpcomingRecentActivity;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

@TargetApi(11)
public class ListWidgetProvider extends AppWidgetProvider {

    public static final String UPDATE = "com.battlelancer.seriesguide.appwidget.UPDATE";
    public static final long REPETITION_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;
    private static final int DIP_THRESHOLD_COMPACT_LAYOUT = 80;

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        // remove the update alarm if the last widget is gone
        Intent update = new Intent(UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 195, update, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // check if we received our update alarm
        if (UPDATE.equals(intent.getAction())) {
            // trigger refresh of list widgets
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                    ListWidgetProvider.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // update all added list widgets
        for (int appWidgetId : appWidgetIds) {
            onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, null);
        }

        // set an alarm to update widgets every x mins if the device is awake
        Intent update = new Intent(UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 195, update, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()
                + REPETITION_INTERVAL, REPETITION_INTERVAL, pi);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        RemoteViews rv = buildRemoteViews(context, appWidgetManager, appWidgetId);

        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static RemoteViews buildRemoteViews(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // determine layout based on given size
        final boolean isCompactLayout = isCompactLayout(appWidgetManager, appWidgetId);
        // determine content type from widget settings
        final int typeIndex = WidgetSettings.getWidgetListType(context, appWidgetId);

        // Here we setup the intent which points to the StackViewService
        // which will provide the views for this collection.
        Intent intent = new Intent(context, ListWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        // When intents are compared, the extras are ignored, so we need to
        // embed the extras into the data so that the extras will not be
        // ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews rv = new RemoteViews(context.getPackageName(),
                isCompactLayout ? R.layout.appwidget_v11_compact : R.layout.appwidget_v11);

        if (AndroidUtils.isICSOrHigher()) {
            rv.setRemoteAdapter(R.id.list_view, intent);
        } else {
            rv.setRemoteAdapter(appWidgetId, R.id.list_view, intent);
        }

        // The empty view is displayed when the collection has no items. It
        // should be a sibling of the collection view.
        rv.setEmptyView(R.id.list_view, R.id.empty_view);

        // set the background color
        int bgColor = WidgetSettings.getWidgetBackgroundColor(context, appWidgetId);
        rv.setInt(R.id.container, "setBackgroundColor", bgColor);

        // determine the activity tab touching the widget title should open
        int activityTab = typeIndex == WidgetSettings.Type.RECENT ? 1 : 0;

        // only regular layout has title
        if (!isCompactLayout) {
            // change title based on config
            if (typeIndex == WidgetSettings.Type.RECENT) {
                rv.setTextViewText(R.id.widgetTitle, context.getString(R.string.recent));
            } else if (typeIndex == WidgetSettings.Type.FAVORITES) {
                rv.setTextViewText(R.id.widgetTitle,
                        context.getString(R.string.action_shows_filter_favorites));
            } else {
                rv.setTextViewText(R.id.widgetTitle, context.getString(R.string.upcoming));
            }

            // Activity button
            PendingIntent pendingIntent;
            if (typeIndex == WidgetSettings.Type.FAVORITES) {
                // launching the shows list
                Intent activityIntent = new Intent(context, ShowsActivity.class);
                pendingIntent = TaskStackBuilder
                        .create(context)
                        .addNextIntent(activityIntent)
                        .getPendingIntent(appWidgetId, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                // launching an activities list
                Intent activityIntent = new Intent(context, UpcomingRecentActivity.class);
                activityIntent.putExtra(UpcomingRecentActivity.InitBundle.SELECTED_TAB, activityTab);
                pendingIntent = TaskStackBuilder
                        .create(context)
                        .addNextIntent(new Intent(context, ShowsActivity.class))
                        .addNextIntent(activityIntent)
                        .getPendingIntent(appWidgetId, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            rv.setOnClickPendingIntent(R.id.widget_title, pendingIntent);
        }

        // Intent template for items to launch an EpisodesActivity
        TaskStackBuilder builder = TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class));
        if (typeIndex != WidgetSettings.Type.FAVORITES) {
            // only insert activity page for activity types
            builder.addNextIntent(
                    new Intent(context, UpcomingRecentActivity.class).putExtra(
                            UpcomingRecentActivity.InitBundle.SELECTED_TAB, activityTab));
        }
        builder.addNextIntent(new Intent(context, EpisodesActivity.class));
        rv.setPendingIntentTemplate(R.id.list_view,
                builder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT));

        // Show list button
        Intent homeIntent = new Intent(context, ShowsActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingHomeIntent = TaskStackBuilder
                .create(context)
                .addNextIntent(homeIntent)
                .getPendingIntent(appWidgetId, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_logo, pendingHomeIntent);

        // Settings button
        Intent settingsIntent = new Intent(context, ListWidgetConfigure.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        rv.setOnClickPendingIntent(R.id.widget_settings,
                PendingIntent.getActivity(context, appWidgetId,
                        settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        return rv;
    }

    /**
     * Based on the widget size determines whether to use a compact layout. Defaults to false on
     * ICS and below.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static boolean isCompactLayout(AppWidgetManager appWidgetManager, int appWidgetId) {
        if (AndroidUtils.isJellyBeanOrHigher()) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            return minHeight < DIP_THRESHOLD_COMPACT_LAYOUT;
        }
        return false;
    }
}
