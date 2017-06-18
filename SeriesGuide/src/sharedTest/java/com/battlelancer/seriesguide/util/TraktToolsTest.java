package com.battlelancer.seriesguide.util;

import java.util.Locale;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TraktToolsTest {

    /**
     * Also available as an instrumented unit test as Android uses a different formatter than the
     * local JVM.
     */
    @Test
    public void ratingString() {
        assertThat(TraktTools.buildRatingString(1.0, Locale.GERMAN)).isEqualToIgnoringCase("1,0");
        assertThat(TraktTools.buildRatingString(1.5, Locale.GERMAN)).isEqualToIgnoringCase("1,5");
        assertThat(TraktTools.buildRatingString(1.05, Locale.GERMAN)).isEqualToIgnoringCase("1,1");
    }
}
