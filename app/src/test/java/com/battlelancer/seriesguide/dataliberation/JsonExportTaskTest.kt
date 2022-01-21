package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgList
import com.battlelancer.seriesguide.model.SgListItem
import com.battlelancer.seriesguide.model.SgMovie
import com.battlelancer.seriesguide.model.SgSeason2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.MovieHelper
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SgEpisode2Helper
import com.battlelancer.seriesguide.provider.SgListHelper
import com.battlelancer.seriesguide.provider.SgSeason2Helper
import com.battlelancer.seriesguide.provider.SgShow2Helper
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class JsonExportTaskTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // JsonExportTask.onProgressUpdate uses Dispatcher.Main
    private val mainThreadSurrogate = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    private fun configureTestExportFile(exportTask: JsonExportTask): File {
        val file = File(context.filesDir, "test-export.json")
        // Clean any existing file, create an empty file as export task expects one
        Files.deleteIfExists(file.toPath())
        file.createNewFile()
        exportTask.testBackupFile = file

        // Using URI does not work, parsing returns empty path
        // and Robolectric fails with FileNotFoundException
//        val uri = Uri.fromFile(file)
//        BackupSettings.storeExportFileUri(context, JsonExportTask.BACKUP_SHOWS, uri, false)

        return file
    }

    @Test
    fun exportShows_jsonAsExpected() = runTest {
        val sgShow2Helper = mock(SgShow2Helper::class.java)
        val sgSeason2Helper = mock(SgSeason2Helper::class.java)
        val sgEpisode2Helper = mock(SgEpisode2Helper::class.java)

        val exportTask = JsonExportTask(
            context,
            progressListener = null,
            isFullDump = true,
            isAutoBackupMode = false,
            type = JsonExportTask.BACKUP_SHOWS,
            sgShow2Helper,
            sgSeason2Helper,
            sgEpisode2Helper,
            mock(SgListHelper::class.java),
            mock(MovieHelper::class.java)
        )

        val exportFile = configureTestExportFile(exportTask)

        // No data
        val noDataResult = exportTask.run()
        assertThat(exportTask.errorCause).isNull()
        assertThat(noDataResult).isEqualTo(JsonExportTask.SUCCESS)

        val exportWithNoData = exportFile.readText()
        println("Export with no data")
        println(exportWithNoData)
        assertThat(exportWithNoData).isEqualTo("[]")

        // With data
        `when`(sgShow2Helper.getShowsForExport()).thenReturn(listOfTestShows)
        `when`(sgSeason2Helper.getSeasonsForExport(1)).thenReturn(listOfTestSeasons)
        `when`(sgEpisode2Helper.getEpisodesForExport(1)).thenReturn(listOfTestEpisodes)
        `when`(sgEpisode2Helper.getEpisodesForExport(2)).thenReturn(listOfTestEpisodes)

        val withDataResult = exportTask.run()
        assertThat(exportTask.errorCause).isNull()
        assertThat(withDataResult).isEqualTo(JsonExportTask.SUCCESS)

        val exportWithData = exportFile.readText()
        println("Export with data")
        println(exportWithData)
        assertThat(exportWithData).isEqualTo(expectedJsonShows)
    }

    companion object {
        val listOfTestShows = listOf(
            SgShow2(
                id = 1,
                tmdbId = 95479,
                tvdbId = null,
                traktId = 52,
                title = "Jujutsu Kaisen",
                titleNoArticle = "",
                overview = "It's all about hollow purple.",
                releaseTime = 1234,
                releaseWeekDay = 1,
                releaseCountry = "JP",
                releaseTimeZone = "America/New_York",
                firstRelease = "2021-02-27T05:11:12.345Z",
                ratingGlobal = 10.0,
                ratingVotes = 1234,
                genres = "Animation|Action & Adventure|Sci-Fi & Fantasy",
                network = "MBS",
                imdbId = "imdbidvalue",
                runtime = 24,
                status = ShowTools.Status.ENDED,
                poster = "someurl/to/a/poster.jpg",
                posterSmall = "someurl/to/a/poster.jpg",
                language = "en",
                lastUpdatedMs = 0,
                lastWatchedMs = 1234567890
            ),
            SgShow2(
                id = 2,
                tmdbId = null,
                tvdbId = null,
                traktId = null,
                titleNoArticle = null,
                releaseTime = null,
                releaseWeekDay = null,
                releaseCountry = null,
                releaseTimeZone = "",
                firstRelease = null,
                ratingGlobal = null,
                ratingVotes = null,
                lastUpdatedMs = 0
            )
        )
        const val expectedJsonShows =
            "[{\"tmdb_id\":95479,\"imdb_id\":\"imdbidvalue\",\"trakt_id\":52,\"title\":\"Jujutsu Kaisen\",\"overview\":\"It\\u0027s all about hollow purple.\",\"language\":\"en\",\"first_aired\":\"2021-02-27T05:11:12.345Z\",\"release_time\":1234,\"release_weekday\":1,\"release_timezone\":\"America/New_York\",\"country\":\"JP\",\"poster\":\"someurl/to/a/poster.jpg\",\"content_rating\":\"\",\"status\":\"ended\",\"runtime\":24,\"genres\":\"Animation|Action \\u0026 Adventure|Sci-Fi \\u0026 Fantasy\",\"network\":\"MBS\",\"rating\":10.0,\"rating_votes\":1234,\"rating_user\":0,\"favorite\":false,\"notify\":true,\"hidden\":false,\"last_watched_ms\":1234567890,\"seasons\":[{\"tmdb_id\":\"1\",\"season\":1,\"episodes\":[{\"tmdb_id\":1,\"episode\":1,\"title\":\"First Episode\",\"first_aired\":1234567890,\"watched\":true,\"plays\":1,\"skipped\":false,\"collected\":false,\"imdb_id\":\"\",\"overview\":\"First overview\",\"image\":\"/first/still/path.jpg\",\"writers\":\"writers string\",\"gueststars\":\"guest stars string\",\"directors\":\"directors string\"},{\"tmdb_id\":2,\"episode\":2,\"title\":\"Second Episode\",\"first_aired\":1234567890,\"watched\":false,\"plays\":0,\"skipped\":true,\"collected\":true,\"imdb_id\":\"\",\"overview\":\"Second overview\",\"image\":\"/first/still/path.jpg\",\"writers\":\"writers string\",\"gueststars\":\"guest stars string\",\"directors\":\"directors string\"}]},{\"tmdb_id\":\"2\",\"season\":2,\"episodes\":[{\"tmdb_id\":1,\"episode\":1,\"title\":\"First Episode\",\"first_aired\":1234567890,\"watched\":true,\"plays\":1,\"skipped\":false,\"collected\":false,\"imdb_id\":\"\",\"overview\":\"First overview\",\"image\":\"/first/still/path.jpg\",\"writers\":\"writers string\",\"gueststars\":\"guest stars string\",\"directors\":\"directors string\"},{\"tmdb_id\":2,\"episode\":2,\"title\":\"Second Episode\",\"first_aired\":1234567890,\"watched\":false,\"plays\":0,\"skipped\":true,\"collected\":true,\"imdb_id\":\"\",\"overview\":\"Second overview\",\"image\":\"/first/still/path.jpg\",\"writers\":\"writers string\",\"gueststars\":\"guest stars string\",\"directors\":\"directors string\"}]}]},{\"imdb_id\":\"\",\"title\":\"\",\"overview\":\"\",\"language\":\"\",\"release_time\":-1,\"release_weekday\":-1,\"release_timezone\":\"\",\"poster\":\"\",\"content_rating\":\"\",\"status\":\"unknown\",\"runtime\":0,\"genres\":\"\",\"network\":\"\",\"rating\":0.0,\"rating_votes\":0,\"rating_user\":0,\"favorite\":false,\"notify\":true,\"hidden\":false,\"last_watched_ms\":0,\"seasons\":[]}]"

        val listOfTestSeasons = listOf(
            SgSeason2(
                id = 1,
                showId = 1,
                tmdbId = "1",
                numberOrNull = 1,
                order = 1,
                name = "Season 1"
            ),
            SgSeason2(
                id = 2,
                showId = 1,
                tmdbId = "2",
                numberOrNull = 2,
                order = 2,
                name = "Season 2"
            )
        )

        val listOfTestEpisodes = listOf(
            SgEpisode2(
                id = 1,
                showId = 1,
                seasonId = 1,
                tmdbId = 1,
                title = "First Episode",
                overview = "First overview",
                number = 1,
                order = 1,
                season = 1,
                image = "/first/still/path.jpg",
                firstReleasedMs = 1234567890,
                directors = "directors string",
                guestStars = "guest stars string",
                writers = "writers string",
                watched = EpisodeFlags.WATCHED,
                plays = 1,
                collected = false
            ),
            SgEpisode2(
                id = 2,
                showId = 1,
                seasonId = 1,
                tmdbId = 2,
                title = "Second Episode",
                overview = "Second overview",
                number = 2,
                order = 2,
                season = 1,
                image = "/first/still/path.jpg",
                firstReleasedMs = 1234567890,
                directors = "directors string",
                guestStars = "guest stars string",
                writers = "writers string",
                watched = EpisodeFlags.SKIPPED,
                plays = 0,
                collected = true
            )
        )
    }

    @Test
    fun exportLists_jsonAsExpected() = runTest {
        val sgListHelper = mock(SgListHelper::class.java)

        val exportTask = JsonExportTask(
            context,
            progressListener = null,
            isFullDump = true,
            isAutoBackupMode = false,
            type = JsonExportTask.BACKUP_LISTS,
            mock(SgShow2Helper::class.java),
            mock(SgSeason2Helper::class.java),
            mock(SgEpisode2Helper::class.java),
            sgListHelper,
            mock(MovieHelper::class.java)
        )

        val exportFile = configureTestExportFile(exportTask)

        // No data
        val noDataResult = exportTask.run()
        assertThat(exportTask.errorCause).isNull()
        assertThat(noDataResult).isEqualTo(JsonExportTask.SUCCESS)

        val exportWithNoData = exportFile.readText()
        println("Export with no data")
        println(exportWithNoData)
        assertThat(exportWithNoData).isEqualTo("[]")

        // With data
        `when`(sgListHelper.getListsForExport()).thenReturn(listOfTestLists)
        `when`(sgListHelper.getListItemsForExport("list-1")).thenReturn(listOfTestListItems)

        val withDataResult = exportTask.run()
        assertThat(exportTask.errorCause).isNull()
        assertThat(withDataResult).isEqualTo(JsonExportTask.SUCCESS)

        val exportWithData = exportFile.readText()
        println("Export with data")
        println(exportWithData)
        assertThat(exportWithData).isEqualTo("[{\"list_id\":\"list-1\",\"name\":\"First List\",\"order\":0,\"items\":[{\"list_item_id\":\"list-1-item-1\",\"tvdb_id\":0,\"externalId\":\"item-ref-1\",\"type\":\"tmdb-show\"},{\"list_item_id\":\"list-1-item-2\",\"tvdb_id\":0,\"externalId\":\"item-ref-2\",\"type\":\"show\"},{\"list_item_id\":\"list-1-item-3\",\"tvdb_id\":0,\"externalId\":\"item-ref-3\",\"type\":\"season\"},{\"list_item_id\":\"list-1-item-4\",\"tvdb_id\":0,\"externalId\":\"item-ref-4\",\"type\":\"episode\"}]},{\"list_id\":\"list-2\",\"name\":\"Empty List\",\"order\":1,\"items\":[]}]")
    }

    private val listOfTestLists = listOf(
        SgList().apply {
            listId = "list-1"
            name = "First List"
            order = 0
        },
        SgList().apply {
            listId = "list-2"
            name = "Empty List"
            order = 1
        }
    )

    private val listOfTestListItems = listOf(
        SgListItem().apply {
            listId = "list-1"
            listItemId = "list-1-item-1"
            itemRefId = "item-ref-1"
            type = SeriesGuideContract.ListItemTypes.TMDB_SHOW
        },
        SgListItem().apply {
            listId = "list-1"
            listItemId = "list-1-item-2"
            itemRefId = "item-ref-2"
            type = SeriesGuideContract.ListItemTypes.TVDB_SHOW
        },
        SgListItem().apply {
            listId = "list-1"
            listItemId = "list-1-item-3"
            itemRefId = "item-ref-3"
            type = SeriesGuideContract.ListItemTypes.SEASON
        },
        SgListItem().apply {
            listId = "list-1"
            listItemId = "list-1-item-4"
            itemRefId = "item-ref-4"
            type = SeriesGuideContract.ListItemTypes.EPISODE
        }
    )

    @Test
    fun exportMovies_jsonAsExpected() = runTest {
        val movieHelper = mock(MovieHelper::class.java)

        val exportTask = JsonExportTask(
            context,
            progressListener = null,
            isFullDump = true,
            isAutoBackupMode = false,
            type = JsonExportTask.BACKUP_MOVIES,
            mock(SgShow2Helper::class.java),
            mock(SgSeason2Helper::class.java),
            mock(SgEpisode2Helper::class.java),
            mock(SgListHelper::class.java),
            movieHelper
        )

        val exportFile = configureTestExportFile(exportTask)

        // No data
        val noDataResult = exportTask.run()
        assertThat(exportTask.errorCause).isNull()
        assertThat(noDataResult).isEqualTo(JsonExportTask.SUCCESS)

        val exportWithNoData = exportFile.readText()
        println("Export with no data")
        println(exportWithNoData)
        assertThat(exportWithNoData).isEqualTo("[]")

        // With data
        `when`(movieHelper.getMoviesForExport()).thenReturn(listOfTestMovies)
        val withDataResult = exportTask.run()
        assertThat(exportTask.errorCause).isNull()
        assertThat(withDataResult).isEqualTo(JsonExportTask.SUCCESS)

        val exportWithData = exportFile.readText()
        println("Export with data")
        println(exportWithData)
        assertThat(exportWithData).isEqualTo("[{\"tmdb_id\":1,\"imdb_id\":\"imdbidvalue\",\"title\":\"First Movie\",\"released_utc_ms\":1234567890,\"runtime_min\":123,\"poster\":\"/path/to/poster.jpg\",\"overview\":\"This is a movie description.\",\"in_collection\":true,\"in_watchlist\":true,\"watched\":true,\"plays\":2,\"last_updated_ms\":1234567890},{\"tmdb_id\":2,\"title\":\"Second Movie\",\"released_utc_ms\":9223372036854775807,\"runtime_min\":0,\"in_collection\":false,\"in_watchlist\":false,\"watched\":false,\"plays\":0,\"last_updated_ms\":0}]")
    }

    private val listOfTestMovies = listOf(
        SgMovie().apply {
            tmdbId = 1
            imdbId = "imdbidvalue"
            title = "First Movie"
            releasedMs = 1234567890
            runtimeMin = 123
            poster = "/path/to/poster.jpg"
            overview = "This is a movie description."
            inCollection = true
            inWatchlist = true
            plays = 2
            watched = true
            lastUpdated = 1234567890
        },
        SgMovie().apply {
            tmdbId = 2
            title = "Second Movie"
        }
    )

}