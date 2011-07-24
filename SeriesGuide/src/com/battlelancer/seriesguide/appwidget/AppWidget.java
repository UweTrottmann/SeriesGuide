/*
 * Copyright (C) 2011 Uwe Trottmann 
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

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesDatabase;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.UpcomingRecent;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class AppWidget extends AppWidgetProvider {
    public static final String REFRESH = "com.battlelancer.seriesguide.appwidget.REFRESH";

    private static final String LIMIT = "2";

    private static final int LAYOUT = R.layout.appwidget;

    private static final int ITEMLAYOUT = R.layout.appwidget_item;

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
     * 
     * @param context
     * @return
     */
    public Intent createUpdateIntent(Context context) {
        Intent i = new Intent(context, UpdateService.class);
        return i;
    }

    public static class UpdateService extends IntentService {

        public UpdateService() {
            super("appwidget.AppWidget$UpdateService");
        }

        @Override
        public void onCreate() {
            super.onCreate();
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
            ImageCache imageCache = ((SeriesGuideApplication) getApplication()).getImageCache();

            // Get the layout for the App Widget, remove existing views
            // RemoteViews views = new RemoteViews(context.getPackageName(),
            // layout);
            RemoteViews views = new RemoteViews(context.getPackageName(), layout);

            views.removeAllViews(R.id.LinearLayoutWidget);
            

            // get upcoming shows (name and next episode text)
            Cursor upcomingEpisodes = SeriesDatabase.getUpcomingEpisodes(context);

            if (upcomingEpisodes.getCount() == 0) {
                // no next episodes exist
                RemoteViews item = new RemoteViews(context.getPackageName(), itemLayout);
                item.setTextViewText(R.id.textViewWidgetShow,
                        context.getString(R.string.no_nextepisode));
                item.setTextViewText(R.id.textViewWidgetEpisode, "");
                item.setTextViewText(R.id.widgetAirtime, "");
                item.setTextViewText(R.id.widgetNetwork, "");
                views.addView(R.id.LinearLayoutWidget, item);
            } else {
                String value;
                Bitmap poster;

                int viewsToAdd = Integer.valueOf(limit);
                while (upcomingEpisodes.moveToNext() && viewsToAdd != 0) {
                    viewsToAdd--;
                    
                    RemoteViews item = new RemoteViews(context.getPackageName(), itemLayout);
                    // upcoming episode
                    String season = upcomingEpisodes.getString(upcomingEpisodes
                            .getColumnIndexOrThrow(SeriesDatabase.EPISODE_SEASON));
                    String number = upcomingEpisodes.getString(upcomingEpisodes
                            .getColumnIndexOrThrow(SeriesDatabase.EPISODE_NUMBER));
                    String title = upcomingEpisodes.getString(upcomingEpisodes
                            .getColumnIndexOrThrow(SeriesDatabase.EPISODE_TITLE));
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    item.setTextViewText(R.id.textViewWidgetEpisode,
                            SeriesGuideData.getNextEpisodeString(prefs, season, number, title));

                    // add relative airdate
                    long airtime = upcomingEpisodes.getLong(upcomingEpisodes
                            .getColumnIndexOrThrow(SeriesDatabase.SERIES_AIRSTIME));
                    value = SeriesGuideData.parseDateToLocalRelative(upcomingEpisodes
                            .getString(upcomingEpisodes
                                    .getColumnIndexOrThrow(SeriesDatabase.EPISODE_FIRSTAIRED)),
                            airtime, context);
                    item.setTextViewText(R.id.widgetAirtime, value);

                    // add airtime and network (if any)
                    value = "";
                    if (airtime != -1) {
                        value = SeriesGuideData.parseMillisecondsToTime(airtime, null,
                                getApplicationContext())[0];
                    }
                    String network = upcomingEpisodes.getString(upcomingEpisodes
                            .getColumnIndexOrThrow(SeriesDatabase.SERIES_NETWORK));
                    if (network.length() != 0) {
                        value += " " + getString(R.string.show_network) + " " + network;
                    }
                    item.setTextViewText(R.id.widgetNetwork, value);

                    // show name
                    value = upcomingEpisodes.getString(upcomingEpisodes
                            .getColumnIndexOrThrow(SeriesDatabase.SERIES_NAME));
                    item.setTextViewText(R.id.textViewWidgetShow, value);

                    if (layout != R.layout.appwidget) {
                        // show poster
                        value = upcomingEpisodes.getString(upcomingEpisodes
                                .getColumnIndexOrThrow(SeriesDatabase.SERIES_POSTER));
                        poster = null;
                        if (value.length() != 0) {
                            poster = imageCache.getThumb(value);

                            if (poster != null) {
                                item.setImageViewBitmap(R.id.widgetPoster, poster);
                            }
                        }
                    }
                    
                    views.addView(R.id.LinearLayoutWidget, item);
                }
            }

            upcomingEpisodes.close();

            // Create an Intent to launch Upcoming
            Intent intent = new Intent(context, UpcomingRecent.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.LinearLayoutWidget, pendingIntent);

            if (layout != R.layout.appwidget) {
                // Create an Intent to launch SeriesGuide
                Intent i = new Intent(context, ShowsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
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
