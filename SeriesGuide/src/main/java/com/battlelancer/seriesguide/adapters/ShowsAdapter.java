package com.battlelancer.seriesguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.View;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Adapter for show items.
 */
public class ShowsAdapter extends BaseShowsAdapter {

    public ShowsAdapter(Activity activity, OnContextMenuClickListener listener) {
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

        // next episode info
        String fieldValue = cursor.getString(Query.NEXTTEXT);
        if (TextUtils.isEmpty(fieldValue)) {
            // display show status if there is no next episode
            viewHolder.episodeTime.setText(
                    ShowTools.getStatus(context, cursor.getInt(Query.STATUS)));
            viewHolder.episode.setText("");
        } else {
            viewHolder.episode.setText(fieldValue);
            fieldValue = cursor.getString(Query.NEXTAIRDATETEXT);
            viewHolder.episodeTime.setText(fieldValue);
        }

        setRemainingCount(context, viewHolder.remainingCount, cursor.getInt(Query.UNWATCHED_COUNT));

        // network, day and time
        viewHolder.timeAndNetwork.setText(buildNetworkAndTimeString(context,
                cursor.getInt(Query.RELEASE_TIME),
                cursor.getInt(Query.RELEASE_WEEKDAY),
                cursor.getString(Query.RELEASE_TIMEZONE),
                cursor.getString(Query.RELEASE_COUNTRY),
                cursor.getString(Query.NETWORK)));

        // set poster
        Utils.loadTvdbShowPoster(context, viewHolder.poster,
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
                SeriesGuideContract.Shows.NEXTAIRDATETEXT,
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
        int NEXTAIRDATETEXT = 11;
        int FAVORITE = 12;
        int HIDDEN = 13;
        int UNWATCHED_COUNT = 14;
    }
}
