/*
 * Copyright 2014 Uwe Trottmann
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
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.WidgetSettings;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.uwetrottmann.androidutils.AndroidUtils;

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
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        RemoteViews rv = buildRemoteViews(context, appWidgetManager, appWidgetId);

        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static RemoteViews buildRemoteViews(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {
        // setup intent pointing to RemoteViewsService providing the views for the collection
        Intent intent = new Intent(context, ListWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // When intents are compared, the extras are ignored, so we need to
        // embed the extras into the data so that the extras will not be
        // ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        // determine layout (current size) and theme (user pref)
        final boolean isCompactLayout = isCompactLayout(appWidgetManager, appWidgetId);
        final boolean isLightTheme = WidgetSettings.isLightTheme(context, appWidgetId);
        int layoutResId;
        if (isLightTheme) {
            layoutResId = isCompactLayout ?
                    R.layout.appwidget_v11_light_compact : R.layout.appwidget_v11_light;
        } else {
            layoutResId = isCompactLayout ?
                    R.layout.appwidget_v11_compact : R.layout.appwidget_v11;
        }

        // build widget views
        RemoteViews rv = new RemoteViews(context.getPackageName(), layoutResId);
        rv.setRemoteAdapter(R.id.list_view, intent);
        // The empty view is displayed when the collection has no items. It
        // should be a sibling of the collection view.
        rv.setEmptyView(R.id.list_view, R.id.empty_view);

        // set the background color
        int bgColor = WidgetSettings.getWidgetBackgroundColor(context, appWidgetId, isLightTheme);
        rv.setInt(R.id.container, "setBackgroundColor", bgColor);

        // determine type specific values
        final int widgetType = WidgetSettings.getWidgetListType(context, appWidgetId);
        int showsTabIndex;
        int titleResId;
        int emptyResId;
        if (widgetType == WidgetSettings.Type.UPCOMING) {
            // upcoming
            showsTabIndex = ShowsActivity.InitBundle.INDEX_TAB_UPCOMING;
            titleResId = R.string.upcoming;
            emptyResId = R.string.noupcoming;
        } else if (widgetType == WidgetSettings.Type.RECENT) {
            // recent
            showsTabIndex = ShowsActivity.InitBundle.INDEX_TAB_RECENT;
            titleResId = R.string.recent;
            emptyResId = R.string.norecent;
        } else {
            // favorites
            showsTabIndex = ShowsActivity.InitBundle.INDEX_TAB_SHOWS;
            titleResId = R.string.action_shows_filter_favorites;
            emptyResId = R.string.no_nextepisode;
        }

        // change title and empty view based on type
        rv.setTextViewText(R.id.empty_view, context.getString(emptyResId));
        if (!isCompactLayout) {
            // only regular layout has text title
            rv.setTextViewText(R.id.widgetTitle, context.getString(titleResId));
        }

        // app launch button
        final Intent appLaunchIntent = new Intent(context, ShowsActivity.class)
                .putExtra(ShowsActivity.InitBundle.SELECTED_TAB, showsTabIndex);
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntent(appLaunchIntent)
                .getPendingIntent(appWidgetId, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

        // item intent template, launches episode detail view
        TaskStackBuilder builder = TaskStackBuilder.create(context);
        builder.addNextIntent(appLaunchIntent);
        builder.addNextIntent(new Intent(context, EpisodesActivity.class));
        rv.setPendingIntentTemplate(R.id.list_view,
                builder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT));

        // settings button
        Intent settingsIntent = new Intent(context, ListWidgetConfigure.class)
                .addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        rv.setOnClickPendingIntent(R.id.widget_settings,
                PendingIntent.getActivity(context, appWidgetId,
                        settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        return rv;
    }

    /**
     * Based on the widget size determines whether to use a compact layout. Defaults to false on ICS
     * and below.
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
