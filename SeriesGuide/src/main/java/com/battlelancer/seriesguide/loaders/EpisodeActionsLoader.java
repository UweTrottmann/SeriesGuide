package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.database.Cursor;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;

/**
 * Tries returning existing actions for an episode. If no actions have been published, will ask
 * extensions to do so and returns an empty list.
 */
public class EpisodeActionsLoader extends GenericSimpleLoader<List<Action>> {

    private final int mEpisodeTvdbId;
    private Cursor mQuery;

    public EpisodeActionsLoader(Context context, int episodeTvdbId) {
        super(context);
        mEpisodeTvdbId = episodeTvdbId;
    }

    @Override
    public List<Action> loadInBackground() {
        List<Action> actions = ExtensionManager.getInstance(getContext())
                .getLatestEpisodeActions(mEpisodeTvdbId);

        // no actions available yet, request extensions to publish them
        if (actions == null || actions.size() == 0) {
            actions = new ArrayList<>();

            mQuery = getContext().getContentResolver().query(
                    Episodes.buildEpisodeWithShowUri(mEpisodeTvdbId),
                    Query.PROJECTION, null, null, null);
            if (mQuery == null || !mQuery.moveToFirst()) {
                return actions;
            }

            Episode episode = new Episode.Builder()
                    .tvdbId(mEpisodeTvdbId)
                    .title(mQuery.getString(Query.TITLE))
                    .number(mQuery.getInt(Query.NUMBER))
                    .numberAbsolute(mQuery.getInt(Query.NUMBER_ABSOLUTE))
                    .season(mQuery.getInt(Query.SEASON))
                    .imdbId(mQuery.getString(Query.IMDB_ID))
                    .showTvdbId(mQuery.getInt(Query.SHOW_TVDB_ID))
                    .showTitle(mQuery.getString(Query.SHOW_TITLE))
                    .showImdbId(mQuery.getString(Query.SHOW_IMDB_ID))
                    .build();

            mQuery.close();
            mQuery = null;

            ExtensionManager.getInstance(getContext()).requestEpisodeActions(episode);
        }

        return actions;
    }

    @Override
    protected void onReleaseResources(List<Action> items) {
        if (mQuery != null && !mQuery.isClosed()) {
            mQuery.close();
        }
    }

    private interface Query {
        String[] PROJECTION = {
                Episodes.TITLE,
                Episodes.NUMBER,
                Episodes.ABSOLUTE_NUMBER,
                Episodes.SEASON,
                Episodes.IMDBID,
                Shows.REF_SHOW_ID,
                Shows.TITLE,
                Shows.IMDBID
        };

        int TITLE = 0;
        int NUMBER = 1;
        int NUMBER_ABSOLUTE = 2;
        int SEASON = 3;
        int IMDB_ID = 4;
        int SHOW_TVDB_ID = 5;
        int SHOW_TITLE = 6;
        int SHOW_IMDB_ID = 7;
    }
}
