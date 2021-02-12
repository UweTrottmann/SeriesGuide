package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import java.util.List;

public class ShowCollectedJob extends ShowBaseJob {

    private final boolean isCollected;

    public ShowCollectedJob(long showId, boolean isCollected) {
        super(showId, isCollected ? 1 : 0, JobAction.EPISODE_COLLECTION);
        this.isCollected = isCollected;
    }

    @Override
    protected boolean applyDatabaseChanges(@NonNull Context context) {
        int rowsUpdated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                .updateCollectedOfShowExcludeSpecials(getShowId(), isCollected);
        return rowsUpdated >= 0; // -1 means error.
    }

    @NonNull
    @Override
    protected List<SgEpisode2Numbers> getEpisodesForNetworkJob(@NonNull Context context) {
        return SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                .getEpisodeNumbersOfShow(getShowId());
    }

    @Override
    protected int getPlaysForNetworkJob(int plays) {
        return plays; // Collected change does not change plays.
    }

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        return context.getString(isCollected
                ? R.string.action_collection_add : R.string.action_collection_remove);
    }

}
