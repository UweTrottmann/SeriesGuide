// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019, 2020, 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0
package com.uwetrottmann.seriesguide.billing.localdb


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CachedPurchase::class,
        GoldStatus::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(PurchaseTypeConverter::class)
abstract class LocalBillingDb : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
    abstract fun entitlementsDao(): EntitlementsDao

    companion object {
        @Volatile
        private var INSTANCE: LocalBillingDb? = null
        private const val DATABASE_NAME = "purchase_db"

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
                .fallbackToDestructiveMigration() // Data is cache, so it is OK to delete
                .build()
        }
    }
}
