package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.enums.EpisodeFlags;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class EpisodeToolsTest {

    private static final int FLAGS_NONE = 0x0;

    private static final int FLAGS_ALL = 0x3;

    @Test
    public void test_isWatched() {
        assertThat(EpisodeTools.isWatched(EpisodeFlags.WATCHED)).isTrue();
        assertThat(EpisodeTools.isWatched(FLAGS_ALL)).isTrue();
        assertThat(EpisodeTools.isWatched(EpisodeFlags.SKIPPED)).isFalse();
        assertThat(EpisodeTools.isWatched(FLAGS_NONE)).isFalse();
    }

}