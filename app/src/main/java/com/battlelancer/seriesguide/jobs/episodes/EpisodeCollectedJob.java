package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.util.TextTools;

public class EpisodeCollectedJob extends EpisodeBaseJob {

    private final boolean isCollected;

    public EpisodeCollectedJob(long episodeId, boolean isCollected) {
        super(episodeId, isCollected ? 1 : 0, JobAction.EPISODE_COLLECTION);
        this.isCollected = isCollected;
    }

    @Override
    protected boolean applyDatabaseChanges(@NonNull Context context) {
        int updated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                .updateCollected(episodeId, isCollected);
        return updated == 1;
    }

    @Override
    protected int getPlaysForNetworkJob(int plays) {
        return plays; // Collected change does not change plays.
    }

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        String number = TextTools.getEpisodeNumber(context, getEpisode().getSeason(),
                getEpisode().getEpisodenumber());
        return TextTools.dotSeparate(number, context.getString(isCollected
                ? R.string.action_collection_add : R.string.action_collection_remove));
    }
}
