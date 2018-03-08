package com.battlelancer.seriesguide.util;

import static com.google.common.truth.Truth.assertThat;

import com.battlelancer.seriesguide.traktapi.TraktTools;
import java.util.Locale;
import org.junit.Test;

public class TraktToolsTest {

    /**
     * Note: ensure to test on Android as Android uses a different formatter than the local JVM.
     */
    @Test
    public void ratingString() {
        assertThat(TraktTools.buildRatingString(1.0, Locale.GERMAN)).isEqualTo("1,0");
        assertThat(TraktTools.buildRatingString(1.5, Locale.GERMAN)).isEqualTo("1,5");
        assertThat(TraktTools.buildRatingString(1.05, Locale.GERMAN)).isEqualTo("1,1");
    }
}
