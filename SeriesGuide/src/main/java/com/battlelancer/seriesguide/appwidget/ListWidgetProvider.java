package com.battlelancer.seriesguide.appwidget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.WidgetSettings;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import timber.log.Timber;

@TargetApi(11)
public class ListWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_DATA_CHANGED
            = "com.battlelancer.seriesguide.appwidget.UPDATE";
    public static final int REQUEST_CODE = 195;

    private static final long REPETITION_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;

    private static final int DIP_THRESHOLD_COMPACT_LAYOUT = 80;

    /**
     * Send broadcast to update lists of all list widgets.
     */
    public static void notifyDataChanged(Context context) {
        context.getApplicationContext().sendBroadcast(getDataChangedIntent(context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            // check for null as super.onReceive does not
            // we only need to guard here as other methods are only called by super.onReceive
            return;
        }

        if (ACTION_DATA_CHANGED.equals(intent.getAction())) {
            // trigger refresh of list widgets
            Timber.d("onReceive: widget DATA_CHANGED action.");
            // use app context as this may be called by activities that can be destroyed
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (appWidgetManager == null) {
                return;
            }
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                    ListWidgetProvider.class));
            if (appWidgetIds == null || appWidgetIds.length == 0) {
                return;
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onDisabled(Context context) {
        // remove the update alarm if the last widget is gone
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            PendingIntent pi = getDataChangedPendingIntent(context);
            am.cancel(pi);
            Timber.d("onDisabled: canceled widget UPDATE alarm.");
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // update all added list widgets
        for (int appWidgetId : appWidgetIds) {
            onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, null);
        }

        // set an alarm to update widgets every x mins if the device is awake
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            PendingIntent pi = getDataChangedPendingIntent(context);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()
                    + REPETITION_INTERVAL, REPETITION_INTERVAL, pi);
            Timber.d("onUpdate: scheduled widget UPDATE alarm.");
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        RemoteViews rv = buildRemoteViews(context, appWidgetManager, appWidgetId);

        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

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

        // set the background colors of...
        // ...the header
        boolean isDarkTheme = WidgetSettings.isDarkTheme(context, appWidgetId);
        rv.setInt(R.id.containerWidgetHeader, "setBackgroundColor", isDarkTheme ? Color.TRANSPARENT
                : ContextCompat.getColor(context, R.color.accent_primary));
        // ...the whole widget
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
            // shows
            showsTabIndex = ShowsActivity.InitBundle.INDEX_TAB_SHOWS;
            titleResId = R.string.shows;
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
    private static boolean isCompactLayout(AppWidgetManager appWidgetManager, int appWidgetId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            return minHeight < DIP_THRESHOLD_COMPACT_LAYOUT;
        }
        return false;
    }

    private PendingIntent getDataChangedPendingIntent(Context context) {
        return PendingIntent.getBroadcast(context, REQUEST_CODE, getDataChangedIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Intent getDataChangedIntent(Context context) {
        // use explicit intent to work around implicit broadcast restrictions on O+
        Intent intent = new Intent(context, ListWidgetProvider.class);
        intent.setAction(ACTION_DATA_CHANGED);
        return intent;
    }

}
