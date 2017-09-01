package com.battlelancer.seriesguide.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

public class EpisodeFlagService extends JobIntentService {

    private static final int JOB_ID = 1000;
    private static final String EXTRA_EPISODE_TVDB_ID = "episodeTvdbId";
    private static final String EXTRA_EPISODE_FLAG = "episodeFlag";

    private static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, EpisodeFlagService.class, JOB_ID, work);
    }

    public static void enqueueChangeEpisodeFlag(Context context, int episodeTvdbId,
            int episodeFlag) {
        Intent work = new Intent();
        work.putExtra(EXTRA_EPISODE_TVDB_ID, episodeTvdbId);
        work.putExtra(EXTRA_EPISODE_FLAG, episodeFlag);
        enqueueWork(context, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        int episodeTvdbId = intent.getIntExtra(EXTRA_EPISODE_TVDB_ID, -1);
        if (episodeTvdbId == -1) {
            return;
        }
        int episodeFlag = intent.getIntExtra(EXTRA_EPISODE_FLAG, -1);
        if (episodeFlag == -1) {
            return;
        }

        EpisodeTask task = EpisodeTask.typeChangeFlag(getApplicationContext(), episodeTvdbId,
                episodeFlag);
        if (task != null) {
            task.execute();
        }
    }

}
