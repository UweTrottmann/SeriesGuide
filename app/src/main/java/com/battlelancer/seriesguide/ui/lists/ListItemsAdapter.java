package com.battlelancer.seriesguide.ui.lists;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.shows.BaseShowsAdapter;
import com.battlelancer.seriesguide.util.ImageTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;

/**
 * Displays shows of a list.
 */
class ListItemsAdapter extends BaseShowsAdapter {

    ListItemsAdapter(Activity activity, OnItemClickListener listener) {
        super(activity, listener);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Support for seasons and episodes was removed, only shows are supported.
        final int itemType = cursor.getInt(Query.ITEM_TYPE);
        if (itemType != ListItemTypes.TVDB_SHOW && itemType != ListItemTypes.TMDB_SHOW) {
            throw new IllegalArgumentException("List item type is not supported: " + itemType);
        }

        ListItemViewHolder viewHolder = (ListItemViewHolder) view.getTag();

        viewHolder.showId = cursor.getLong(Query.SHOW_ID);
        viewHolder.isFavorited = cursor.getInt(Query.SHOW_FAVORITE) == 1;

        // show title
        viewHolder.name.setText(cursor.getString(Query.SHOW_TITLE));

        // favorite label
        setFavoriteState(viewHolder.favorited, viewHolder.isFavorited);

        // show details
        int time = cursor.getInt(Query.SHOW_RELEASE_TIME);
        int weekDay = cursor.getInt(Query.SHOW_RELEASE_WEEKDAY);
        String timeZone = cursor.getString(Query.SHOW_RELEASE_TIMEZONE);
        String country = cursor.getString(Query.SHOW_RELEASE_COUNTRY);
        String network = cursor.getString(Query.SHOW_NETWORK);

        Date releaseTimeShow;
        if (time != -1) {
            releaseTimeShow = TimeTools.getShowReleaseDateTime(context, time, weekDay,
                    timeZone, country, network);
        } else {
            releaseTimeShow = null;
        }

        // network, regular day and time
        viewHolder.timeAndNetwork.setText(
                TextTools.networkAndTime(context, releaseTimeShow, weekDay, network));

        // next episode info
        String fieldValue = cursor.getString(Query.SHOW_NEXTTEXT);
        if (TextUtils.isEmpty(fieldValue)) {
            // display show status if there is no next episode
            viewHolder.episodeTime.setText(SgApp.getServicesComponent(context).showTools()
                    .getStatus(cursor.getInt(Query.SHOW_STATUS)));
            viewHolder.episode.setText(null);
        } else {
            viewHolder.episode.setText(fieldValue);

            Date releaseTimeEpisode = TimeTools.applyUserOffset(context,
                    cursor.getLong(Query.SHOW_NEXT_DATE_MS));
            boolean displayExactDate = DisplaySettings.isDisplayExactDate(context);
            String dateTime = displayExactDate ?
                    TimeTools.formatToLocalDateShort(context, releaseTimeEpisode)
                    : TimeTools.formatToLocalRelativeTime(context, releaseTimeEpisode);
            if (TimeTools.isSameWeekDay(releaseTimeEpisode, releaseTimeShow, weekDay)) {
                // just display date
                viewHolder.episodeTime.setText(dateTime);
            } else {
                // display date and explicitly day
                viewHolder.episodeTime.setText(
                        context.getString(R.string.format_date_and_day, dateTime,
                                TimeTools.formatToLocalDay(releaseTimeEpisode)));
            }
        }

        // remaining count
        setRemainingCount(viewHolder.remainingCount, cursor.getInt(Query.SHOW_UNWATCHED_COUNT));

        // poster
        ImageTools.loadShowPosterResizeCrop(context, viewHolder.poster,
                cursor.getString(Query.SHOW_POSTER_SMALL));

        // context menu
        viewHolder.itemType = itemType;
        viewHolder.itemId = cursor.getString(Query.LIST_ITEM_ID);
        viewHolder.itemStableId = cursor.getInt(Query.ITEM_REF_ID);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_show, parent, false);

        ListItemViewHolder viewHolder = new ListItemViewHolder(v, onItemClickListener);
        v.setTag(viewHolder);

        return v;
    }

    public static class ListItemViewHolder extends ShowViewHolder {

        public String itemId;
        public int itemStableId;
        public int itemType;

        public ListItemViewHolder(View v, OnItemClickListener onItemClickListener) {
            super(v, onItemClickListener);
        }
    }

    public interface Query {

        String[] PROJECTION = new String[] {
                ListItems._ID, // 0
                ListItems.LIST_ITEM_ID,
                ListItems.ITEM_REF_ID,
                ListItems.TYPE,
                SgShow2Columns.REF_SHOW_ID,
                SgShow2Columns.TITLE, // 5
                SgShow2Columns.POSTER_SMALL,
                SgShow2Columns.NETWORK,
                SgShow2Columns.RELEASE_TIME,
                SgShow2Columns.RELEASE_WEEKDAY,
                SgShow2Columns.RELEASE_TIMEZONE, // 10
                SgShow2Columns.RELEASE_COUNTRY,
                SgShow2Columns.STATUS,
                SgShow2Columns.NEXTTEXT,
                SgShow2Columns.NEXTAIRDATEMS,
                SgShow2Columns.FAVORITE, // 15
                SgShow2Columns.UNWATCHED_COUNT
        };

        int LIST_ITEM_ID = 1;
        int ITEM_REF_ID = 2;
        int ITEM_TYPE = 3;
        int SHOW_ID = 4;
        int SHOW_TITLE = 5;
        int SHOW_POSTER_SMALL = 6;
        int SHOW_NETWORK = 7;
        int SHOW_RELEASE_TIME = 8;
        int SHOW_RELEASE_WEEKDAY = 9;
        int SHOW_RELEASE_TIMEZONE = 10;
        int SHOW_RELEASE_COUNTRY = 11;
        int SHOW_STATUS = 12;
        int SHOW_NEXTTEXT = 13;
        int SHOW_NEXT_DATE_MS = 14;
        int SHOW_FAVORITE = 15;
        int SHOW_UNWATCHED_COUNT = 16;
    }
}
