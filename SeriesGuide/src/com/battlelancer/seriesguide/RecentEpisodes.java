
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity;
import com.battlelancer.seriesguide.util.AnalyticsUtils;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.Date;

public class RecentEpisodes extends UpcomingEpisodes {

    @Override
    protected void onTrackPageView() {
        AnalyticsUtils.getInstance(this).trackPageView("/Recent");
    }

    protected final OnItemClickListener onItemClickListener = new OnItemClickListener() {

        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            Intent i = new Intent(RecentEpisodes.this, EpisodeDetailsActivity.class);
            i.putExtra(Episodes._ID, String.valueOf(id));
            startActivity(i);
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Date date = new Date();
        String today = SeriesGuideData.theTVDBDateFormat.format(date);
        return new CursorLoader(this, Episodes.CONTENT_URI_WITHSHOW, UpcomingQuery.PROJECTION,
                Episodes.FIRSTAIRED + "<?", new String[] {
                    today
                }, RecentQuery.sortOrder);
    }

    interface RecentQuery {

        String sortOrder = Episodes.FIRSTAIRED + " DESC," + Shows.AIRSTIME + " ASC," + Shows.TITLE
                + " ASC";
    }
}
