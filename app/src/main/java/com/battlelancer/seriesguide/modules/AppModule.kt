// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.modules

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class AppModule(context: Context) {

    private val context: Context = context.applicationContext

    @Provides
    @ApplicationContext
    fun provideApplicationContext(): Context {
        return context
    }

}
