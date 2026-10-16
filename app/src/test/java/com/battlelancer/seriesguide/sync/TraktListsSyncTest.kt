// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.lists.database.SgList
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktSettings.InitialSync
import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.trakt5.entities.ListEntry
import com.uwetrottmann.trakt5.entities.ListIds
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
        setRegularSync(time)

        assertThat(createSync().sync(null)).isFalse()
        assertThat(fakeApi.calls).isEmpty()
    }

    @Test
    fun regularSync_notChanged_skips() {
        setRegularSync(time)

        val sync = createSync()
        assertThat(sync.sync(time)).isTrue()
        assertThat(sync.sync(time.minusMinutes(1))).isTrue()
        assertThat(fakeApi.calls).isEmpty()
    }

    @Test
    fun initialSync_notChanged_stillSyncs() {
        setInitialSync(InitialSync.EXISTING_LISTS_MERGE)
        TraktSettings.storeLastListsUpdatedAt(context, time)

        assertThat(createSync().sync(time)).isTrue()
        assertThat(fakeApi.calls).containsExactly("getLists")
    }

    @Test
    fun regularSync_getListsFails_noChanges() {
        setRegularSync(time)
        insertLocalList("A", "A", traktId = 1)
        fakeApi.lists = null

        assertThat(createSync().sync(timeLater)).isFalse()

        assertLocalListUnchanged("A", "A", traktId = 1)
        assertThat(TraktSettings.getLastListsUpdatedAt(context)).isEqualTo(millis(time))
    }

    @Test
    fun initialSyncDelete_getListsFails_keepsLists() {
        setInitialSync(InitialSync.EXISTING_LISTS_DELETE)
        insertLocalList("A", "A")
        fakeApi.lists = null

        assertThat(createSync().sync(time)).isFalse()

        assertLocalListUnchanged("A", "A")
        assertThat(TraktSettings.isInitialSyncLists(context)).isTrue()
    }

    @Test
    fun initialSyncDelete_missingTraktId_rollsBack() {
        setInitialSync(InitialSync.EXISTING_LISTS_DELETE)
        insertLocalList("A", "A")
        fakeApi.lists = listOf(traktList(1, "B"), traktList(null, "C"))

        assertThat(createSync().sync(time)).isFalse()

        assertLocalListUnchanged("A", "A")
        assertThat(TraktSettings.isInitialSyncLists(context)).isTrue()
    }

    @Test
    fun initialSyncDelete_blankName_rollsBack() {
        setInitialSync(InitialSync.EXISTING_LISTS_DELETE)
        insertLocalList("A", "A")
        fakeApi.lists = listOf(traktList(1, "B"), traktList(2, " "))

        assertThat(createSync().sync(time)).isFalse()

        assertLocalListUnchanged("A", "A")
        assertThat(TraktSettings.isInitialSyncLists(context)).isTrue()
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

    @Test
    fun initialSync_noSyncPreference_doesNothing() {
        setInitialSync(InitialSync.NO_SYNC)
        insertLocalList("A", "A")

        assertThat(createSync().sync(time)).isTrue()

        assertNothingDone()
    }

    @Test
    fun initialSync_noPreference_defaultsToNoSync() {
        insertLocalList("A", "A")

        assertThat(createSync().sync(time)).isTrue()

        assertNothingDone()
    }

    private fun assertNothingDone() {
        assertThat(fakeApi.calls).isEmpty()
        assertLocalListUnchanged("A", "A")
        assertThat(TraktSettings.isInitialSyncLists(context)).isTrue()
        assertThat(TraktSettings.getLastListsUpdatedAt(context)).isEqualTo(0)
    }

    private fun assertLocalListUnchanged(listId: String, name: String, traktId: Long? = null) {
        val lists = listHelper.getListsForExport()
        assertThat(lists).hasSize(1)
        assertThat(lists[0].listId).isEqualTo(listId)
        assertThat(lists[0].name).isEqualTo(name)
        assertThat(lists[0].traktId).isEqualTo(traktId)
    }

    private fun millis(time: OffsetDateTime) = time.toInstant().toEpochMilli()

    private fun traktList(id: Long?, name: String?): TraktList {
        val list = TraktList()
        list.ids = ListIds().also { it.trakt = id }
        list.name = name
        return list
    }

    private fun insertLocalList(listId: String, name: String, traktId: Long? = null) {
        listHelper.insertList(SgList(listId = listId, name = name, traktId = traktId))
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
        val calls = mutableListOf<String>()

        override suspend fun getLists() = lists.also { calls.add("getLists") }

        override suspend fun getListItems(traktListId: Long) =
            emptyList<ListEntry>().also { calls.add("getListItems $traktListId") }

        override suspend fun createList(name: String): Long? =
            null.also { calls.add("createList $name") }

        override suspend fun addItemsToList(traktListId: Long, items: SyncItems): SyncResponse? =
            null.also { calls.add("addItemsToList $traktListId") }
    }

}
