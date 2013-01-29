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
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.battlelancer.seriesguide.enums.WidgetListType;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.ui.UpcomingFragment.UpcomingQuery;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

@TargetApi(11)
public class ListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private Context mContext;

        private int mAppWidgetId;

        private Cursor mEpisodeCursor;

        private WidgetListType mType;

        private boolean mIsOnlyUnwatched;

        public ListRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate() {
            // In onCreate() you setup any connections / cursors to your data
            // source. Heavy lifting, for example downloading or creating
            // content etc, should be deferred to onDataSetChanged() or
            // getViewAt(). Taking more than 20 seconds in this call will result
            // in an ANR.
            final SharedPreferences prefs = getSharedPreferences(ListWidgetConfigure.PREFS_NAME, 0);
            final int typeIndex = prefs.getInt(
                    ListWidgetConfigure.PREF_LISTTYPE_KEY + mAppWidgetId,
                    WidgetListType.UPCOMING.index);
            if (typeIndex == WidgetListType.RECENT.index) {
                mType = WidgetListType.RECENT;
            } else {
                mType = WidgetListType.UPCOMING;
            }
            mIsOnlyUnwatched = prefs.getBoolean(ListWidgetConfigure.PREF_WATCHEDONLY_KEY
                    + mAppWidgetId, false);

            queryForData();
        }

        private void queryForData() {
            switch (mType) {
                case RECENT:
                    mEpisodeCursor = DBUtils.getRecentEpisodes(mIsOnlyUnwatched, mContext);
                    break;
                case UPCOMING:
                default:
                    mEpisodeCursor = DBUtils.getUpcomingEpisodes(mIsOnlyUnwatched, mContext);
                    break;
            }
        }

        public void onDestroy() {
            // In onDestroy() you should tear down anything that was setup for
            // your data source, eg. cursors, connections, etc.
            mEpisodeCursor.close();
        }

        public int getCount() {
            return mEpisodeCursor.getCount();
        }

        public RemoteViews getViewAt(int position) {
            // We construct a remote views item based on our widget item xml
            // file, and set the text based on the position.
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.appwidget_row);

            if (mEpisodeCursor.isClosed()) {
                return rv;
            }
            // position will always range from 0 to getCount() - 1.
            mEpisodeCursor.moveToPosition(position);

            // episode description
            int seasonNumber = mEpisodeCursor.getInt(UpcomingQuery.SEASON);
            int episodeNumber = mEpisodeCursor.getInt(UpcomingQuery.NUMBER);
            String title = mEpisodeCursor.getString(UpcomingQuery.TITLE);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            rv.setTextViewText(R.id.textViewWidgetEpisode,
                    Utils.getNextEpisodeString(prefs, seasonNumber, episodeNumber, title));

            // relative airtime
            long airtime = mEpisodeCursor.getLong(UpcomingQuery.FIRSTAIREDMS);
            String[] dayAndTime = Utils.formatToTimeAndDay(airtime, mContext);
            String value = dayAndTime[2] + " (" + dayAndTime[1] + ")";
            rv.setTextViewText(R.id.widgetAirtime, value);

            // absolute airtime and network (if any)
            value = dayAndTime[0];
            String network = mEpisodeCursor.getString(UpcomingQuery.SHOW_NETWORK);
            if (network.length() != 0) {
                value += " " + network;
            }
            rv.setTextViewText(R.id.widgetNetwork, value);

            // show name
            value = mEpisodeCursor.getString(UpcomingQuery.SHOW_TITLE);
            rv.setTextViewText(R.id.textViewWidgetShow, value);

            // show poster
            value = mEpisodeCursor.getString(UpcomingQuery.SHOW_POSTER);
            final Bitmap poster = ImageProvider.getInstance(mContext).getImage(value, true);
            if (poster != null) {
                rv.setImageViewBitmap(R.id.widgetPoster, poster);
            } else {
                rv.setImageViewResource(R.id.widgetPoster, R.drawable.show_generic);
            }

            // Set the fill-in intent for the list items
            Bundle extras = new Bundle();
            extras.putInt(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                    mEpisodeCursor.getInt(UpcomingQuery._ID));
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.appwidget_row, fillInIntent);

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
            if (mEpisodeCursor != null) {
                mEpisodeCursor.close();
            }
            queryForData();
        }
    }
}
