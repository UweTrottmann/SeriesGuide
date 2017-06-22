package com.battlelancer.seriesguide.appwidget;

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
import android.text.TextUtils;
import android.widget.RemoteViews;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.CalendarAdapter;
import com.battlelancer.seriesguide.settings.CalendarSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import java.io.IOException;
import java.util.Date;
import timber.log.Timber;

public class AppWidget extends AppWidgetProvider {

    public static final String REFRESH = "com.battlelancer.seriesguide.appwidget.REFRESH";

    private static final String LIMIT = "1";

    private static final int LAYOUT = R.layout.appwidget;

    private static final int ITEMLAYOUT = R.layout.appwidget_big_item;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            // check for null as super.onReceive does not
            // we only need to guard here as other methods are only called by super.onReceive
            return;
        }

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
            final Cursor upcomingEpisodes = DBUtils.upcomingEpisodesQuery(context,
                    CalendarSettings.isHidingWatchedEpisodes(context));

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
                boolean displayExactDate = DisplaySettings.isDisplayExactDate(context);
                boolean preventSpoilers = DisplaySettings.preventSpoilers(context);

                int viewsToAdd = Integer.valueOf(limit);
                while (upcomingEpisodes.moveToNext() && viewsToAdd != 0) {
                    viewsToAdd--;

                    RemoteViews item = new RemoteViews(context.getPackageName(), itemLayout);
                    // upcoming episode
                    int seasonNumber = upcomingEpisodes.getInt(CalendarAdapter.Query.SEASON);
                    int episodeNumber = upcomingEpisodes.getInt(CalendarAdapter.Query.NUMBER);
                    String title = upcomingEpisodes.getString(CalendarAdapter.Query.TITLE);
                    int watchedFlag = upcomingEpisodes.getInt(CalendarAdapter.Query.WATCHED);
                    String nextEpisodeString;
                    if (EpisodeTools.isUnwatched(watchedFlag) && preventSpoilers) {
                        // just display the episode number
                        nextEpisodeString = TextTools.getEpisodeNumber(context, seasonNumber,
                                episodeNumber);
                    } else {
                        // display episode number and title
                        nextEpisodeString = TextTools.getNextEpisodeString(context, seasonNumber,
                                episodeNumber, title);
                    }
                    item.setTextViewText(R.id.textViewWidgetEpisode, nextEpisodeString);

                    Date actualRelease = TimeTools.applyUserOffset(context,
                            upcomingEpisodes.getLong(CalendarAdapter.Query.RELEASE_TIME_MS)
                    );

                    // "Oct 31 (Fri)" or "in 13 mins (Fri)"
                    String dateTime = displayExactDate ?
                            TimeTools.formatToLocalDateShort(context, actualRelease)
                            : TimeTools.formatToLocalRelativeTime(context, actualRelease);
                    item.setTextViewText(R.id.widgetAirtime,
                            getString(R.string.format_date_and_day,
                                    dateTime,
                                    TimeTools.formatToLocalDay(actualRelease))
                    );

                    // absolute release time and network (if any)
                    String releaseTime = TimeTools.formatToLocalTime(context, actualRelease);
                    String network = upcomingEpisodes.getString(
                            CalendarAdapter.Query.SHOW_NETWORK);
                    if (!TextUtils.isEmpty(network)) {
                        releaseTime += " " + network;
                    }
                    item.setTextViewText(R.id.widgetNetwork, releaseTime);

                    // show title
                    item.setTextViewText(R.id.textViewWidgetShow,
                            upcomingEpisodes.getString(CalendarAdapter.Query.SHOW_TITLE));

                    // show poster
                    String posterPath = upcomingEpisodes.getString(
                            CalendarAdapter.Query.SHOW_POSTER_PATH);
                    maybeSetPoster(item, posterPath);

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

            // Create an intent to update the widget
            updateIntent.setAction(REFRESH);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, updateIntent, 0);
            views.setOnClickPendingIntent(R.id.ImageButtonWidget, pi);

            return views;
        }

        private void maybeSetPoster(RemoteViews item, String posterPath) {
            try {
                Bitmap poster = ServiceUtils.loadWithPicasso(this,
                        TvdbImageTools.smallSizeUrl(posterPath))
                        .centerCrop()
                        .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
                        .get();
                item.setImageViewBitmap(R.id.widgetPoster, poster);
            } catch (IOException e) {
                Timber.e(e, "maybeSetPoster: failed.");
            }
        }
    }
}
