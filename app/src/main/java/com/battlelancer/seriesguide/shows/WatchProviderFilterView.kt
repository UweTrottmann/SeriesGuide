// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.ui.theme.SeriesGuideTheme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WatchProviderFilter(
    watchProvidersFlow: StateFlow<List<SgWatchProvider>>,
    onProviderFilterChange: (SgWatchProvider, Boolean) -> Unit,
    onProviderIncludeAny: () -> Unit,
    onSelectRegion: () -> Unit,
    useDynamicColor: Boolean
) {
    val watchProviders by watchProvidersFlow.collectAsState()
    SeriesGuideTheme(useDynamicColor = useDynamicColor) {
        WatchProviderList(
            watchProviders,
            onProviderFilterChange,
            onProviderIncludeAny,
            onSelectRegion
        )
    }
}

@Composable
fun WatchProviderList(
    watchProviders: List<SgWatchProvider>,
    onProviderFilterChange: (SgWatchProvider, Boolean) -> Unit,
    onProviderIncludeAny: () -> Unit,
    onSelectRegion: () -> Unit
) {
    Surface {
        Column(
            modifier = Modifier.heightIn(112.dp, 400.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items = watchProviders, key = { it._id }) {
                    WatchProviderFilterItem(it, onProviderFilterChange)
                }
            }
            HorizontalDivider()
            Row {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            role = Role.Button,
                            onClick = onProviderIncludeAny
                        )
                        .padding(16.dp)
                ) {
                    Text(stringResource(id = R.string.action_reset))
                }
                IconButton(
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                    onClick = onSelectRegion
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(id = R.string.action_stream_info)
                    )
                }
            }
        }
    }
}

@Composable
fun WatchProviderFilterItem(
    item: SgWatchProvider,
    onProviderFilterChange: (SgWatchProvider, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .toggleable(
                value = item.filter_local,
                role = Role.Checkbox,
                onValueChange = { onProviderFilterChange(item, it) }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.provider_name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        )
        Checkbox(
            checked = item.filter_local,
            onCheckedChange = null, // null recommended for accessibility with screenreaders
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
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
                    enabled = false,
                    filter_local = it.mod(2) == 0,
                    logo_path = "",
                    type = SgWatchProvider.Type.SHOWS.id
                )
            },
            onProviderFilterChange = { _: SgWatchProvider, _: Boolean -> },
            onProviderIncludeAny = {},
            onSelectRegion = {}
        )
    }
}