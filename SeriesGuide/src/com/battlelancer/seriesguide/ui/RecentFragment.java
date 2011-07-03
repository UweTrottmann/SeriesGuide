
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.AnalyticsUtils;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import java.util.Date;

public class RecentFragment extends UpcomingFragment {
    
    protected static final int LOADER_ID = 1;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setupAdapter();

        getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }
    
    @Override
    protected void onTrackPageView() {
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Recent");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Date date = new Date();
        String today = SeriesGuideData.theTVDBDateFormat.format(date);
        return new CursorLoader(getActivity(), Episodes.CONTENT_URI_WITHSHOW,
                UpcomingQuery.PROJECTION, Episodes.FIRSTAIRED + "<?", new String[] {
                    today
                }, RecentQuery.sortOrder);
    }

    interface RecentQuery {

        String sortOrder = Episodes.FIRSTAIRED + " DESC," + Shows.AIRSTIME + " ASC," + Shows.TITLE
                + " ASC";
    }

}
