package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.database.Cursor;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.util.TextTools;
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

    private final int episodeTvdbId;
    private Cursor query;

    public EpisodeActionsLoader(Context context, int episodeTvdbId) {
        super(context);
        this.episodeTvdbId = episodeTvdbId;
    }

    @Override
    public List<Action> loadInBackground() {
        List<Action> actions = ExtensionManager.get()
                .getLatestEpisodeActions(getContext(), episodeTvdbId);

        // no actions available yet, request extensions to publish them
        if (actions == null || actions.size() == 0) {
            actions = new ArrayList<>();

            query = getContext().getContentResolver().query(
                    Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                    Query.PROJECTION, null, null, null);
            if (query == null) {
                return actions;
            }

            Episode episode = null;
            if (query.moveToFirst()) {
                int number = query.getInt(Query.NUMBER);
                episode = new Episode.Builder()
                        .tvdbId(episodeTvdbId)
                        .title(TextTools.getEpisodeTitle(getContext(), query.getString(Query.TITLE),
                                number))
                        .number(number)
                        .numberAbsolute(query.getInt(Query.NUMBER_ABSOLUTE))
                        .season(query.getInt(Query.SEASON))
                        .imdbId(query.getString(Query.IMDB_ID))
                        .showTvdbId(query.getInt(Query.SHOW_TVDB_ID))
                        .showTitle(query.getString(Query.SHOW_TITLE))
                        .showImdbId(query.getString(Query.SHOW_IMDB_ID))
                        .showFirstReleaseDate(query.getString(Query.SHOW_FIRST_RELEASE))
                        .build();
            }
            // clean up query first
            query.close();
            query = null;

            if (episode != null) {
                ExtensionManager.get()
                        .requestEpisodeActions(getContext(), episode);
            }
        }

        return actions;
    }

    @Override
    protected void onReleaseResources(List<Action> items) {
        if (query != null && !query.isClosed()) {
            query.close();
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
                Shows.IMDBID,
                Shows.FIRST_RELEASE
        };

        int TITLE = 0;
        int NUMBER = 1;
        int NUMBER_ABSOLUTE = 2;
        int SEASON = 3;
        int IMDB_ID = 4;
        int SHOW_TVDB_ID = 5;
        int SHOW_TITLE = 6;
        int SHOW_IMDB_ID = 7;
        int SHOW_FIRST_RELEASE = 8;
    }
}
