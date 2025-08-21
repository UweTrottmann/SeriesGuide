// SPDX-License-Identifier: Apache-2.0
// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019-2025 Uwe Trottmann

package com.uwetrottmann.seriesguide.billing.localdb


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CachedPurchase::class,
        PlayUnlockState::class
    ],
    version = LocalBillingDb.VERSION_3
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

        @JvmStatic
        fun getInstance(context: Context): LocalBillingDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }

        private fun buildDatabase(appContext: Context): LocalBillingDb {
            return Room.databaseBuilder(appContext, LocalBillingDb::class.java, DATABASE_NAME)
                .allowMainThreadQueries() // Gold status detection currently runs on main thread.
                .fallbackToDestructiveMigration(dropAllTables = true) // Data is cache, so it is OK to delete
                .build()
        }
    }
}
