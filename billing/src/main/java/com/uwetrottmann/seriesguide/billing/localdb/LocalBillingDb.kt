// SPDX-License-Identifier: Apache-2.0
// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019-2025 Uwe Trottmann

package com.uwetrottmann.seriesguide.billing.localdb


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

@Database(
    entities = [
        CachedPurchase::class,
        PlayUnlockState::class,
        UnlockState::class
    ],
    version = LocalBillingDb.VERSION_4
)
@TypeConverters(PurchaseTypeConverter::class)
abstract class LocalBillingDb : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
    abstract fun unlockStateHelper(): UnlockStateHelper

    companion object {
        @Volatile
        private var INSTANCE: LocalBillingDb? = null
        private const val DATABASE_NAME = "purchase_db"

        /**
         * Original version.
         */
        const val VERSION_1 = 1

        /**
         * Store subscription purchase token for up/downgrades.
         */
        const val VERSION_2 = 2

        /**
         * Drop AugmentedSkuDetails table, stored in a StateFlow instead.
         */
        const val VERSION_3 = 3

        /**
         * Add table for global unlock state.
         */
        const val VERSION_4 = 4

        @JvmStatic
        fun getInstance(context: Context): LocalBillingDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }

        private fun buildDatabase(appContext: Context): LocalBillingDb {
            return Room.databaseBuilder(appContext, LocalBillingDb::class.java, DATABASE_NAME)
                .allowMainThreadQueries() // Only small objects, main thread queries are fine
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, VERSION_1, VERSION_2)
                .addMigrations(MIGRATION_3_4)
                .build()
        }

        @JvmField
        val MIGRATION_3_4: Migration = object :
            Migration(VERSION_3, VERSION_4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Timber.d("Migrating database from $VERSION_3 to $VERSION_4")

                // Create new table, copied from exported schema JSON
                db.execSQL("CREATE TABLE IF NOT EXISTS `unlock_state` (`isUnlockAll` INTEGER NOT NULL, `lastUnlockedAllMs` INTEGER NOT NULL, `notifyUnlockAllExpired` INTEGER NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}
