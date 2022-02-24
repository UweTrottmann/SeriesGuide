package com.battlelancer.seriesguide.provider

import android.content.Context
import android.database.sqlite.SQLiteQueryBuilder
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.ActivityType
import com.battlelancer.seriesguide.model.SgActivity
import com.battlelancer.seriesguide.model.SgEpisode
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgJob
import com.battlelancer.seriesguide.model.SgList
import com.battlelancer.seriesguide.model.SgListItem
import com.battlelancer.seriesguide.model.SgMovie
import com.battlelancer.seriesguide.model.SgSeason
import com.battlelancer.seriesguide.model.SgSeason2
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.model.SgWatchProvider
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import timber.log.Timber

@Database(
    entities = [
        SgShow::class,
        SgSeason::class,
        SgEpisode::class,
        SgShow2::class,
        SgSeason2::class,
        SgEpisode2::class,
        SgList::class,
        SgListItem::class,
        SgMovie::class,
        SgActivity::class,
        SgJob::class,
        SgWatchProvider::class
    ],
    version = SgRoomDatabase.VERSION
)
abstract class SgRoomDatabase : RoomDatabase() {

    abstract fun sgShow2Helper(): SgShow2Helper
    abstract fun sgSeason2Helper(): SgSeason2Helper
    abstract fun sgEpisode2Helper(): SgEpisode2Helper
    abstract fun sgActivityHelper(): SgActivityHelper
    abstract fun sgListHelper(): SgListHelper

    abstract fun movieHelper(): MovieHelper

    abstract fun sgWatchProviderHelper(): SgWatchProviderHelper

    class SgRoomCallback(context: Context) : Callback() {
        private val context = context.applicationContext

        override fun onCreate(db: SupportSQLiteDatabase) {
            // manually create FTS table, not supported by Room
            db.execSQL(SeriesGuideDatabase.CREATE_SEARCH_TABLE)
            // Add initial data, currently only first list
            val listName = context.getString(R.string.first_list)
            val listId = Lists.generateListId(listName)
            val stmt= db.compileStatement("INSERT INTO `${Tables.LISTS}` (`${Lists.LIST_ID}`,`${Lists.NAME}`,`${Lists.ORDER}`) VALUES (?,?,?)")
            stmt.bindString(1, listId)
            stmt.bindString(2, listName)
            stmt.bindLong(3, 0)
            stmt.executeInsert()
            //db.execSQL("INSERT INTO `${Tables.LISTS}` (`${Lists.LIST_ID}`,`${Lists.NAME}`,`${Lists.ORDER}`) VALUES (`$listId`,`$listName`,0)")
        }
    }

    companion object {

        const val VERSION_43_ROOM = 43
        const val VERSION_44_RECREATE_SERIES_EPISODES = 44
        const val VERSION_45_RECREATE_SEASONS = 45
        const val VERSION_46_SERIES_SLUG = 46
        const val VERSION_47_SERIES_POSTER_THUMB = 47
        const val VERSION_48_EPISODE_PLAYS = 48

        /**
         * Shows, seasons and episodes now use auto-generated row IDs,
         * a TMDB ID column is introduced. Also renames some columns for consistency.
         *
         * Also adds new activity table column modifies its index.
         */
        const val VERSION_49_AUTO_ID_MIGRATION = 49

        /**
         * Added [SgWatchProvider] table.
         */
        const val VERSION_50_WATCH_PROVIDERS = 50
        const val VERSION = VERSION_50_WATCH_PROVIDERS

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
                    val newInstance = Room
                        .databaseBuilder(
                            context.applicationContext,
                            SgRoomDatabase::class.java,
                            SeriesGuideDatabase.DATABASE_NAME
                        ).addMigrations(
                            MIGRATION_49_50,
                            MIGRATION_48_49,
                            MIGRATION_47_48,
                            MIGRATION_46_47,
                            MIGRATION_45_46,
                            MIGRATION_44_45,
                            MIGRATION_42_44,
                            MIGRATION_43_44,
                            MIGRATION_42_43,
                            MIGRATION_41_43,
                            MIGRATION_40_43,
                            MIGRATION_39_43,
                            MIGRATION_38_43,
                            MIGRATION_37_43,
                            MIGRATION_36_43,
                            MIGRATION_35_43,
                            MIGRATION_34_43
                        )
                        .addCallback(SgRoomCallback(context))
                        .allowMainThreadQueries()
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
            instance = Room
                .inMemoryDatabaseBuilder(context.applicationContext, SgRoomDatabase::class.java)
                .addMigrations(MIGRATION_42_43)
                .addCallback(SgRoomCallback(context))
                .allowMainThreadQueries()
                .build()
        }

        data class SeasonIds(
            val seasonId: Int,
            val showId: Int,
            val seasonTvdbId: Int
        )

        @JvmField
        val MIGRATION_49_50: Migration = object :
        Migration(VERSION_49_AUTO_ID_MIGRATION, VERSION_50_WATCH_PROVIDERS) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 49 to 50")

                // Create new table
                @Suppress("LocalVariableName") val TABLE_NAME = "sg_watch_provider"
                database.execSQL("CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `provider_id` INTEGER NOT NULL, `type` INTEGER NOT NULL, `provider_name` TEXT NOT NULL, `display_priority` INTEGER NOT NULL, `logo_path` TEXT NOT NULL, `enabled` INTEGER NOT NULL)")
                // Create indices
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sg_watch_provider_provider_id_type` ON `${TABLE_NAME}` (`provider_id`, `type`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_sg_watch_provider_provider_name` ON `${TABLE_NAME}` (`provider_name`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_sg_watch_provider_display_priority` ON `${TABLE_NAME}` (`display_priority`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_sg_watch_provider_enabled` ON `${TABLE_NAME}` (`enabled`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_sg_watch_provider_type` ON `${TABLE_NAME}` (`type`)")
            }
        }

        @JvmField
        val MIGRATION_48_49: Migration = object :
            Migration(VERSION_48_EPISODE_PLAYS, VERSION_49_AUTO_ID_MIGRATION) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 48 to 49")

                // Creates new tables sg_show, sg_season and sg_episode;
                // keeps old data in old tables as backup if something
                // goes horribly wrong with the new schema.

                // Shows
                // Create the new table
                database.execSQL("CREATE TABLE `sg_show` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `series_tmdb_id` INTEGER, `series_tvdb_id` INTEGER, `series_slug` TEXT, `series_trakt_id` INTEGER, `series_title` TEXT NOT NULL, `series_title_noarticle` TEXT, `series_overview` TEXT, `series_airstime` INTEGER, `series_airsdayofweek` INTEGER, `series_country` TEXT, `series_timezone` TEXT, `series_firstaired` TEXT, `series_genres` TEXT, `series_network` TEXT, `series_imdbid` TEXT, `series_rating` REAL, `series_rating_votes` INTEGER, `series_rating_user` INTEGER, `series_runtime` INTEGER, `series_status` INTEGER, `series_contentrating` TEXT, `series_next` TEXT, `series_poster` TEXT, `series_poster_small` TEXT, `series_nextairdate` INTEGER, `series_nexttext` TEXT, `series_lastupdate` INTEGER NOT NULL, `series_lastedit` INTEGER NOT NULL, `series_lastwatchedid` INTEGER NOT NULL, `series_lastwatched_ms` INTEGER NOT NULL, `series_language` TEXT, `series_unwatched_count` INTEGER NOT NULL, `series_favorite` INTEGER NOT NULL, `series_hidden` INTEGER NOT NULL, `series_notify` INTEGER NOT NULL, `series_syncenabled` INTEGER NOT NULL)")
                // Copy the data
                // Note: replacing series_next with default value as it changes from
                // episode TVDB ID to row ID.
                database.execSQL("INSERT INTO sg_show (series_tvdb_id, series_slug, series_title, series_title_noarticle, series_overview, series_airstime, series_airsdayofweek, series_country, series_timezone, series_firstaired, series_genres, series_network, series_rating, series_rating_votes, series_rating_user, series_runtime, series_status, series_contentrating, series_next, series_poster, series_poster_small, series_nextairdate, series_nexttext, series_imdbid, series_trakt_id, series_favorite, series_syncenabled, series_hidden, series_lastupdate, series_lastedit, series_lastwatchedid, series_lastwatched_ms, series_language, series_unwatched_count, series_notify) SELECT _id, series_slug, seriestitle, series_title_noarticle, overview, airstime, airsdayofweek, series_airtime, series_timezone, firstaired, genres, network, rating, series_rating_votes, series_rating_user, runtime, status, contentrating, '', poster, series_poster_small, series_nextairdate, nexttext, imdbid, series_trakt_id, series_favorite, series_syncenabled, series_hidden, series_lastupdate, series_lastedit, series_lastwatchedid, series_lastwatched_ms, series_language, series_unwatched_count, series_notify FROM series")
                // Create table indexes
                database.execSQL("CREATE INDEX `index_sg_show_series_tmdb_id` ON `sg_show` (`series_tmdb_id`)")
                database.execSQL("CREATE INDEX `index_sg_show_series_tvdb_id` ON `sg_show` (`series_tvdb_id`)")

                // Seasons
                // Create the new table
                database.execSQL("CREATE TABLE `sg_season` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `series_id` INTEGER NOT NULL, `season_tmdb_id` TEXT, `season_tvdb_id` INTEGER, `season_number` INTEGER, `season_name` TEXT, `season_order` INTEGER NOT NULL, `season_watchcount` INTEGER, `season_willaircount` INTEGER, `season_noairdatecount` INTEGER, `season_totalcount` INTEGER, `season_tags` TEXT, FOREIGN KEY(`series_id`) REFERENCES `sg_show`(`_id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
                // Copy the data, ignore (skip) rows with constraint violations.
                val showsQuery = database.query(
                    SQLiteQueryBuilder.buildQueryString(
                        false,
                        "sg_show",
                        arrayOf("_id", "series_tvdb_id"),
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                )
                val showTvdbIdsToIds = showsQuery.use {
                    val array = ArrayList<Pair<Int, Int>>(it.count)
                    while (it.moveToNext()) {
                        val id = it.getInt(0)
                        val tvdbId = it.getInt(1)
                        array.add(Pair(tvdbId, id))
                    }
                    array
                }
                showTvdbIdsToIds.forEach {
                    val showTvdbId = it.first
                    val showId = it.second
                    database.execSQL("INSERT OR IGNORE INTO sg_season (series_id, season_tvdb_id, season_number, season_order, season_watchcount, season_willaircount, season_noairdatecount, season_tags, season_totalcount) SELECT $showId, _id, combinednr, combinednr, watchcount, willaircount, noairdatecount, seasonposter, season_totalcount FROM seasons WHERE series_id=$showTvdbId")
                }
                // Create table indexes
                database.execSQL("CREATE INDEX `index_sg_season_series_id` ON `sg_season` (`series_id`)")

                // Episodes
                // Create the new table
                database.execSQL("CREATE TABLE `sg_episode` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `season_id` INTEGER NOT NULL, `series_id` INTEGER NOT NULL, `episode_tmdb_id` INTEGER, `episode_tvdb_id` INTEGER, `episode_title` TEXT, `episode_description` TEXT, `episode_number` INTEGER NOT NULL, `episode_absolute_number` INTEGER, `episode_season_number` INTEGER NOT NULL, `episode_order` INTEGER NOT NULL, `episode_dvd_number` REAL, `episode_watched` INTEGER NOT NULL, `episode_plays` INTEGER, `episode_collected` INTEGER NOT NULL, `episode_directors` TEXT, `episode_gueststars` TEXT, `episode_writers` TEXT, `episode_image` TEXT, `episode_firstairedms` INTEGER NOT NULL, `episode_rating` REAL, `episode_rating_votes` INTEGER, `episode_rating_user` INTEGER, `episode_imdbid` TEXT, `episode_lastedit` INTEGER NOT NULL, `episode_lastupdate` INTEGER NOT NULL, FOREIGN KEY(`series_id`) REFERENCES `sg_show`(`_id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
                // Copy the data, ignore (skip) rows with constraint violations.
                val seasonsQuery = database.query(
                    SQLiteQueryBuilder.buildQueryString(
                        false,
                        "sg_season",
                        arrayOf("_id", "series_id", "season_tvdb_id"),
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                )
                val seasonTvdbIdsToIds = seasonsQuery.use {
                    val array = ArrayList<SeasonIds>(it.count)
                    while (it.moveToNext()) {
                        val id = it.getInt(0)
                        val showId = it.getInt(1)
                        val tvdbId = it.getInt(2)
                        array.add(SeasonIds(id, showId, tvdbId))
                    }
                    array
                }
                seasonTvdbIdsToIds.forEach {
                    val seasonTvdbId = it.seasonTvdbId
                    val seasonId = it.seasonId
                    val showId = it.showId
                    database.execSQL("INSERT OR IGNORE INTO sg_episode (season_id, series_id, episode_tvdb_id, episode_title, episode_description, episode_number, episode_season_number, episode_order, episode_dvd_number, episode_watched, episode_plays, episode_directors, episode_gueststars, episode_writers, episode_image, episode_firstairedms, episode_collected, episode_rating, episode_rating_votes, episode_rating_user, episode_imdbid, episode_lastedit, episode_absolute_number, episode_lastupdate) SELECT $seasonId, $showId, _id, episodetitle, episodedescription, episodenumber, season, episodenumber, dvdnumber, watched, plays, directors, gueststars, writers, episodeimage, episode_firstairedms, episode_collected, rating, episode_rating_votes, episode_rating_user, episode_imdbid, episode_lastedit, absolute_number, episode_lastupdate FROM episodes WHERE season_id=$seasonTvdbId")
                }
                // Create table indexes
                database.execSQL("CREATE INDEX `index_sg_episode_season_id` ON `sg_episode` (`season_id`)")
                database.execSQL("CREATE INDEX `index_sg_episode_series_id` ON `sg_episode` (`series_id`)")

                // Add new column to the activity table, add it to unique index.
                // Note: setting default value, as it is easier than creating a totally new table.
                database.execSQL("ALTER TABLE activity ADD COLUMN activity_type INTEGER NOT NULL DEFAULT ${ActivityType.TVDB_ID}")
                database.execSQL("UPDATE activity SET activity_type = ${ActivityType.TVDB_ID}")
                database.execSQL("DROP INDEX IF EXISTS index_activity_activity_episode")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_activity_activity_episode_activity_type` ON `activity` (`activity_episode`, `activity_type`)")
            }
        }

        @JvmField
        val MIGRATION_47_48: Migration = object :
            Migration(VERSION_47_SERIES_POSTER_THUMB, VERSION_48_EPISODE_PLAYS) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 47 to 48")

                database.execSQL("ALTER TABLE episodes ADD COLUMN plays INTEGER;")
                database.execSQL("UPDATE episodes SET plays = 1 WHERE watched = 1;")
                database.execSQL("UPDATE episodes SET plays = 0 WHERE watched != 1;")
                // Movies already have plays column, but also prepopulate it.
                database.execSQL("UPDATE movies SET movies_plays = 1 WHERE movies_watched = 1;")
                database.execSQL("UPDATE movies SET movies_plays = 0 WHERE movies_watched = 0;")
            }
        }

        @JvmField
        val MIGRATION_46_47: Migration = object :
            Migration(VERSION_46_SERIES_SLUG, VERSION_47_SERIES_POSTER_THUMB) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 46 to 47")

                database.execSQL("ALTER TABLE series ADD COLUMN series_poster_small TEXT;")
                // || concatenates strings.
                database.execSQL("UPDATE series SET series_poster_small = '_cache/' || poster WHERE poster NOT NULL AND poster != '';")
            }
        }

        @JvmField
        val MIGRATION_45_46: Migration = object :
                Migration(VERSION_45_RECREATE_SEASONS, VERSION_46_SERIES_SLUG) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 45 to 46")

                database.execSQL("ALTER TABLE series ADD COLUMN series_slug TEXT;")
            }
        }

        /**
         * Recreates seasons table to pass migration validation which failed due to
         * columns with unexpected types likely due to using legacy backup tool.
         */
        @JvmField
        val MIGRATION_44_45: Migration = object : Migration(
                VERSION_44_RECREATE_SERIES_EPISODES, VERSION_45_RECREATE_SEASONS) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 44 to 45")

                // Create the new table
                database.execSQL(
                        "CREATE TABLE `seasons_new` (`_id` INTEGER, `combinednr` INTEGER, `series_id` TEXT, `watchcount` INTEGER, `willaircount` INTEGER, `noairdatecount` INTEGER, `seasonposter` TEXT, `season_totalcount` INTEGER, PRIMARY KEY(`_id`), FOREIGN KEY(`series_id`) REFERENCES `series`(`_id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
                // Copy the data
                // ignore (skip) rows with constraint violations
                database.execSQL("INSERT OR IGNORE INTO seasons_new (" +
                        "_id, combinednr, series_id, watchcount, willaircount, noairdatecount, seasonposter, season_totalcount" +
                        ") SELECT " +
                        "_id, combinednr, series_id, watchcount, willaircount, noairdatecount, seasonposter, season_totalcount"
                        + " FROM seasons")
                // Remove the old table
                database.execSQL("DROP TABLE seasons")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE seasons_new RENAME TO seasons")

                // Re-create seasons table index
                database.execSQL("CREATE  INDEX `index_seasons_series_id` "
                        + "ON `seasons` (`series_id`)")
            }
        }

        /**
         * Quick path from pre-Room, skips actors column check.
         */
        @JvmField
        val MIGRATION_42_44: Migration = object : Migration(
                SeriesGuideDatabase.DBVER_42_JOBS, VERSION_44_RECREATE_SERIES_EPISODES) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 42 to 44")
                MIGRATION_43_44.migrate(database)

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
                // Skip those for episodes table, created by 43->44 migration
                database.execSQL("CREATE  INDEX `index_seasons_series_id` "
                        + "ON `seasons` (`series_id`)")
                database.execSQL("CREATE  INDEX `index_listitems_list_id` "
                        + "ON `listitems` (`list_id`)")
            }
        }

        /**
         * Recreates series and episodes tables to pass migration validation which failed due to
         * left over columns and columns that have changed types from pre-Room.
         */
        @JvmField
        val MIGRATION_43_44: Migration = object : Migration(
                VERSION_43_ROOM, VERSION_44_RECREATE_SERIES_EPISODES) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 43 to 44")

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
                // ignore (skip) rows with constraint violations
                database.execSQL("INSERT OR IGNORE INTO episodes_new (" +
                        "_id, episodetitle, episodedescription, episodenumber, season, dvdnumber, season_id, series_id, watched, directors, gueststars, writers, episodeimage, episode_firstairedms, episode_collected, rating, episode_rating_votes, episode_rating_user, episode_imdbid, episode_lastedit, absolute_number, episode_lastupdate" +
                        ") SELECT " +
                        "_id, episodetitle, episodedescription, episodenumber, season, dvdnumber, season_id, series_id, watched, directors, gueststars, writers, episodeimage, episode_firstairedms, episode_collected, rating, episode_rating_votes, episode_rating_user, episode_imdbid, episode_lastedit, absolute_number, episode_lastupdate"
                        + " FROM episodes")
                // Remove the old table
                database.execSQL("DROP TABLE episodes")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE episodes_new RENAME TO episodes")

                // Re-create episodes table indexes
                database.execSQL("CREATE  INDEX `index_episodes_season_id` "
                        + "ON `episodes` (`season_id`)")
                database.execSQL("CREATE  INDEX `index_episodes_series_id` "
                        + "ON `episodes` (`series_id`)")
            }
        }

        /**
         * Migrates from pre-Room to Room.
         */
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
        // so only provide upgrade support from version 34 (used in SG 21 from January 2015)

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

        private val MIGRATION_37_43 = object : Migration(
            SeriesGuideDatabase.DBVER_37_LANGUAGE_PER_SERIES, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 37 to 43")
                SeriesGuideDatabase.upgradeToThirtyEight(database)
                MIGRATION_38_43.migrate(database)
            }
        }

        private val MIGRATION_36_43 = object : Migration(
            SeriesGuideDatabase.DBVER_36_ORDERABLE_LISTS, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 36 to 43")
                SeriesGuideDatabase.upgradeToThirtySeven(database)
                MIGRATION_37_43.migrate(database)
            }
        }

        private val MIGRATION_35_43 = object : Migration(
            SeriesGuideDatabase.DBVER_35_ACTIVITY_TABLE, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 35 to 43")
                SeriesGuideDatabase.upgradeToThirtySix(database)
                MIGRATION_36_43.migrate(database)
            }
        }

        private val MIGRATION_34_43 = object : Migration(
            SeriesGuideDatabase.DBVER_34_TRAKT_V2, VERSION_43_ROOM) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from 34 to 43")
                SeriesGuideDatabase.upgradeToThirtyFive(database)
                MIGRATION_35_43.migrate(database)
            }
        }
    }
}
