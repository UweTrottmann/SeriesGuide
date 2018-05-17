package com.battlelancer.seriesguide.provider

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.support.annotation.VisibleForTesting
import com.battlelancer.seriesguide.model.SgActivity
import com.battlelancer.seriesguide.model.SgEpisode
import com.battlelancer.seriesguide.model.SgJob
import com.battlelancer.seriesguide.model.SgList
import com.battlelancer.seriesguide.model.SgListItem
import com.battlelancer.seriesguide.model.SgMovie
import com.battlelancer.seriesguide.model.SgSeason
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.uwetrottmann.androidutils.AndroidUtils
import timber.log.Timber

@Database(entities = arrayOf(
        SgShow::class,
        SgSeason::class, SgEpisode::class,
        SgList::class,
        SgListItem::class, SgMovie::class,
        SgActivity::class,
        SgJob::class
), version = SgRoomDatabase.VERSION)
abstract class SgRoomDatabase : RoomDatabase() {

    abstract fun showHelper(): ShowHelper

    companion object {

        private const val VERSION_43_ROOM = 43
        const val VERSION = VERSION_43_ROOM

        @Volatile
        private var instance: SgRoomDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): SgRoomDatabase {
            // double-checked locking, adapted from lazy implementation
            val existing = instance
            if (existing != null) { // First check (no locking)
                return existing
            }

            return synchronized(this) {
                val existingInLock = instance
                if (existingInLock != null) { // Second check (with locking)
                    existingInLock
                } else {
                    val newInstance = Room.databaseBuilder(context.applicationContext,
                            SgRoomDatabase::class.java, SeriesGuideDatabase.DATABASE_NAME)
                            .addMigrations(
                                    MIGRATION_42_43,
                                    MIGRATION_41_43,
                                    MIGRATION_40_43,
                                    MIGRATION_39_43,
                                    MIGRATION_38_43
                            )
                            .addCallback(CALLBACK)
                            .build()
                    instance = newInstance
                    newInstance
                }
            }
        }

        /**
         * Changes the instance to an in memory database for content provider unit testing.
         */
        @VisibleForTesting
        @JvmStatic
        fun switchToInMemory(context: Context) {
            instance = Room.inMemoryDatabaseBuilder(context.applicationContext,
                    SgRoomDatabase::class.java)
                    .addMigrations(MIGRATION_42_43)
                    .addCallback(CALLBACK)
                    .build()
        }

        @JvmField
        val CALLBACK: RoomDatabase.Callback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // manually create FTS table not supported by Room
                if (AndroidUtils.isJellyBeanOrHigher()) {
                    db.execSQL(SeriesGuideDatabase.CREATE_SEARCH_TABLE)
                } else {
                    db.execSQL(SeriesGuideDatabase.CREATE_SEARCH_TABLE_API_ICS)
                }
            }
        }

        @JvmField
        val MIGRATION_42_43: Migration = object : Migration(
                SeriesGuideDatabase.DBVER_42_JOBS, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 42 to 43")
                // Room uses an own database hash to uniquely identify the database
                // Since version 42 does not use Room, it doesn't have the database hash associated.
                // By implementing a Migration class, we're telling Room that it should use the data
                // from version 42 to version 43.
                // If no migration is provided, then the tables will be dropped and recreated.
                // Since we didn't alter the tables, there's not much to do here.

                // We dropped the actors column on new installations for v38
                // but installations before that still have it
                // so make it required again to avoid having to copy the table
                if (SeriesGuideDatabase.isTableColumnMissing(database, Tables.SHOWS,
                                Shows.ACTORS)) {
                    database.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
                            + Shows.ACTORS + " TEXT DEFAULT '';")
                }

                // Room does not support UNIQUE constraints, so use unique indexes instead (probably
                // faster anyhow)
                database.execSQL("CREATE UNIQUE INDEX `index_lists_list_id` "
                        + "ON `lists` (`list_id`)")
                database.execSQL("CREATE UNIQUE INDEX `index_listitems_list_item_id` "
                        + "ON `listitems` (`list_item_id`)")
                database.execSQL("CREATE UNIQUE INDEX `index_movies_movies_tmdbid` "
                        + "ON `movies` (`movies_tmdbid`)")
                database.execSQL("CREATE UNIQUE INDEX `index_activity_activity_episode` "
                        + "ON `activity` (`activity_episode`)")
                database.execSQL("CREATE UNIQUE INDEX `index_jobs_job_created_at` "
                        + "ON `jobs` (`job_created_at`)")

                // Room suggests adding indices for foreign key columns, so add them
                database.execSQL("CREATE  INDEX `index_seasons_series_id` "
                        + "ON `seasons` (`series_id`)")
                database.execSQL("CREATE  INDEX `index_episodes_season_id` "
                        + "ON `episodes` (`season_id`)")
                database.execSQL("CREATE  INDEX `index_episodes_series_id` "
                        + "ON `episodes` (`series_id`)")
                database.execSQL("CREATE  INDEX `index_listitems_list_id` "
                        + "ON `listitems` (`list_id`)")
            }
        }

        // can not update pre-Room versions incrementally, always need to update to first Room version
        // so only provide upgrade support from version 38 (used in SG 26 from end 2015)
        private val MIGRATION_41_43 = object : Migration(
                SeriesGuideDatabase.DBVER_41_EPISODE_LAST_UPDATED, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 41 to 43")
                SeriesGuideDatabase.upgradeToFortyTwo(database)
                MIGRATION_42_43.migrate(database)
            }
        }

        private val MIGRATION_40_43 = object : Migration(
                SeriesGuideDatabase.DBVER_40_NOTIFY_PER_SHOW, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 40 to 43")
                SeriesGuideDatabase.upgradeToFortyOne(database)
                MIGRATION_41_43.migrate(database)
            }
        }

        private val MIGRATION_39_43 = object : Migration(
                SeriesGuideDatabase.DBVER_39_SHOW_LAST_WATCHED, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 39 to 43")
                SeriesGuideDatabase.upgradeToForty(database)
                MIGRATION_40_43.migrate(database)
            }
        }

        private val MIGRATION_38_43 = object : Migration(
                SeriesGuideDatabase.DBVER_38_SHOW_TRAKT_ID, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 38 to 43")
                SeriesGuideDatabase.upgradeToThirtyNine(database)
                MIGRATION_39_43.migrate(database)
            }
        }
    }
}
