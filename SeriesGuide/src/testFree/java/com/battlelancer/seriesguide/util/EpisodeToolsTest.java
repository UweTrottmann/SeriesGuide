package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.enums.EpisodeFlags;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

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
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

}