package com.battlelancer.seriesguide.ui.search

import com.battlelancer.seriesguide.model.SgWatchProvider
import com.battlelancer.seriesguide.model.SgWatchProvider.Type
import com.battlelancer.seriesguide.streaming.DiscoverFilterViewModel
import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.tmdb2.entities.WatchProviders
import org.junit.Test

class DiscoverFilterTest {

    @Test
    fun calculateProviderUpdates() {
        val newProviders = listOf(
            tmdbWatchProvider(1, "Insert", 1, "path"),
            tmdbWatchProvider(2, "Updated", 2, "Updated"),
            tmdbWatchProvider(3, "Update No Change", 1, "Path")
        )
        val oldProviders = listOf(
            SgWatchProvider(10, 2, "To Update", 1, "Path", Type.SHOWS.id, true),
            SgWatchProvider(11, 3, "Update No Change", 1, "Path", Type.SHOWS.id, true),
            SgWatchProvider(12, 4, "To Delete", 1, "Path", Type.SHOWS.id, true)
        )

        val diff = DiscoverFilterViewModel.calculateProviderDiff(newProviders, oldProviders, Type.SHOWS)
        assertThat(diff.inserts).containsExactly(
            SgWatchProvider(0, 1, "Insert", 1, "path", Type.SHOWS.id, false)
        )
        assertThat(diff.updates).containsExactly(
            SgWatchProvider(10, 2, "Updated", 2, "Updated", Type.SHOWS.id, true)
        )
        assertThat(diff.deletes).containsExactly(oldProviders[2])
    }

    private fun tmdbWatchProvider(
        id: Int,
        name: String,
        priority: Int,
        logo: String
    ): WatchProviders.WatchProvider {
        return WatchProviders.WatchProvider().apply {
            provider_id = id
            provider_name = name
            display_priority = priority
            logo_path = logo
        }
    }

}