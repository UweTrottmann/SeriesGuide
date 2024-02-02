// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.ui.theme.SeriesGuideTheme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WatchProviderFilter(
    showsDistillationUiState: StateFlow<ShowsDistillationUiState>,
    onProviderFilterChange: (SgWatchProvider, Boolean) -> Unit
) {
    val uiState by showsDistillationUiState.collectAsState()
    SeriesGuideTheme {
        WatchProviderList(
            watchProviders = uiState.watchProviders,
            onProviderFilterChange
        )
    }
}

@Composable
fun WatchProviderList(
    watchProviders: List<SgWatchProvider>,
    onProviderFilterChange: (SgWatchProvider, Boolean) -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        items(items = watchProviders, key = { it._id }) {
            WatchProviderFilterItem(it, onProviderFilterChange)
        }
    }

}

@Composable
fun WatchProviderFilterItem(
    item: SgWatchProvider,
    onProviderFilterChange: (SgWatchProvider, Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        Text(text = item.provider_name)
        Switch(checked = item.filter_local, onCheckedChange = {
            onProviderFilterChange(item, it)
        })
    }
}

@Preview()
@Composable
fun WatchProviderFilterPreview() {
    SeriesGuideTheme {
        WatchProviderList(
            watchProviders = List(20) {
                SgWatchProvider(
                    _id = it,
                    provider_id = it,
                    provider_name = "Watch Provider $it",
                    display_priority = 0,
                    enabled = it.mod(2) == 0,
                    logo_path = "",
                    type = SgWatchProvider.Type.SHOWS.id
                )
            },
            onProviderFilterChange = { _: SgWatchProvider, _: Boolean -> })
    }
}