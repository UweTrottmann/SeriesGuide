package com.battlelancer.seriesguide.notifications

import android.content.Context
import android.text.format.DateUtils
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.shows.database.SgEpisode2WithShow
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class NotificationServiceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun shouldCheckToNotify() {
        val service = NotificationService(context)

        val currentTime = System.currentTimeMillis()
        val nextRelease = currentTime + 1 * DateUtils.HOUR_IN_MILLIS
        val lastNotifiedAbout = nextRelease - 6 * DateUtils.HOUR_IN_MILLIS;
        NotificationSettings.setLastNotifiedAbout(context, lastNotifiedAbout)
        val episodes = listOf(
            // 12 hours before is max returned by upcoming episodes query
            sgEpisode2WithShow(1, lastNotifiedAbout),
            sgEpisode2WithShow(2, nextRelease - 2 * DateUtils.HOUR_IN_MILLIS),
            sgEpisode2WithShow(3, nextRelease),
            sgEpisode2WithShow(4, nextRelease + 10 * DateUtils.HOUR_IN_MILLIS),
            // 14 days in the future is max returned by upcoming episodes query
            sgEpisode2WithShow(5, nextRelease + 14 * DateUtils.DAY_IN_MILLIS)
        )

        // Running for the first time = default values
        assertThat(service.shouldCheckToNotify(0, 0, episodes)).isTrue()

        // Woken at or after planned time
        assertThat(service.shouldCheckToNotify(currentTime, nextRelease, episodes)).isTrue()
        assertThat(
            service.shouldCheckToNotify(
                currentTime + 5 * DateUtils.MINUTE_IN_MILLIS,
                nextRelease,
                episodes
            )
        ).isTrue()

        // Woken up earlier
        val beforeNextRelease = nextRelease - 5 * DateUtils.MINUTE_IN_MILLIS
        //   No upcoming episodes at all
        assertThat(
            service.shouldCheckToNotify(beforeNextRelease, nextRelease, emptyList())
        ).isTrue()
        //   New upcoming episode released before planned one to notify about.
        assertThat(
            service.shouldCheckToNotify(beforeNextRelease, nextRelease, episodes)
        ).isTrue()
        //   No new upcoming episodes to notify about, but planned one was removed.
        assertThat(
            service.shouldCheckToNotify(
                beforeNextRelease, nextRelease,
                listOf(
                    sgEpisode2WithShow(1, lastNotifiedAbout - DateUtils.HOUR_IN_MILLIS),
                    sgEpisode2WithShow(2, lastNotifiedAbout),
                    sgEpisode2WithShow(4, nextRelease + 10 * DateUtils.HOUR_IN_MILLIS),
                    sgEpisode2WithShow(5, nextRelease + 14 * DateUtils.DAY_IN_MILLIS)
                )
            )
        ).isTrue()
        //   Earlier episodes already notified about, no new ones, find new wake-up time.
        assertThat(
            service.shouldCheckToNotify(
                beforeNextRelease, nextRelease,
                listOf(
                    sgEpisode2WithShow(1, lastNotifiedAbout - DateUtils.HOUR_IN_MILLIS),
                    sgEpisode2WithShow(2, lastNotifiedAbout)
                )
            )
        ).isTrue()

        //   Next one to notify about is the planned one, continue sleeping until then.
        assertThat(
            service.shouldCheckToNotify(
                beforeNextRelease, nextRelease,
                listOf(
                    sgEpisode2WithShow(3, nextRelease),
                    sgEpisode2WithShow(4, nextRelease + 10 * DateUtils.HOUR_IN_MILLIS),
                    sgEpisode2WithShow(5, nextRelease + 14 * DateUtils.DAY_IN_MILLIS)
                )
            )
        ).isFalse()
    }

    private fun sgEpisode2WithShow(idAndNumber: Int, releaseTime: Long) = SgEpisode2WithShow(
        id = idAndNumber.toLong(),
        episodetitle = null,
        episodenumber = idAndNumber,
        season = 1,
        episode_firstairedms = releaseTime,
        watched = EpisodeFlags.UNWATCHED,
        episode_collected = false,
        overview = null,
        seriestitle = "That Show",
        network = null,
        series_poster_small = null
    )
}