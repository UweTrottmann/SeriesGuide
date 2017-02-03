package com.battlelancer.seriesguide.test.instrumented;

import android.support.test.runner.AndroidJUnit4;
import com.battlelancer.seriesguide.util.TraktTools;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(AndroidJUnit4.class)
public class TraktToolsTest {

    /**
     * Also available as a basic unit test as Android uses a different formatter.
     */
    @Test
    public void ratingString() {
        assertThat(TraktTools.buildRatingString(1.0, Locale.GERMAN)).isEqualToIgnoringCase("1,0");
        assertThat(TraktTools.buildRatingString(1.5, Locale.GERMAN)).isEqualToIgnoringCase("1,5");
        assertThat(TraktTools.buildRatingString(1.05, Locale.GERMAN)).isEqualToIgnoringCase("1,1");
    }

}
