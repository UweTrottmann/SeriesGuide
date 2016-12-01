package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;

/**
 * Loads a list of episodes released today and wraps them in a list of {@link
 * com.battlelancer.seriesguide.adapters.NowAdapter.NowItem} for use with {@link
 * com.battlelancer.seriesguide.adapters.NowAdapter}.
 */
public class ReleasedTodayLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    public ReleasedTodayLoader(Context context) {
        super(context);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        // go to start of current day
        long timeAtStartOfDay = new DateTime().withTimeAtStartOfDay().getMillis();
        // modify time to meet any user offset on episode release instants
        timeAtStartOfDay = TimeTools.applyUserOffsetInverted(getContext(), timeAtStartOfDay);

        Cursor query = getContext().getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                Query.PROJECTION, Query.SELECTION,
                new String[] {
                        String.valueOf(timeAtStartOfDay),
                        String.valueOf(timeAtStartOfDay + DateUtils.DAY_IN_MILLIS)
                },
                Query.SORT_ORDER);
        if (query != null) {
            List<NowAdapter.NowItem> items = new ArrayList<>();

            // add header
            if (query.getCount() > 0) {
                items.add(new NowAdapter.NowItem().header(
                        getContext().getString(R.string.released_today)));
            }

            // add items
            boolean preventSpoilers = DisplaySettings.preventSpoilers(getContext());
            while (query.moveToNext()) {
                String episodeString;
                int season = query.getInt(Query.SEASON);
                int episode = query.getInt(Query.NUMBER);
                int episodeFlag = query.getInt(Query.WATCHED);
                if (EpisodeTools.isUnwatched(episodeFlag) && preventSpoilers) {
                    // just display the number
                    episodeString = TextTools.getEpisodeNumber(getContext(), season, episode);
                } else {
                    // display number and title
                    episodeString = TextTools.getNextEpisodeString(getContext(), season, episode,
                            query.getString(Query.EPISODE_TITLE));
                }
                NowAdapter.NowItem item = new NowAdapter.NowItem()
                        .displayData(
                                query.getLong(Query.FIRSTAIREDMS),
                                query.getString(Query.SHOW_TITLE),
                                episodeString,
                                query.getString(Query.POSTER)
                        )
                        .tvdbIds(query.getInt(Query.EPISODE_TVDBID),
                                query.getInt(Query.SHOW_TVDBID))
                        .releasedToday(query.getString(Query.NETWORK));

                items.add(item);
            }

            query.close();

            return items;
        }

        return null;
    }

    private interface Query {
        String SELECTION = Episodes.FIRSTAIREDMS + ">=? AND "
                + Episodes.FIRSTAIREDMS + "<? AND "
                + Shows.SELECTION_NO_HIDDEN;

        String SORT_ORDER = Episodes.FIRSTAIREDMS + " DESC,"
                + Shows.SORT_TITLE + ","
                + Episodes.NUMBER + " DESC";

        String[] PROJECTION = new String[] {
                SeriesGuideDatabase.Tables.EPISODES + "." + Episodes._ID, // 0
                Episodes.TITLE,
                Episodes.NUMBER,
                Episodes.SEASON, // 3
                Episodes.FIRSTAIREDMS,
                Episodes.WATCHED,
                Shows.REF_SHOW_ID, // 6
                Shows.TITLE,
                Shows.NETWORK,
                Shows.POSTER // 9
        };

        int EPISODE_TVDBID = 0;
        int EPISODE_TITLE = 1;
        int NUMBER = 2;
        int SEASON = 3;
        int FIRSTAIREDMS = 4;
        int WATCHED = 5;
        int SHOW_TVDBID = 6;
        int SHOW_TITLE = 7;
        int NETWORK = 8;
        int POSTER = 9;
    }
}
