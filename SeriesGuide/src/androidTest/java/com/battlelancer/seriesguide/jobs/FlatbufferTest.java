package com.battlelancer.seriesguide.jobs;

import android.support.test.runner.AndroidJUnit4;
import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class FlatbufferTest {

    @Test
    public void createAndReadBuffer() {
        FlatBufferBuilder builder = new FlatBufferBuilder(0);

        int[] episodeInfos = new int[42];
        for (int i = 0; i < 21; i++) {
            episodeInfos[i] = EpisodeInfo.createEpisodeInfo(builder, 1, i + 1);
        }
        for (int i = 21; i < 42; i++) {
            episodeInfos[i] = EpisodeInfo.createEpisodeInfo(builder, 2, i + 1);
        }

        int episodes = SgJobInfo.createEpisodesVector(builder, episodeInfos);
        int jobInfo = SgJobInfo.createSgJobInfo(builder, 42, 1, episodes);

        builder.finish(jobInfo);

        byte[] bytes = builder.sizedByteArray();

        ByteBuffer bufferReloaded = ByteBuffer.wrap(bytes);

        SgJobInfo jobInfoReloaded = SgJobInfo.getRootAsSgJobInfo(bufferReloaded);

        assertThat(jobInfoReloaded.showTvdbId(), equalTo(42));
        assertThat(jobInfoReloaded.flagValue(), equalTo(1));
        assertThat(jobInfoReloaded.episodesLength(), equalTo(42));
        for (int i = 0; i < 21; i++) {
            EpisodeInfo episodeInfo = jobInfoReloaded.episodes(i);
            assertThat(episodeInfo.season(), equalTo(1));
            assertThat(episodeInfo.number(), equalTo(i + 1));
        }
        for (int i = 21; i < 42; i++) {
            EpisodeInfo episodeInfo = jobInfoReloaded.episodes(i);
            assertThat(episodeInfo.season(), equalTo(2));
            assertThat(episodeInfo.number(), equalTo(i + 1));
        }
    }

}
