package com.battlelancer.seriesguide.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.CalendarAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Qualified;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.ShowsDistillationSettings;
import com.battlelancer.seriesguide.settings.WidgetSettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
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

        @Override
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
                case WidgetSettings.Type.SHOWS:
                    // Shows
                    // not hidden
                    StringBuilder selection = new StringBuilder(Shows.SELECTION_NO_HIDDEN);

                    // optionally only favorites
                    if (isOnlyFavorites) {
                        selection.append(" AND ").append(Shows.SELECTION_FAVORITES);
                    }

                    // with next episode
                    selection.append(" AND ").append(Shows.SELECTION_WITH_RELEASED_NEXT_EPISODE);

                    // if next episode is in the future, exclude if too far into the future
                    final long timeInAnHour = System.currentTimeMillis() + DateUtils.HOUR_IN_MILLIS;
                    int upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(context);
                    long latestAirtime = timeInAnHour
                            + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS;
                    selection.append(" AND ")
                            .append(Shows.NEXTAIRDATEMS)
                            .append("<=")
                            .append(latestAirtime);

                    // query, sort based on user preference
                    newCursor = getContentResolver().query(
                            Shows.CONTENT_URI_WITH_NEXT_EPISODE,
                            ShowsQuery.PROJECTION,
                            selection.toString(),
                            null,
                            ShowsDistillationSettings.getSortQuery(
                                    WidgetSettings.getWidgetShowsSortOrderId(context, appWidgetId),
                                    false, DisplaySettings.isSortOrderIgnoringArticles(context))
                    );
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

        @Override
        public void onDestroy() {
            // In onDestroy() you should tear down anything that was setup for
            // your data source, eg. cursors, connections, etc.
            if (dataCursor != null) {
                dataCursor.close();
            }
        }

        @Override
        public int getCount() {
            if (dataCursor != null) {
                return dataCursor.getCount();
            } else {
                return 0;
            }
        }

        @Override
        public RemoteViews getViewAt(int position) {
            final boolean isShowQuery = widgetType == WidgetSettings.Type.SHOWS;

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
                    TextTools.getNextEpisodeString(context, seasonNumber, episodeNumber, title));

            // relative release time
            Date actualRelease = TimeTools.applyUserOffset(context,
                    dataCursor.getLong(isShowQuery ?
                            ShowsQuery.EPISODE_FIRSTAIRED_MS
                            : CalendarAdapter.Query.RELEASE_TIME_MS));
            // "Fri Oct 31" or "Fri 2 days ago"
            boolean displayExactDate = DisplaySettings.isDisplayExactDate(context);
            rv.setTextViewText(R.id.widgetAirtime, displayExactDate ?
                    TimeTools.formatToLocalDay(actualRelease) + " "
                            + TimeTools.formatToLocalDateShort(context, actualRelease)
                    : TimeTools.formatToLocalDayAndRelativeTime(context, actualRelease));

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
                poster = ServiceUtils.loadWithPicasso(context, TvdbTools.buildPosterUrl(posterPath))
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

        @Override
        public RemoteViews getLoadingView() {
            // If you return null here, you will get the default loading view.
            // create a custom loading view
            return new RemoteViews(context.getPackageName(),
                    isLightTheme ? R.layout.appwidget_row_light : R.layout.appwidget_row);
        }

        @Override
        public int getViewTypeCount() {
            // different view layout for default and light theme
            return 2;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
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
