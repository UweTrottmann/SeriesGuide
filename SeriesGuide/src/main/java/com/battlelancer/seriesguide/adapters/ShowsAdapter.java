package com.battlelancer.seriesguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.View;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;

/**
 * Adapter for show items.
 */
public class ShowsAdapter extends BaseShowsAdapter {

    public ShowsAdapter(Activity activity, OnItemClickListener listener) {
        super(activity, listener);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ShowViewHolder viewHolder = (ShowViewHolder) view.getTag();

        viewHolder.showTvdbId = cursor.getInt(Query._ID);
        viewHolder.isFavorited = cursor.getInt(Query.FAVORITE) == 1;

        // set text properties immediately
        viewHolder.name.setText(cursor.getString(Query.TITLE));

        // favorite label
        setFavoriteState(viewHolder.favorited, viewHolder.isFavorited);

        int weekDay = cursor.getInt(Query.RELEASE_WEEKDAY);
        int time = cursor.getInt(Query.RELEASE_TIME);
        String timeZone = cursor.getString(Query.RELEASE_TIMEZONE);
        String country = cursor.getString(Query.RELEASE_COUNTRY);
        String network = cursor.getString(Query.NETWORK);
        Date releaseTimeShow;
        if (time != -1) {
            releaseTimeShow = TimeTools.getShowReleaseDateTime(context, time, weekDay, timeZone,
                    country, network);
        } else {
            releaseTimeShow = null;
        }

        // next episode info
        String fieldValue = cursor.getString(Query.NEXTTEXT);
        if (TextUtils.isEmpty(fieldValue)) {
            // display show status if there is no next episode
            viewHolder.episodeTime.setText(
                    ShowTools.getStatus(context, cursor.getInt(Query.STATUS)));
            viewHolder.episode.setText("");
        } else {
            viewHolder.episode.setText(fieldValue);

            Date releaseTimeEpisode = TimeTools.applyUserOffset(context,
                    cursor.getLong(Query.NEXTAIRDATEMS));
            boolean displayExactDate = DisplaySettings.isDisplayExactDate(context);
            String dateTime = displayExactDate ?
                    TimeTools.formatToLocalDateShort(context, releaseTimeEpisode)
                    : TimeTools.formatToLocalRelativeTime(context, releaseTimeEpisode);
            if (TimeTools.isSameWeekDay(releaseTimeEpisode, releaseTimeShow, weekDay)) {
                // just display date
                viewHolder.episodeTime.setText(dateTime);
            } else {
                // display date and explicitly day
                viewHolder.episodeTime.setText(context.getString(R.string.format_date_and_day,
                        dateTime, TimeTools.formatToLocalDay(releaseTimeEpisode)));
            }
        }

        // remaining count, network, day and time
        viewHolder.timeAndNetwork.setText(TextTools.remainingAndNetworkAndTime(
                context, makeRemainingCount(viewHolder.timeAndNetwork, cursor.getInt(Query.UNWATCHED_COUNT)),
                releaseTimeShow, weekDay, network));


        // set poster
        TvdbImageTools.loadShowPosterResizeCrop(context, viewHolder.poster,
                cursor.getString(Query.POSTER));

        // context menu
        viewHolder.isHidden = DBUtils.restoreBooleanFromInt(cursor.getInt(Query.HIDDEN));
        viewHolder.episodeTvdbId = cursor.getInt(Query.NEXTEPISODE);
    }

    public interface Query {

        String[] PROJECTION = {
                BaseColumns._ID, // 0
                SeriesGuideContract.Shows.TITLE,
                SeriesGuideContract.Shows.RELEASE_TIME,
                SeriesGuideContract.Shows.RELEASE_WEEKDAY,
                SeriesGuideContract.Shows.RELEASE_TIMEZONE,
                SeriesGuideContract.Shows.RELEASE_COUNTRY, // 5
                SeriesGuideContract.Shows.NETWORK,
                SeriesGuideContract.Shows.POSTER,
                SeriesGuideContract.Shows.STATUS,
                SeriesGuideContract.Shows.NEXTEPISODE,
                SeriesGuideContract.Shows.NEXTTEXT, // 10
                SeriesGuideContract.Shows.NEXTAIRDATEMS,
                SeriesGuideContract.Shows.FAVORITE,
                SeriesGuideContract.Shows.HIDDEN,
                SeriesGuideContract.Shows.UNWATCHED_COUNT // 14
        };

        int _ID = 0;
        int TITLE = 1;
        int RELEASE_TIME = 2;
        int RELEASE_WEEKDAY = 3;
        int RELEASE_TIMEZONE = 4;
        int RELEASE_COUNTRY = 5;
        int NETWORK = 6;
        int POSTER = 7;
        int STATUS = 8;
        int NEXTEPISODE = 9;
        int NEXTTEXT = 10;
        int NEXTAIRDATEMS = 11;
        int FAVORITE = 12;
        int HIDDEN = 13;
        int UNWATCHED_COUNT = 14;
    }
}
