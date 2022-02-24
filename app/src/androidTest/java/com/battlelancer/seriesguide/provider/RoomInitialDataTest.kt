package com.battlelancer.seriesguide.provider

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomInitialDataTest {

    private var db: SgRoomDatabase? = null

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SgRoomDatabase::class.java)
            .addCallback(SgRoomDatabase.SgRoomCallback(context))
            .build()
    }

    @After
    fun closeDb() {
        db?.close()
    }

    @Test
    fun searchTableExists() {
        val query = ("SELECT name FROM sqlite_master "
                + "WHERE type='table' AND name=? "
                + "LIMIT 1")
        val args = arrayOf(Tables.EPISODES_SEARCH)
        val result = db!!.query(query, args)
        assertThat(result).isNotNull()
        assertThat(result.count).isEqualTo(1)
        result.close()
    }

    @Test
    fun firstListExists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstListName = context.getString(R.string.first_list)
        val query = "SELECT * FROM ${Tables.LISTS} WHERE ${Lists.NAME}=?"
        val args = arrayOf(firstListName)
        val result = db!!.query(query, args)
        assertThat(result).isNotNull()
        assertThat(result.count).isEqualTo(1)
        result.close()
    }
}