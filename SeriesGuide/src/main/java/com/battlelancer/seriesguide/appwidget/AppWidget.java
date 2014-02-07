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

import com.battlelancer.seriesguide.ui.ActivityFragment;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.R;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import java.util.Date;

public class AppWidget extends AppWidgetProvider {

    public static final String REFRESH = "com.battlelancer.seriesguide.appwidget.REFRESH";

    private static final String LIMIT = "1";

    private static final int LAYOUT = R.layout.appwidget;

    private static final int ITEMLAYOUT = R.layout.appwidget_big_item;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (REFRESH.equals(intent.getAction())) {
            context.startService(createUpdateIntent(context));
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(createUpdateIntent(context));
    }

    /**
     * Creates an intent which must include the update service class to start.
     */
    public Intent createUpdateIntent(Context context) {
        return new Intent(context, UpdateService.class);
    }

    public static class UpdateService extends IntentService {

        public UpdateService() {
            super("appwidget.AppWidget$UpdateService");
        }

        @Override
        public void onHandleIntent(Intent intent) {
            ComponentName me = new ComponentName(this, AppWidget.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);

            Intent i = new Intent(this, AppWidget.class);
            mgr.updateAppWidget(me, buildUpdate(this, LIMIT, LAYOUT, ITEMLAYOUT, i));
        }

        protected RemoteViews buildUpdate(Context context, String limit, int layout,
                int itemLayout, Intent updateIntent) {
            // Get the layout for the App Widget, remove existing views
            // RemoteViews views = new RemoteViews(context.getPackageName(),
            // layout);
            final RemoteViews views = new RemoteViews(context.getPackageName(), layout);

            views.removeAllViews(R.id.LinearLayoutWidget);

            // get upcoming shows (name and next episode text)
            final Cursor upcomingEpisodes = DBUtils.getUpcomingEpisodes(context);

            if (upcomingEpisodes == null || upcomingEpisodes.getCount() == 0) {
                // no next episodes exist
                RemoteViews item = new RemoteViews(context.getPackageName(), itemLayout);
                item.setTextViewText(R.id.textViewWidgetShow,
                        context.getString(R.string.no_nextepisode));
                item.setTextViewText(R.id.textViewWidgetEpisode, "");
                item.setTextViewText(R.id.widgetAirtime, "");
                item.setTextViewText(R.id.widgetNetwork, "");
                views.addView(R.id.LinearLayoutWidget, item);
            } else {
                int viewsToAdd = Integer.valueOf(limit);
                while (upcomingEpisodes.moveToNext() && viewsToAdd != 0) {
                    viewsToAdd--;

                    RemoteViews item = new RemoteViews(context.getPackageName(), itemLayout);
                    // upcoming episode
                    int seasonNumber = upcomingEpisodes.getInt(ActivityFragment.ActivityQuery.SEASON);
                    int episodeNumber = upcomingEpisodes.getInt(ActivityFragment.ActivityQuery.NUMBER);
                    String title = upcomingEpisodes.getString(ActivityFragment.ActivityQuery.TITLE);
                    item.setTextViewText(R.id.textViewWidgetEpisode,
                            Utils.getNextEpisodeString(this, seasonNumber, episodeNumber, title));

                    Date actualRelease = TimeTools.getEpisodeReleaseTime(context,
                            upcomingEpisodes.getLong(
                                    ActivityFragment.ActivityQuery.EPISODE_FIRST_RELEASE_MS));

                    // "in 13 mins (Fri)"
                    item.setTextViewText(R.id.widgetAirtime,
                            TimeTools.formatToRelativeLocalReleaseTime(actualRelease)
                                    + " (" + TimeTools.formatToLocalReleaseDay(actualRelease)
                                    + ")");

                    // absolute release time and network (if any)
                    String releaseTime = TimeTools.formatToLocalReleaseTime(context, actualRelease);
                    String network = upcomingEpisodes.getString(ActivityFragment.ActivityQuery.SHOW_NETWORK);
                    if (network.length() != 0) {
                        releaseTime += " " + network;
                    }
                    item.setTextViewText(R.id.widgetNetwork, releaseTime);

                    // show title
                    item.setTextViewText(R.id.textViewWidgetShow,
                            upcomingEpisodes.getString(ActivityFragment.ActivityQuery.SHOW_TITLE));

                    // show poster
                    String posterPath = upcomingEpisodes.getString(ActivityFragment.ActivityQuery.SHOW_POSTER);
                    final Bitmap poster = ImageProvider.getInstance(context).getImage(posterPath, true);
                    if (poster != null) {
                        item.setImageViewBitmap(R.id.widgetPoster, poster);
                    }

                    views.addView(R.id.LinearLayoutWidget, item);
                }
            }

            if (upcomingEpisodes != null) {
                upcomingEpisodes.close();
            }

            // Create an Intent to launch Upcoming
            Intent activityIntent = new Intent(context, ShowsActivity.class);
            activityIntent.putExtra(ShowsActivity.InitBundle.SELECTED_TAB,
                    ShowsActivity.InitBundle.INDEX_TAB_UPCOMING);
            PendingIntent activityPendingIntent = TaskStackBuilder
                    .create(context)
                    .addNextIntent(activityIntent)
                    .getPendingIntent(0, 0);
            views.setOnClickPendingIntent(R.id.LinearLayoutWidget, activityPendingIntent);

            if (layout != R.layout.appwidget) {
                // Create an Intent to launch SeriesGuide
                Intent launchIntent = new Intent(context, ShowsActivity.class);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent
                        .getActivity(context, 0, launchIntent, 0);
                views.setOnClickPendingIntent(R.id.widgetShowlistButton, pendingIntent);
            }

            // Create an intent to update the widget
            updateIntent.setAction(REFRESH);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, updateIntent, 0);
            views.setOnClickPendingIntent(R.id.ImageButtonWidget, pi);

            return views;
        }
    }
}
