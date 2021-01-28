package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.provider.SgEpisode2Helper;
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import java.util.ArrayList;
import java.util.List;

/**
 * Flagging single episodes watched or collected.
 */
public abstract class EpisodeBaseJob extends BaseEpisodesJob {

    protected final long episodeId;
    private SgEpisode2Numbers episode;

    public EpisodeBaseJob(long episodeId, int flagValue, JobAction action) {
        super(flagValue, action);
        this.episodeId = episodeId;
    }

    @Override
    public boolean applyLocalChanges(Context context, boolean requiresNetworkJob) {
        // Gather data needed for later steps.
        SgEpisode2Helper helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper();
        SgEpisode2Numbers episode = helper.getEpisodeNumbers(episodeId);
        if (episode == null) {
            return false;
        }
        this.episode = episode;

        return super.applyLocalChanges(context, requiresNetworkJob);
    }

    @NonNull
    protected SgEpisode2Numbers getEpisode() {
        return episode;
    }

    @Override
    protected long getShowId() {
        return getEpisode().getShowId();
    }

    @NonNull
    @Override
    protected List<SgEpisode2Numbers> getEpisodesForNetworkJob(@NonNull Context context) {
        List<SgEpisode2Numbers> list = new ArrayList<>();
        list.add(episode);
        return list;
    }
}
