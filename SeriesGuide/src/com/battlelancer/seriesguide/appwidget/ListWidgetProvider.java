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
import android.os.SystemClock;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.UpcomingRecentActivity;
import com.battlelancer.seriesguide.util.WidgetSettings;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

@TargetApi(11)
public class ListWidgetProvider extends AppWidgetProvider {

    public static final String UPDATE = "com.battlelancer.seriesguide.appwidget.UPDATE";

    public static final long REPETITION_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;

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

        if (UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                    ListWidgetProvider.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view);
            // Toast.makeText(context,
            // "ListWidgets called to refresh " + Arrays.toString(appWidgetIds),
            // Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            // Toast.makeText(context, "Refreshing widget " + appWidgetIds[i],
            // Toast.LENGTH_SHORT)
            // .show();

            RemoteViews rv = buildRemoteViews(context, appWidgetIds[i]);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // set an alarm to update widgets every x mins if the device is awake
        Intent update = new Intent(UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 195, update, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()
                + REPETITION_INTERVAL, REPETITION_INTERVAL, pi);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @SuppressWarnings("deprecation")
    public static RemoteViews buildRemoteViews(Context context, int appWidgetId) {

        // Here we setup the intent which points to the StackViewService
        // which will provide the views for this collection.
        Intent intent = new Intent(context, ListWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        // When intents are compared, the extras are ignored, so we need to
        // embed the extras into the data so that the extras will not be
        // ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_v11);
        if (AndroidUtils.isICSOrHigher()) {
            rv.setRemoteAdapter(R.id.list_view, intent);
        } else {
            rv.setRemoteAdapter(appWidgetId, R.id.list_view, intent);
        }

        // The empty view is displayed when the collection has no items. It
        // should be a sibling of the collection view.
        rv.setEmptyView(R.id.list_view, R.id.empty_view);

        // change title based on config
        int typeIndex = WidgetSettings.getWidgetListType(context, appWidgetId);
        int activityTab = 0;
        if (typeIndex == 1) {
            activityTab = 1;
            rv.setTextViewText(R.id.widgetTitle, context.getString(R.string.recent));
        } else {
            activityTab = 0;
            rv.setTextViewText(R.id.widgetTitle, context.getString(R.string.upcoming));
        }

        // set the background color
        int bgColor = WidgetSettings.getWidgetBackgroundColor(context, appWidgetId);
        rv.setInt(R.id.container, "setBackgroundColor", bgColor);

        // Activity button
        Intent activityIntent = new Intent(context, UpcomingRecentActivity.class);
        activityIntent.putExtra(UpcomingRecentActivity.InitBundle.SELECTED_TAB, activityTab);
        PendingIntent pendingIntent = TaskStackBuilder
                .create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(activityIntent)
                .getPendingIntent(appWidgetId, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

        // Intent template for items to launch an EpisodesActivity
        Intent itemIntent = new Intent(context, EpisodesActivity.class);
        PendingIntent pendingIntentTemplate = TaskStackBuilder
                .create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(
                        new Intent(context, UpcomingRecentActivity.class).putExtra(
                                UpcomingRecentActivity.InitBundle.SELECTED_TAB, activityTab))
                .addNextIntent(itemIntent)
                .getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.list_view, pendingIntentTemplate);

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
}
