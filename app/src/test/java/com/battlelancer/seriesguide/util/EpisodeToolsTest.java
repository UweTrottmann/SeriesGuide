package com.battlelancer.seriesguide.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import org.junit.Test;

public class EpisodeToolsTest {

    @Test
    public void test_isWatched() {
        assertThat(EpisodeTools.isWatched(EpisodeFlags.WATCHED)).isTrue();
        assertThat(EpisodeTools.isWatched(EpisodeFlags.SKIPPED)).isFalse();
        assertThat(EpisodeTools.isWatched(EpisodeFlags.UNWATCHED)).isFalse();
    }

    @Test
    public void test_isUnwatched() {
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.UNWATCHED)).isTrue();
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.WATCHED)).isFalse();
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.SKIPPED)).isFalse();
    }

    @Test
    public void test_validateFlags() {
        EpisodeTools.validateFlags(EpisodeFlags.UNWATCHED);
        EpisodeTools.validateFlags(EpisodeFlags.WATCHED);
        EpisodeTools.validateFlags(EpisodeFlags.SKIPPED);
        try {
            EpisodeTools.validateFlags(123);
            fail("IllegalArgumentException not thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

}