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

    abstract fun seasonHelper(): SeasonHelper

    abstract fun episodeHelper(): EpisodeHelper

    companion object {

        private const val VERSION_43_ROOM = 43
        private const val VERSION_44_ROOM_COPY = 44
        const val VERSION = VERSION_44_ROOM_COPY

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
                                    MIGRATION_43_44,
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
        val MIGRATION_43_44: Migration = object : Migration(VERSION_43_ROOM, VERSION_44_ROOM_COPY) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // show and episode tables have columns left over or with different types
                // from old versions where this did not matter
                // but for Room we have to copy all data into new tables
                // so migration validation succeeds

                // Create the new table
                database.execSQL(
                        "CREATE TABLE series_new (`_id` INTEGER NOT NULL, `seriestitle` TEXT NOT NULL, `series_title_noarticle` TEXT, `overview` TEXT, `airstime` INTEGER, `airsdayofweek` INTEGER, `series_airtime` TEXT, `series_timezone` TEXT, `firstaired` TEXT, `genres` TEXT, `network` TEXT, `rating` REAL, `series_rating_votes` INTEGER, `series_rating_user` INTEGER, `runtime` TEXT, `status` TEXT, `contentrating` TEXT, `next` TEXT, `poster` TEXT, `series_nextairdate` INTEGER, `nexttext` TEXT, `imdbid` TEXT, `series_trakt_id` INTEGER, `series_favorite` INTEGER NOT NULL, `series_syncenabled` INTEGER NOT NULL, `series_hidden` INTEGER NOT NULL, `series_lastupdate` INTEGER NOT NULL, `series_lastedit` INTEGER NOT NULL, `series_lastwatchedid` INTEGER NOT NULL, `series_lastwatched_ms` INTEGER NOT NULL, `series_language` TEXT, `series_unwatched_count` INTEGER NOT NULL, `series_notify` INTEGER NOT NULL, PRIMARY KEY(`_id`))")
                // Copy the data
                database.execSQL("INSERT INTO series_new (" +
                        "_id, seriestitle, series_title_noarticle, overview, airstime, airsdayofweek, series_airtime, series_timezone, firstaired, genres, network, rating, series_rating_votes, series_rating_user, runtime, status, contentrating, next, poster, series_nextairdate, nexttext, imdbid, series_trakt_id, series_favorite, series_syncenabled, series_hidden, series_lastupdate, series_lastedit, series_lastwatchedid, series_lastwatched_ms, series_language, series_unwatched_count, series_notify" +
                        ") SELECT " +
                        "_id, seriestitle, series_title_noarticle, overview, airstime, airsdayofweek, series_airtime, series_timezone, firstaired, genres, network, rating, series_rating_votes, series_rating_user, runtime, status, contentrating, next, poster, series_nextairdate, nexttext, imdbid, series_trakt_id, series_favorite, series_syncenabled, series_hidden, series_lastupdate, series_lastedit, series_lastwatchedid, series_lastwatched_ms, series_language, series_unwatched_count, series_notify"
                        + " FROM series")
                // Remove the old table
                database.execSQL("DROP TABLE series")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE series_new RENAME TO series")

                // Create the new table
                database.execSQL(
                        "CREATE TABLE episodes_new (`_id` INTEGER NOT NULL, `episodetitle` TEXT NOT NULL, `episodedescription` TEXT, `episodenumber` INTEGER NOT NULL, `season` INTEGER NOT NULL, `dvdnumber` REAL, `season_id` INTEGER NOT NULL, `series_id` INTEGER NOT NULL, `watched` INTEGER NOT NULL, `directors` TEXT, `gueststars` TEXT, `writers` TEXT, `episodeimage` TEXT, `episode_firstairedms` INTEGER NOT NULL, `episode_collected` INTEGER NOT NULL, `rating` REAL, `episode_rating_votes` INTEGER, `episode_rating_user` INTEGER, `episode_imdbid` TEXT, `episode_lastedit` INTEGER NOT NULL, `absolute_number` INTEGER, `episode_lastupdate` INTEGER NOT NULL, PRIMARY KEY(`_id`), FOREIGN KEY(`season_id`) REFERENCES `seasons`(`_id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`series_id`) REFERENCES `series`(`_id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
                // Copy the data
                database.execSQL("INSERT INTO episodes_new (" +
                        "_id, episodetitle, episodedescription, episodenumber, season, dvdnumber, season_id, series_id, watched, directors, gueststars, writers, episodeimage, episode_firstairedms, episode_collected, rating, episode_rating_votes, episode_rating_user, episode_imdbid, episode_lastedit, absolute_number, episode_lastupdate" +
                        ") SELECT " +
                        "_id, episodetitle, episodedescription, episodenumber, season, dvdnumber, season_id, series_id, watched, directors, gueststars, writers, episodeimage, episode_firstairedms, episode_collected, rating, episode_rating_votes, episode_rating_user, episode_imdbid, episode_lastedit, absolute_number, episode_lastupdate"
                        + " FROM episodes")
                // Remove the old table
                database.execSQL("DROP TABLE episodes")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE episodes_new RENAME TO episodes")
                // Re-create indexes
                database.execSQL("CREATE  INDEX `index_episodes_season_id` "
                        + "ON `episodes` (`season_id`)")
                database.execSQL("CREATE  INDEX `index_episodes_series_id` "
                        + "ON `episodes` (`series_id`)")
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
                if (SeriesGuideDatabase.isTableColumnMissing(database, "series", "actors")) {
                    database.execSQL("ALTER TABLE series ADD COLUMN actors TEXT DEFAULT '';")
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
