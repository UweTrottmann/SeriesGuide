// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.modules

import com.battlelancer.seriesguide.sync.TmdbSyncTest
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        HttpClientModule::class,
        TmdbModule::class,
        TraktModule::class
    ]
)
interface TestServicesComponent : ServicesComponent {
    fun inject(tmdbSyncTest: TmdbSyncTest)
}
