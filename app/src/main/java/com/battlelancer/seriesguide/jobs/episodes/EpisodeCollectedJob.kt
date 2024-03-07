// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2020-2023 Uwe Trottmann

package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;

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
}
