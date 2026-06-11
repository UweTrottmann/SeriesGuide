// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.lists.database.SgList
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Helper
import com.battlelancer.seriesguide.shows.database.SgSeason2
import com.battlelancer.seriesguide.shows.database.SgSeason2Helper
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.database.SgShow2Helper
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files
import kotlin.io.path.writeText

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class JsonImportTaskTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var testDb: SgRoomDatabase

    @Before
    fun switchToInMemoryDb() {
        // Use an in-memory database for testing with Room
        SgRoomDatabase.switchToInMemory(context)
        testDb = SgRoomDatabase.getInstance(context)
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    /**
     * Mockito.any returns null causing Kotlin null check to throw,
     * wrap in this helper to fake the return value never being null.
     */
    private fun <T> anyNotNull(type: Class<T>): T = Mockito.any(type)

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
            sgEpisode2Helper,
            mock(SgListHelper::class.java),
            mock(MovieHelper::class.java)
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
            titleNoArticle = "Jujutsu Kaisen", // set from title
            language = "de-DE" // Legacy language code is mapped.
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
        )
        val expectedEpisode2 = JsonExportTaskTest.listOfTestEpisodes[1].copy(
            id = 0, // insert
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

    @Test
    fun importList_modelAsExpected() = runTest {
        val sgListHelper = mock(SgListHelper::class.java)

        val importTask = JsonImportTask(
            context,
            importShows = false,
            importLists = true,
            importMovies = false,
            mock(SgRoomDatabase::class.java),
            mock(SgShow2Helper::class.java),
            mock(SgSeason2Helper::class.java),
            mock(SgEpisode2Helper::class.java),
            sgListHelper,
            mock(MovieHelper::class.java)
        )

        // Test data from export task test: two lists, the first with one item of each type.
        val testBackupFile = Files.createTempFile("seriesguide-lists-json", null)
        testBackupFile.writeText(JsonExportTaskTest.expectedJsonLists)
        importTask.testBackupFile = testBackupFile.toFile()

        val result = importTask.run()
        assertThat(importTask.errorCause).isNull()
        assertThat(result).isEqualTo(JsonImportTask.SUCCESS)

        // List 1 with items
        verify(sgListHelper).insertList(
            SgList(listId = "list-1", name = "First List", order = 0)
        )
        val expectedListItems = JsonExportTaskTest.listOfTestListItems
        verify(sgListHelper).insertListItems(expectedListItems)

        // List 2 has no items
        verify(sgListHelper).insertList(
            SgList(listId = "list-2", name = "Empty List", order = 1)
        )
    }

    private suspend fun importMovies(json: String) {
        val importTask = JsonImportTask(
            context,
            importShows = false,
            importLists = false,
            importMovies = true
        )

        @Suppress("BlockingMethodInNonBlockingContext")
        val testBackupFile = Files.createTempFile("seriesguide-movies-json", null)
        testBackupFile.writeText(json)
        importTask.testBackupFile = testBackupFile.toFile()

        val result = importTask.run()
        assertThat(importTask.errorCause).isNull()
        assertThat(result).isEqualTo(JsonImportTask.SUCCESS)
    }

    @Test
    fun importMovie_modelAsExpected() = runTest {
        importMovies(JsonExportTaskTest.expectedJsonMovies)

        // Two movies, second one with only TMDB ID and title
        val insertedMovies = testDb.movieHelper().getAllMovies()
        assertThat(insertedMovies).hasSize(2)

        val testMovieWithValues = JsonExportTaskTest.testMovieWithValues
        val insertedMovieWithValues =
            insertedMovies.first { it.tmdbId == testMovieWithValues.tmdbId }
        // Check a primary key was assigned
        assertThat(insertedMovieWithValues.id).isGreaterThan(0)
        assertThat(insertedMovieWithValues)
            .isEqualTo(
                testMovieWithValues.copy(
                    id = insertedMovieWithValues.id,
                    titleNoArticle = testMovieWithValues.title
                )
            )

        val testMovieMinimal = JsonExportTaskTest.testMovieMinimal
        val insertedMovieMinimal =
            insertedMovies.first { it.tmdbId == testMovieMinimal.tmdbId }
        // Check a primary key was assigned
        assertThat(insertedMovieMinimal.id).isGreaterThan(0)
        assertThat(insertedMovieMinimal)
            .isEqualTo(
                testMovieMinimal.copy(
                    id = insertedMovieMinimal.id,
                    titleNoArticle = testMovieMinimal.title,
                    releasedMs = SgMovie.RELEASED_MS_UNKNOWN,
                    lastUpdated = 0
                )
            )
    }

    @Test
    fun importMovie_missingRequired_notImported() = runTest {
        @Language("json")
        val jsonMovieMissingRequiredValues =
            """
            [
            {"imdb_id":"imdbidvalue","title":"First Movie"}
            ]
            """.trimIndent()

        importMovies(jsonMovieMissingRequiredValues)

        // Nothing is imported
        assertThat(testDb.movieHelper().getAllMovies()).hasSize(0)
    }

}