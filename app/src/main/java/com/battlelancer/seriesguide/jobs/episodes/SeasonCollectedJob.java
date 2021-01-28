package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.util.TextTools;
import java.util.List;

public class SeasonCollectedJob extends SeasonBaseJob {

    private final boolean isCollected;

    public SeasonCollectedJob(long seasonId, boolean isCollected) {
        super(seasonId, isCollected ? 1 : 0, JobAction.EPISODE_COLLECTION);
        this.isCollected = isCollected;
    }

    @Override
    protected boolean applyDatabaseChanges(@NonNull Context context) {
        int rowsUpdated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                .updateCollectedOfSeason(seasonId, isCollected);
        return rowsUpdated >= 0; // -1 means error.
    }

    @NonNull
    @Override
    protected List<SgEpisode2Numbers> getEpisodesForNetworkJob(@NonNull Context context) {
        return SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                .getEpisodeNumbersOfSeason(seasonId);
    }

    @Override
    protected int getPlaysForNetworkJob(int plays) {
        return plays; // Collected change does not change plays.
    }

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        String number = TextTools.getEpisodeNumber(context, getSeason().getNumber(), -1);
        return TextTools.dotSeparate(number, context.getString(isCollected
                ? R.string.action_collection_add : R.string.action_collection_remove));
    }
}
