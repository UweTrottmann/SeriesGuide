
package com.battlelancer.seriesguide.beta.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.test.ActivityUnitTestCase;

import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.FlagTask.FlagTaskType;
import com.battlelancer.seriesguide.util.FlagTask.OnFlagListener;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB.ShowStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FlagTaskTest extends ActivityUnitTestCase<ShowsActivity> implements OnFlagListener {

    private int showTvdbId;
    private int seasonId;
    private CountDownLatch mFlagTaskSignal;

    public FlagTaskTest(Class<ShowsActivity> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        insertSampleShowIntoDatabase();
    }

    private void insertSampleShowIntoDatabase() {
        showTvdbId = 987654321;
        seasonId = 987654321;
        long showAirtime = Utils.parseTimeToMilliseconds("20:00 PM");

        ContentValues sampleShow = new ContentValues();
        sampleShow.put(Shows._ID, showTvdbId); // high id, unlikey to be in TVDb
        // anytime soon
        sampleShow.put(Shows.TITLE, "Sample show");
        sampleShow.put(Shows.NETWORK, "Sample Network");
        sampleShow.put(Shows.STATUS, ShowStatus.CONTINUING);
        sampleShow.put(Shows.RUNTIME, 5);
        sampleShow.put(Shows.AIRSDAYOFWEEK, "Monday");
        sampleShow.put(Shows.AIRSTIME, showAirtime);
        getActivity().getContentResolver().insert(Shows.CONTENT_URI, sampleShow);

        ContentValues sampleSeason = new ContentValues();
        sampleSeason.put(Seasons._ID, seasonId);
        sampleSeason.put(Seasons.COMBINED, 1);
        sampleSeason.put(Shows.REF_SHOW_ID, showTvdbId);
        getActivity().getContentResolver().insert(Seasons.CONTENT_URI, sampleSeason);

        ContentValues[] sampleEpisodes = new ContentValues[10];
        for (int i = 0; i < sampleEpisodes.length; i++) {
            ContentValues sampleEpisode = new ContentValues();
            sampleEpisode.put(Episodes._ID, 987654321 + i);
            sampleEpisode.put(Episodes.TITLE, "Sample Episode " + i);
            sampleEpisode.put(Shows.REF_SHOW_ID, showTvdbId);
            sampleEpisode.put(Seasons.REF_SEASON_ID, seasonId);
            sampleEpisode.put(Episodes.SEASON, 1);
            sampleEpisode.put(Episodes.NUMBER, i);
            sampleEpisode.put(Episodes.FIRSTAIREDMS,
                    Utils.buildEpisodeAirtime("2013-01-0" + i, showAirtime));
            sampleEpisodes[i] = sampleEpisode;
        }
        getActivity().getContentResolver().bulkInsert(Episodes.CONTENT_URI, sampleEpisodes);
    }

    public void testEpisodeWatched() throws Throwable {
        int episodeId = 987654321;

        // create a signal to let us know when our task is done.
        mFlagTaskSignal = new CountDownLatch(1);

        final FlagTask task = new FlagTask(getActivity(), showTvdbId, this)
                .episodeWatched(episodeId, 1, 1, true);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                task.execute();
            }
        });

        mFlagTaskSignal.await(30, TimeUnit.SECONDS);

        Cursor episode = getActivity().getContentResolver().query(
                Episodes.buildEpisodeUri(String.valueOf(episodeId)), new String[] {
                        Episodes._ID, Episodes.WATCHED
                }, null, null, null);

        // TODO Assert
    }

    @Override
    public void onFlagCompleted(FlagTaskType type) {
        if (mFlagTaskSignal != null) {
            mFlagTaskSignal.countDown();
        }
    }
}
