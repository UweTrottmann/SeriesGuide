package com.battlelancer.seriesguide.util

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.TextTools.EpisodeFormat.DEFAULT
import com.battlelancer.seriesguide.util.TextTools.EpisodeFormat.PREFIX_ENGLISH
import com.battlelancer.seriesguide.util.TextTools.EpisodeFormat.PREFIX_ENGLISH_LOWER
import com.battlelancer.seriesguide.util.TextTools.EpisodeFormat.SUFFIX_LONG
import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class TextToolsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun getEpisodeNumber() {
        setNumberFormat(DEFAULT.value)
        assertGetEpisodeNumber().isEqualTo("1x02")
        setNumberFormat(PREFIX_ENGLISH.value)
        assertGetEpisodeNumber().isEqualTo("S1:E2")
        setNumberFormat(PREFIX_ENGLISH_LOWER.value)
        assertGetEpisodeNumber().isEqualTo("s1:e2")
        setNumberFormat(PREFIX_ENGLISH.value + SUFFIX_LONG.value)
        assertGetEpisodeNumber().isEqualTo("S01E02")
        setNumberFormat(PREFIX_ENGLISH_LOWER.value + SUFFIX_LONG.value)
        assertGetEpisodeNumber().isEqualTo("s01e02")
    }

    private fun setNumberFormat(format: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(DisplaySettings.KEY_NUMBERFORMAT, format)
        }
    }

    private fun assertGetEpisodeNumber(): StringSubject {
        return assertThat(TextTools.getEpisodeNumber(context, 1, 2))
    }

    @Test
    fun splitPipeSeparatedStrings() {
        assertThat(
            TextTools.splitPipeSeparatedStrings(null)
        ).isEqualTo("")
        assertThat(
            TextTools.splitPipeSeparatedStrings("")
        ).isEqualTo("")
        assertThat(
            TextTools.splitPipeSeparatedStrings("First|Second | Third")
        ).isEqualTo("First, Second, Third")
    }

    @Test
    fun buildPipeSeparatedString() {
        assertThat(
            TextTools.buildPipeSeparatedString(null)
        ).isEqualTo("")
        assertThat(
            TextTools.buildPipeSeparatedString(emptyList())
        ).isEqualTo("")
        assertThat(
            TextTools.buildPipeSeparatedString(listOf("First", "Second ", null, "", " Third"))
        ).isEqualTo("First|Second | Third")
    }

    @Test
    fun trimLeadingArticle() {
        assertThat(
            TextTools.trimLeadingArticle(null)
        ).isEqualTo(null)
        assertThat(
            TextTools.trimLeadingArticle("")
        ).isEqualTo("")
        assertThat(
            TextTools.trimLeadingArticle("keep as is")
        ).isEqualTo("keep as is")

        assertThat(
            TextTools.trimLeadingArticle("The first the is gone")
        ).isEqualTo("first the is gone")
        assertThat(
            TextTools.trimLeadingArticle("the lower-case the is gone")
        ).isEqualTo("lower-case the is gone")

        assertThat(
            TextTools.trimLeadingArticle("A initial A is gone")
        ).isEqualTo("initial A is gone")
        assertThat(
            TextTools.trimLeadingArticle("a lower-case a is gone")
        ).isEqualTo("lower-case a is gone")

        assertThat(
            TextTools.trimLeadingArticle("An initial An is gone")
        ).isEqualTo("initial An is gone")
        assertThat(
            TextTools.trimLeadingArticle("an lower-case an is gone")
        ).isEqualTo("lower-case an is gone")
    }

    @Test
    fun dotSeparate() {
        assertThat(TextTools.dotSeparate(null, null)).isEqualTo("")
        assertThat(TextTools.dotSeparate("", "")).isEqualTo("")
        assertThat(TextTools.dotSeparate("Left", "Right")).isEqualTo("Left · Right")

        assertThat(TextTools.dotSeparate("Left", "")).isEqualTo("Left")
        assertThat(TextTools.dotSeparate("Left", null)).isEqualTo("Left")

        assertThat(TextTools.dotSeparate("", "Right")).isEqualTo("Right")
        assertThat(TextTools.dotSeparate(null, "Right")).isEqualTo("Right")
    }

    @Test
    fun getWatchedButtonText() {
        assertThat(TextTools.getWatchedButtonText(context, true, null))
            .isEqualTo(context.getString(R.string.state_watched))
        assertThat(TextTools.getWatchedButtonText(context, true, 1))
            .isEqualTo(context.getString(R.string.state_watched))

        assertThat(TextTools.getWatchedButtonText(context, true, 3))
            .isEqualTo(context.getString(R.string.state_watched_multiple_format, 3))

        assertThat(TextTools.getWatchedButtonText(context, false, null))
            .isEqualTo(context.getString(R.string.action_watched))
        assertThat(TextTools.getWatchedButtonText(context, false, 1))
            .isEqualTo(context.getString(R.string.action_watched))
    }
}