package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.traktapi.TraktTools;
import java.util.Locale;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TraktToolsTest {

    /**
     * Also available as an instrumented unit test as Android uses a different formatter than the
     * local JVM.
     */
    @Test
    public void ratingString() {
        assertThat(TraktTools.buildRatingString(1.0, Locale.GERMAN)).isEqualTo("1,0");
        assertThat(TraktTools.buildRatingString(1.5, Locale.GERMAN)).isEqualTo("1,5");
        assertThat(TraktTools.buildRatingString(1.05, Locale.GERMAN)).isEqualTo("1,1");
    }
}
