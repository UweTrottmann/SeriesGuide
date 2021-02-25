package com.battlelancer.seriesguide.extensions;

import android.content.Context;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.model.SgEpisode2;
import com.battlelancer.seriesguide.model.SgShow2;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Tries returning existing actions for an episode. If no actions have been published, will ask
 * extensions to do so and returns an empty list.
 */
public class EpisodeActionsLoader extends GenericSimpleLoader<List<Action>> {

    private final long episodeId;

    public EpisodeActionsLoader(Context context, long episodeId) {
        super(context);
        this.episodeId = episodeId;
    }

    @Override
    public List<Action> loadInBackground() {
        SgRoomDatabase database = SgRoomDatabase.getInstance(getContext());

        SgEpisode2 episode = database.sgEpisode2Helper().getEpisode(episodeId);
        if (episode == null) {
            return new ArrayList<>();
        }
        Integer episodeTmdbId = episode.getTmdbId();
        if (episodeTmdbId == null) {
            return new ArrayList<>();
        }

        List<Action> actions = ExtensionManager.get(getContext())
                .getLatestEpisodeActions(getContext(), episodeTmdbId);

        // no actions available yet, request extensions to publish them
        if (actions == null || actions.size() == 0) {
            actions = new ArrayList<>();

            SgShow2 show = database.sgShow2Helper().getShow(episode.getShowId());
            if (show == null) {
                return actions;
            }

            int number = episode.getNumber();
            Episode data = new Episode.Builder()
                    .tmdbId(episodeTmdbId)
                    .tvdbId(episode.getTvdbId())
                    .title(TextTools.getEpisodeTitle(getContext(), episode.getTitle(), number))
                    .number(number)
                    .numberAbsolute(episode.getAbsoluteNumber())
                    .season(episode.getSeason())
                    .imdbId(episode.getImdbId())
                    .showTvdbId(show.getTvdbId())
                    .showTitle(show.getTitle())
                    .showImdbId(show.getImdbId())
                    .showFirstReleaseDate(show.getFirstRelease())
                    .build();
            ExtensionManager.get(getContext()).requestEpisodeActions(getContext(), data);
        }

        return actions;
    }
}
