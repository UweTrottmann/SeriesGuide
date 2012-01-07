
package com.battlelancer.seriesguide.appwidget;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class ListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private Context mContext;

        // private int mAppWidgetId;

        private Cursor mUpcomingEpisodes;

        public ListRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
            // mAppWidgetId =
            // intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            // AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate() {
            // In onCreate() you setup any connections / cursors to your data
            // source. Heavy lifting, for example downloading or creating
            // content etc, should be deferred to onDataSetChanged() or
            // getViewAt(). Taking more than 20 seconds in this call will result
            // in an ANR.
            mUpcomingEpisodes = DBUtils.getUpcomingEpisodes(mContext);
        }

        public void onDestroy() {
            // In onDestroy() you should tear down anything that was setup for
            // your data source, eg. cursors, connections, etc.
            mUpcomingEpisodes.close();
        }

        public int getCount() {
            return mUpcomingEpisodes.getCount();
        }

        public RemoteViews getViewAt(int position) {
            // position will always range from 0 to getCount() - 1.
            mUpcomingEpisodes.moveToPosition(position);

            // We construct a remote views item based on our widget item xml
            // file, and set the text based on the position.
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.appwidget_big_item);

            // episode description
            String season = mUpcomingEpisodes.getString(mUpcomingEpisodes
                    .getColumnIndexOrThrow(Episodes.SEASON));
            String number = mUpcomingEpisodes.getString(mUpcomingEpisodes
                    .getColumnIndexOrThrow(Episodes.NUMBER));
            String title = mUpcomingEpisodes.getString(mUpcomingEpisodes
                    .getColumnIndexOrThrow(Episodes.TITLE));
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            rv.setTextViewText(R.id.textViewWidgetEpisode,
                    Utils.getNextEpisodeString(prefs, season, number, title));

            // airdate
            long airtime = mUpcomingEpisodes.getLong(mUpcomingEpisodes
                    .getColumnIndexOrThrow(Shows.AIRSTIME));
            String value = Utils.parseDateToLocalRelative(mUpcomingEpisodes
                    .getString(mUpcomingEpisodes.getColumnIndexOrThrow(Episodes.FIRSTAIRED)),
                    airtime, mContext);
            rv.setTextViewText(R.id.widgetAirtime, value);

            // airtime and network (if any)
            value = "";
            if (airtime != -1) {
                value = Utils.parseMillisecondsToTime(airtime, null, getApplicationContext())[0];
            }
            String network = mUpcomingEpisodes.getString(mUpcomingEpisodes
                    .getColumnIndexOrThrow(Shows.NETWORK));
            if (network.length() != 0) {
                value += " " + network;
            }
            rv.setTextViewText(R.id.widgetNetwork, value);

            // show name
            value = mUpcomingEpisodes.getString(mUpcomingEpisodes
                    .getColumnIndexOrThrow(Shows.TITLE));
            rv.setTextViewText(R.id.textViewWidgetShow, value);

            // show poster
            value = mUpcomingEpisodes.getString(mUpcomingEpisodes
                    .getColumnIndexOrThrow(Shows.POSTER));
            Bitmap poster = null;
            if (value.length() != 0) {
                poster = ImageCache.getInstance(mContext).getThumb(value, false);

                if (poster != null) {
                    rv.setImageViewBitmap(R.id.widgetPoster, poster);
                }
            }

            // Set the fill-in intent for the list items
            Bundle extras = new Bundle();
            extras.putString(EpisodeDetailsActivity.InitBundle.EPISODE_ID,
                    mUpcomingEpisodes.getString(0));
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.appwidget_bit_item, fillInIntent);

            // Return the remote views object.
            return rv;
        }

        public RemoteViews getLoadingView() {
            // You can create a custom loading view (for instance when
            // getViewAt() is slow.) If you return null here, you will get the
            // default loading view.
            return null;
        }

        public int getViewTypeCount() {
            return 1;
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
            if (mUpcomingEpisodes != null) {
                mUpcomingEpisodes.close();
            }
            mUpcomingEpisodes = DBUtils.getUpcomingEpisodes(mContext);
        }
    }
}
