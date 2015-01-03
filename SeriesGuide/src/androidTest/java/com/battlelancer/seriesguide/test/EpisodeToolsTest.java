package com.battlelancer.seriesguide.test;

import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.util.EpisodeTools;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class EpisodeToolsTest extends TestCase {

    public static Test suite() {
        return new TestSuite(EpisodeToolsTest.class);
    }

    public void test_isWatched() {
        assertThat(EpisodeTools.isWatched(EpisodeFlags.WATCHED)).isTrue();
        assertThat(EpisodeTools.isWatched(EpisodeFlags.SKIPPED)).isFalse();
        assertThat(EpisodeTools.isWatched(EpisodeFlags.UNWATCHED)).isFalse();
    }

    public void test_isUnwatched() {
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.UNWATCHED)).isTrue();
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.WATCHED)).isFalse();
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.SKIPPED)).isFalse();
    }

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