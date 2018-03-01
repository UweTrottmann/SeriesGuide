package com.battlelancer.seriesguide.provider;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.model.List;
import com.battlelancer.seriesguide.model.Show;

@Database(
        entities = {
                Show.class,
                List.class
        },
        version = SgRoomDatabase.VERSION
)
public abstract class SgRoomDatabase extends RoomDatabase {

    private static final int VERSION_43_ROOM = 43;
    public static final int VERSION = VERSION_43_ROOM;

    private static SgRoomDatabase INSTANCE;

    private static final Object sLock = new Object();

    static final Migration MIGRATION_42_43 = new Migration(
            SeriesGuideDatabase.DBVER_42_JOBS, VERSION_43_ROOM) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Room uses an own database hash to uniquely identify the database
            // Since version 42 does not use Room, it doesn't have the database hash associated.
            // By implementing a Migration class, we're telling Room that it should use the data
            // from version 42 to version 43.
            // If no migration is provided, then the tables will be dropped and recreated.
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    public static SgRoomDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                        SgRoomDatabase.class, SeriesGuideDatabase.DATABASE_NAME)
                        .addMigrations(MIGRATION_42_43)
                        .build();
            }
            return INSTANCE;
        }
    }
}
