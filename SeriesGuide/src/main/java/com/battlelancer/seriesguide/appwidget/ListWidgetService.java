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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.CalendarAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Qualified;
import com.battlelancer.seriesguide.settings.WidgetSettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import java.io.IOException;
import java.util.Date;
import timber.log.Timber;

public class ListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private final Context context;
        private final int appWidgetId;

        private Cursor dataCursor;
        private int widgetType;
        private boolean isLightTheme;

        public ListRemoteViewsFactory(Context context, Intent intent) {
            this.context = context;
            this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate() {
            // Since we reload the cursor in onDataSetChanged() which gets called immediately after
            // onCreate(), we do nothing here.
        }

        private void onQueryForData() {
            boolean isOnlyFavorites = WidgetSettings.isOnlyFavoriteShows(context, appWidgetId);
            boolean isHideWatched = WidgetSettings.isHidingWatchedEpisodes(context, appWidgetId);
            int widgetType = WidgetSettings.getWidgetListType(context, appWidgetId);

            Cursor newCursor;
            switch (widgetType) {
                case WidgetSettings.Type.RECENT:
                    // Recent episodes
                    newCursor = DBUtils.getRecentEpisodes(context, isOnlyFavorites, isHideWatched);
                    break;
                case WidgetSettings.Type.FAVORITES:
                    // Favorite shows + next episodes, exclude those without
                    // episode
                    newCursor = getContentResolver().query(
                            Shows.CONTENT_URI_WITH_NEXT_EPISODE,
                            ShowsQuery.PROJECTION,
                            Shows.SELECTION_NO_HIDDEN + " AND " + Shows.SELECTION_FAVORITES
                                    + " AND " + Shows.SELECTION_WITH_NEXT_EPISODE, null,
                            Shows.DEFAULT_SORT);
                    break;
                default:
                    // Upcoming episodes
                    newCursor = DBUtils.getUpcomingEpisodes(context, isOnlyFavorites,
                            isHideWatched);
                    break;
            }

            if (newCursor == null) {
                // do NOT switch to null cursor
                return;
            }

            // switch out cursor
            Cursor oldCursor = dataCursor;

            this.dataCursor = newCursor;
            this.widgetType = widgetType;
            this.isLightTheme = WidgetSettings.isLightTheme(context, appWidgetId);

            if (oldCursor != null) {
                oldCursor.close();
            }
        }

        public void onDestroy() {
            // In onDestroy() you should tear down anything that was setup for
            // your data source, eg. cursors, connections, etc.
            if (dataCursor != null) {
                dataCursor.close();
            }
        }

        public int getCount() {
            if (dataCursor != null) {
                return dataCursor.getCount();
            } else {
                return 0;
            }
        }

        public RemoteViews getViewAt(int position) {
            final boolean isShowQuery = widgetType == WidgetSettings.Type.FAVORITES;

            // build a remote views collection item
            RemoteViews rv = new RemoteViews(context.getPackageName(),
                    isLightTheme ? R.layout.appwidget_row_light : R.layout.appwidget_row);

            // return empty item if no data available
            if (dataCursor == null
                    || dataCursor.isClosed() || !dataCursor.moveToPosition(position)) {
                return rv;
            }

            // set the fill-in intent for the collection item
            Bundle extras = new Bundle();
            extras.putInt(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                    dataCursor.getInt(isShowQuery ?
                            ShowsQuery.SHOW_NEXT_EPISODE_ID : CalendarAdapter.Query._ID));
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.appwidget_row, fillInIntent);

            // episode description
            int seasonNumber = dataCursor.getInt(isShowQuery ?
                    ShowsQuery.EPISODE_SEASON : CalendarAdapter.Query.SEASON);
            int episodeNumber = dataCursor.getInt(isShowQuery ?
                    ShowsQuery.EPISODE_NUMBER : CalendarAdapter.Query.NUMBER);
            String title = dataCursor.getString(isShowQuery ?
                    ShowsQuery.EPISODE_TITLE : CalendarAdapter.Query.TITLE);
            rv.setTextViewText(R.id.textViewWidgetEpisode,
                    Utils.getNextEpisodeString(context, seasonNumber, episodeNumber, title));

            // relative release time
            Date actualRelease = TimeTools.applyUserOffset(context,
                    dataCursor.getLong(isShowQuery ?
                            ShowsQuery.EPISODE_FIRSTAIRED_MS
                            : CalendarAdapter.Query.RELEASE_TIME_MS));
            // "Fri 2 days ago"
            rv.setTextViewText(R.id.widgetAirtime,
                    TimeTools.formatToLocalDayAndRelativeTime(context, actualRelease));

            // absolute release time and network (if any)
            String absoluteTime = TimeTools.formatToLocalTime(context, actualRelease);
            String network = dataCursor.getString(isShowQuery ?
                    ShowsQuery.SHOW_NETWORK : CalendarAdapter.Query.SHOW_NETWORK);
            if (!TextUtils.isEmpty(network)) {
                absoluteTime += " " + network;
            }
            rv.setTextViewText(R.id.widgetNetwork, absoluteTime);

            // show name
            rv.setTextViewText(R.id.textViewWidgetShow, dataCursor.getString(isShowQuery ?
                    ShowsQuery.SHOW_TITLE : CalendarAdapter.Query.SHOW_TITLE));

            // show poster
            String posterPath = dataCursor.getString(isShowQuery
                    ? ShowsQuery.SHOW_POSTER : CalendarAdapter.Query.SHOW_POSTER);
            Bitmap poster;
            try {
                poster = ServiceUtils.loadWithPicasso(context, TheTVDB.buildPosterUrl(posterPath))
                        .centerCrop()
                        .resizeDimen(R.dimen.widget_item_width, R.dimen.widget_item_height)
                        .get();
            } catch (IOException e) {
                Timber.w(e, "getViewAt: Loading show poster for widget item failed");
                poster = null;
            }
            if (poster != null) {
                rv.setImageViewBitmap(R.id.widgetPoster, poster);
            } else {
                rv.setImageViewResource(R.id.widgetPoster, R.drawable.ic_image_missing);
            }

            // Return the remote views object.
            return rv;
        }

        public RemoteViews getLoadingView() {
            // If you return null here, you will get the default loading view.
            // create a custom loading view
            return new RemoteViews(context.getPackageName(),
                    isLightTheme ? R.layout.appwidget_row_light : R.layout.appwidget_row);
        }

        public int getViewTypeCount() {
            // different view layout for default and light theme
            return 2;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return true;
        }

        public void onDataSetChanged() {
            // This is triggered when you call AppWidgetManager
            // notifyAppWidgetViewDataChanged
            // on the collection view corresponding to this factory. You can do
            // heaving lifting in
            // here, synchronously. For example, if you need to process an
            // image, fetch something
            // from the network, etc., it is ok to do it here, synchronously.
            // The widget will remain
            // in its current state while work is being done here, so you don't
            // need to worry about locking up the widget.
            onQueryForData();
        }
    }

    interface ShowsQuery {
        String[] PROJECTION = {
                Qualified.SHOWS_ID, Shows.TITLE, Shows.NETWORK, Shows.POSTER, Shows.STATUS,
                Shows.NEXTEPISODE, Episodes.TITLE, Episodes.NUMBER, Episodes.SEASON,
                Episodes.FIRSTAIREDMS
        };

        int SHOW_ID = 0;

        int SHOW_TITLE = 1;

        int SHOW_NETWORK = 2;

        int SHOW_POSTER = 3;

        int SHOW_STATUS = 4;

        int SHOW_NEXT_EPISODE_ID = 5;

        int EPISODE_TITLE = 6;

        int EPISODE_NUMBER = 7;

        int EPISODE_SEASON = 8;

        int EPISODE_FIRSTAIRED_MS = 9;
    }
}
