package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.model.SgSeason2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SgEpisode2Helper
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgSeason2Helper
import com.battlelancer.seriesguide.provider.SgShow2Helper
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files
import kotlin.io.path.writeText

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class JsonImportTaskTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Mockito.any returns null causing Kotlin null check to throw,
     * wrap in this helper to fake the return value never being null.
     */
    private fun <T> anyNotNull(type: Class<T>): T = Mockito.any(type)

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun importShow_modelAsExpected() = runTest {
        val sgShow2Helper = mock(SgShow2Helper::class.java)
        val sgSeason2Helper = mock(SgSeason2Helper::class.java)
        val sgEpisode2Helper = mock(SgEpisode2Helper::class.java)

        val importTask = JsonImportTask(
            context,
            importShows = true,
            importLists = false,
            importMovies = false,
            mock(SgRoomDatabase::class.java),
            sgShow2Helper,
            sgSeason2Helper,
            sgEpisode2Helper
        )

        // Test data from export task test: single show, two seasons, each with two episodes.
        val testBackupFile = Files.createTempFile("seriesguide-shows-json", null)
        testBackupFile.writeText(JsonExportTaskTest.expectedJsonShows)
        importTask.testBackupFile = testBackupFile.toFile()

        // Return row ids like it would be an insert on an empty database, so start at 1.
        `when`(sgShow2Helper.insertShow(anyNotNull(SgShow2::class.java))).thenReturn(1)
        var seasonRowId = 0L
        `when`(sgSeason2Helper.insertSeason(anyNotNull(SgSeason2::class.java))).then {
            seasonRowId++
            return@then seasonRowId
        }

        val result = importTask.run()
        assertThat(importTask.errorCause).isNull()
        assertThat(result).isEqualTo(JsonImportTask.SUCCESS)

        val expectedShow = JsonExportTaskTest.listOfTestShows[0].copy(
            id = 0, // insert
            titleNoArticle = "Jujutsu Kaisen" // set from title
        )
        verify(sgShow2Helper).insertShow(expectedShow)

        // Season 1
        val expectedSeason1 = JsonExportTaskTest.listOfTestSeasons[0].copy(
            id = 0, // insert
            name = null // not imported
        )
        verify(sgSeason2Helper).insertSeason(expectedSeason1)

        // Season 2
        val expectedSeason2 = JsonExportTaskTest.listOfTestSeasons[1].copy(
            id = 0, // insert
            name = null // not imported
        )
        verify(sgSeason2Helper).insertSeason(expectedSeason2)

        // Episodes of season 1
        val expectedEpisode1 = JsonExportTaskTest.listOfTestEpisodes[0].copy(
            id = 0, // insert
            ratingGlobal = 0.0,
            ratingVotes = 0,
            ratingUser = 0
        )
        val expectedEpisode2 = JsonExportTaskTest.listOfTestEpisodes[1].copy(
            id = 0, // insert
            ratingGlobal = 0.0,
            ratingVotes = 0,
            ratingUser = 0
        )
        verify(sgEpisode2Helper).insertEpisodes(listOf(expectedEpisode1, expectedEpisode2))

        // Episodes of season 2
        val expectedEpisode3 = expectedEpisode1.copy(
            seasonId = 2,
            season = 2
        )
        val expectedEpisode4 = expectedEpisode2.copy(
            seasonId = 2,
            season = 2
        )
        verify(sgEpisode2Helper).insertEpisodes(listOf(expectedEpisode3, expectedEpisode4))
    }

}