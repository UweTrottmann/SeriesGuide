package com.battlelancer.seriesguide.provider;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.battlelancer.seriesguide.model.Episode;
import com.battlelancer.seriesguide.model.SgList;
import com.battlelancer.seriesguide.model.Movie;
import com.battlelancer.seriesguide.model.Season;
import com.battlelancer.seriesguide.model.SgListItem;
import com.battlelancer.seriesguide.model.Show;
import com.uwetrottmann.androidutils.AndroidUtils;

@Database(
        entities = {
                Show.class,
                Season.class,
                Episode.class,
                SgList.class,
                SgListItem.class,
                Movie.class
        },
        version = SgRoomDatabase.VERSION
)
public abstract class SgRoomDatabase extends RoomDatabase {

    private static final int VERSION_43_ROOM = 43;
    public static final int VERSION = VERSION_43_ROOM;

    private static volatile SgRoomDatabase instance;

    public static SgRoomDatabase getInstance(Context context) {
        SgRoomDatabase result = instance;
        if (result == null) { // First check (no locking)
            synchronized (SgRoomDatabase.class) {
                result = instance;
                if (result == null) { // Second check (with locking)
                    result = instance = Room.databaseBuilder(context.getApplicationContext(),
                            SgRoomDatabase.class, SeriesGuideDatabase.DATABASE_NAME)
                            .addMigrations(MIGRATION_42_43)
                            .addCallback(CALLBACK)
                            .build();
                }
            }
        }
        return result;
    }

    /**
     * Changes the instance to an in memory database for content provider unit testing.
     */
    @VisibleForTesting
    public static void switchToInMemory(Context context) {
        instance = Room.inMemoryDatabaseBuilder(context.getApplicationContext(),
                SgRoomDatabase.class)
                .addMigrations(MIGRATION_42_43)
                .addCallback(CALLBACK)
                .build();
    }

    public abstract ShowHelper showHelper();

    static final Callback CALLBACK = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            // manually create FTS table not supported by Room
            if (AndroidUtils.isJellyBeanOrHigher()) {
                db.execSQL(SeriesGuideDatabase.CREATE_SEARCH_TABLE);
            } else {
                db.execSQL(SeriesGuideDatabase.CREATE_SEARCH_TABLE_API_ICS);
            }
        }
    };

    static final Migration MIGRATION_42_43 = new Migration(
            SeriesGuideDatabase.DBVER_42_JOBS, VERSION_43_ROOM) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Room uses an own database hash to uniquely identify the database
            // Since version 42 does not use Room, it doesn't have the database hash associated.
            // By implementing a Migration class, we're telling Room that it should use the data
            // from version 42 to version 43.
            // If no migration is provided, then the tables will be dropped and recreated.
            // Since we didn't alter the tables, there's not much to do here.

            // Room does not support UNIQUE constraints, so use unique indexes instead (probably
            // faster anyhow)
            database.execSQL("CREATE UNIQUE INDEX `index_lists_list_id` "
                    + "ON `lists` (`list_id`)");
            database.execSQL("CREATE UNIQUE INDEX `index_listitems_list_item_id` "
                    + "ON `listitems` (`list_item_id`)");
            database.execSQL("CREATE UNIQUE INDEX `index_movies_movies_tmdbid` "
                    + "ON `movies` (`movies_tmdbid`)");
        }
    };
}
