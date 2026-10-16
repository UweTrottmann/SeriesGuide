// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.lists.database.SgList
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.lists.database.SgListItem
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktSettings.InitialSync
import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.trakt5.entities.ListEntry
import com.uwetrottmann.trakt5.entities.ListIds
import com.uwetrottmann.trakt5.entities.Movie
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.TraktList
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

/**
 * Tests using Room are recommended to be run on an actual device due to SQLite differences,
 * but these tests only use basic queries.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class TraktListsSyncTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var listHelper: SgListHelper
    private val fakeApi = FakeTraktListsApi()

    private val time = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    private val timeLater = time.plusMinutes(1)

    @Before
    fun switchToInMemoryDb() {
        // Use an in-memory database for testing with Room
        SgRoomDatabase.switchToInMemory(context)
        listHelper = SgRoomDatabase.getInstance(context).sgListHelper()
        // A new database contains a default list
        listHelper.deleteAllListsAndItems()
    }

    @After
    fun closeDb() {
        SgRoomDatabase.getInstance(context).close()
    }

    private fun createSync() = TraktListsSync(context, fakeApi)

    @Test
    fun nullUpdatedAt_fails() {
        assertThat(createSync().sync(null)).isFalse()
        assertThat(fakeApi.calls).isEmpty()
    }

    @Test
    fun regularSync_notChanged_doesNothing() {
        setRegularSync(time)
        insertLocalList()

        val sync = createSync()
        assertThat(sync.sync(time)).isTrue()
        assertThat(sync.sync(time.minusMinutes(1))).isTrue()

        assertThat(fakeApi.calls).isEmpty()
        assertLocalListUnchanged()
    }

    @Test
    fun initialSync_notChanged_stillSyncs() {
        // Change to merge mode, as default is "no sync" mode
        setInitialSync(InitialSync.EXISTING_LISTS_MERGE)
        TraktSettings.storeLastListsUpdatedAt(context, time)

        assertThat(createSync().sync(time)).isTrue()
        assertThat(fakeApi.calls).containsExactly("getLists")
    }

    @Test
    fun initialSync_noSync_doesNothing() {
        insertLocalList()

        // Default is to not sync
        assertThat(createSync().sync(time)).isTrue()

        assertThat(fakeApi.calls).isEmpty()
        assertLocalListUnchanged()
        // Initial sync setting remains unchanged, no last updated time set
        assertThat(TraktSettings.isInitialSyncLists(context)).isTrue()
        assertThat(TraktSettings.getLastListsUpdatedAt(context)).isEqualTo(0)
    }

    @Test
    fun sync_getLists_fails_noChanges() {
        setRegularSync(time)
        insertLocalList(traktId = 1)
        fakeApi.lists = null

        assertThat(createSync().sync(timeLater)).isFalse()

        assertLocalListUnchanged(traktId = 1)
        assertThat(TraktSettings.getLastListsUpdatedAt(context)).isEqualTo(millis(time))
    }

    @Test
    fun sync_missingTraktId_blankName_rollsBackChanges() {
        setInitialSync(InitialSync.EXISTING_LISTS_DELETE)
        insertLocalList()

        // Missing Trakt ID
        fakeApi.lists = listOf(traktList(1, "B"), traktList(null, "C"))
        assertThat(createSync().sync(time)).isFalse()

        // Blank list name
        fakeApi.lists = listOf(traktList(1, "B"), traktList(2, " "))
        assertThat(createSync().sync(time)).isFalse()

        assertLocalListUnchanged()
        assertThat(TraktSettings.isInitialSyncLists(context)).isTrue()
    }

    @Test
    fun initialSync_delete_replacesLocalLists() {
        setInitialSync(InitialSync.EXISTING_LISTS_DELETE)
        insertLocalList()
        insertLocalListItem(TEST_LIST_NAME_AND_ID, 30, ListItemTypes.TMDB_SHOW)
        val listIdB = "B"
        val listIdC = "C"
        val traktIdB = 1L
        val traktIdC = 2L
        val traktListB = traktList(traktIdB, listIdB)
        val traktListC = traktList(traktIdC, listIdC)
        fakeApi.lists = listOf(traktListB, traktListC)
        fakeApi.listItems = mapOf(
            traktIdB to listOf(showEntry(10), movieEntry(20)),
            traktIdC to listOf(movieEntry(21))
        )

        assertThat(createSync().sync(time)).isTrue()

        assertThat(
            listHelper.getListsForExport().map { Triple(it.listId, it.name, it.traktId) }
        ).containsExactly(
            Triple(listIdB, listIdB, traktIdB),
            Triple(listIdC, listIdC, traktIdC)
        )
        assertThat(listItemKeys(TEST_LIST_NAME_AND_ID)).isEmpty()
        assertThat(listItemKeys(listIdB)).containsExactly(
            ListItemTypes.TMDB_SHOW to "10",
            ListItemTypes.TMDB_MOVIE to "20"
        )
        assertThat(listItemKeys(listIdC)).containsExactly(
            ListItemTypes.TMDB_MOVIE to "21"
        )
        // Nothing uploaded: lists from Trakt have a Trakt ID and no items yet when uploading
        assertThat(fakeApi.calls)
            .containsExactly("getLists", "getListItems $traktIdB", "getListItems $traktIdC")
            .inOrder()
        assertThat(TraktSettings.isInitialSyncLists(context)).isFalse()
    }

    @Test
    fun initialSync_merge_sameName_mergesFirstAndAddsOthers() {
        setInitialSync(InitialSync.EXISTING_LISTS_MERGE)
        insertLocalList()
        insertLocalListItem(TEST_LIST_NAME_AND_ID, 30, ListItemTypes.TMDB_SHOW)
        val traktIdFirst = 1L
        val traktIdSecond = 2L
        fakeApi.lists = listOf(
            traktList(traktIdFirst, TEST_LIST_NAME_AND_ID),
            traktList(traktIdSecond, TEST_LIST_NAME_AND_ID)
        )
        fakeApi.listItems = mapOf(
            // Contains the created local show item plus a movie item
            traktIdFirst to listOf(showEntry(30), movieEntry(20)),
            traktIdSecond to listOf(movieEntry(21))
        )

        assertThat(createSync().sync(time)).isTrue()

        // The first Trakt list is merged with the local list (matched by name),
        // the second is added with a non-colliding list ID.
        val listIdSecond = "$TEST_LIST_NAME_AND_ID-2"
        assertThat(
            listHelper.getListsForExport().map { Triple(it.listId, it.name, it.traktId) }
        ).containsExactly(
            Triple(TEST_LIST_NAME_AND_ID, TEST_LIST_NAME_AND_ID, traktIdFirst),
            Triple(listIdSecond, TEST_LIST_NAME_AND_ID, traktIdSecond)
        )
        assertThat(listItemKeys(TEST_LIST_NAME_AND_ID)).containsExactly(
            ListItemTypes.TMDB_SHOW to "30",
            ListItemTypes.TMDB_MOVIE to "20"
        )
        assertThat(listItemKeys(listIdSecond)).containsExactly(
            ListItemTypes.TMDB_MOVIE to "21"
        )
        // Only the local item was uploaded, only to the first list, no list was created
        assertThat(fakeApi.calls).containsExactly(
            "getLists",
            "addItemsToList $traktIdFirst",
            "getListItems $traktIdFirst",
            "getListItems $traktIdSecond"
        ).inOrder()
        val addedItems = fakeApi.addedItems.getValue(traktIdFirst)
        assertThat(addedItems.shows!!.map { it.ids!!.tmdb }).containsExactly(30)
        assertThat(addedItems.movies).isEmpty()

        // Finally, initial sync was marked completed
        assertThat(TraktSettings.isInitialSyncLists(context)).isFalse()
    }

    @Test
    fun initialSync_success_completesAndStoresTime() {
        setInitialSync(InitialSync.EXISTING_LISTS_DELETE)

        assertThat(createSync().sync(time)).isTrue()

        assertThat(TraktSettings.isInitialSyncLists(context)).isFalse()
        assertThat(TraktSettings.getLastListsUpdatedAt(context)).isEqualTo(millis(time))
    }

    @Test
    fun regularSync_success_storesTime() {
        setRegularSync(time)

        assertThat(createSync().sync(timeLater)).isTrue()

        assertThat(TraktSettings.getLastListsUpdatedAt(context)).isEqualTo(millis(timeLater))
    }

    private fun insertLocalList(traktId: Long? = null) {
        listHelper.insertList(
            SgList(
                listId = TEST_LIST_NAME_AND_ID,
                name = TEST_LIST_NAME_AND_ID,
                traktId = traktId
            )
        )
    }

    private fun assertLocalListUnchanged(traktId: Long? = null) {
        val lists = listHelper.getListsForExport()
        assertThat(lists).hasSize(1)
        assertThat(lists[0].listId).isEqualTo(TEST_LIST_NAME_AND_ID)
        assertThat(lists[0].name).isEqualTo(TEST_LIST_NAME_AND_ID)
        assertThat(lists[0].traktId).isEqualTo(traktId)
    }

    private fun insertLocalListItem(listId: String, tmdbId: Int, type: Int) {
        listHelper.insertListItems(listOf(SgListItem(tmdbId, type, listId)))
    }

    private fun listItemKeys(listId: String) =
        listHelper.getListItemsForExport(listId).map { it.type to it.itemRefId }

    private fun showEntry(tmdbId: Int) =
        ListEntry().also { it.show = Show().also { show -> show.ids = ShowIds.tmdb(tmdbId) } }

    private fun movieEntry(tmdbId: Int) =
        ListEntry().also { it.movie = Movie().also { movie -> movie.ids = MovieIds.tmdb(tmdbId) } }

    private fun millis(time: OffsetDateTime) = time.toInstant().toEpochMilli()

    private fun traktList(id: Long?, name: String?): TraktList {
        val list = TraktList()
        list.ids = ListIds().also { it.trakt = id }
        list.name = name
        return list
    }

    private fun setRegularSync(lastUpdatedAt: OffsetDateTime) {
        TraktSettings.setInitialSyncListsCompleted(context)
        TraktSettings.storeLastListsUpdatedAt(context, lastUpdatedAt)
    }

    private fun setInitialSync(preference: InitialSync) {
        TraktSettings.setInitialSyncListsPreference(context, preference)
    }

    private class FakeTraktListsApi : TraktListsApi {
        /** Returned by getLists, null simulates an error. */
        var lists: List<TraktList>? = emptyList()

        /** Returned by getListItems by Trakt list ID, empty if not set. */
        var listItems: Map<Long, List<ListEntry>> = emptyMap()

        val calls = mutableListOf<String>()

        override suspend fun getLists() = lists.also { calls.add("getLists") }

        override suspend fun getListItems(traktListId: Long): List<ListEntry>? =
            (listItems[traktListId] ?: emptyList()).also { calls.add("getListItems $traktListId") }

        override suspend fun createList(name: String): Long? =
            null.also { calls.add("createList $name") }

        /** Items passed to addItemsToList by Trakt list ID. */
        val addedItems = mutableMapOf<Long, SyncItems>()

        override suspend fun addItemsToList(traktListId: Long, items: SyncItems): SyncResponse? {
            calls.add("addItemsToList $traktListId")
            addedItems[traktListId] = items
            return SyncResponse()
        }
    }

    companion object {
        private const val TEST_LIST_NAME_AND_ID = "A"
    }
}
