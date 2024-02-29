// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2023 Uwe Trottmann

package com.battlelancer.seriesguide.preferences

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.theme.SeriesGuideTheme
import com.battlelancer.seriesguide.util.PackageTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ThemeUtils.plus
import com.battlelancer.seriesguide.util.WebTools

/**
 * Displays details about the app version, links to credits and terms.
 */
class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.configureEdgeToEdge(window)

        setContent {
            SeriesGuideTheme(useDynamicColor = DisplaySettings.isDynamicColorsEnabled(this)) {
                About(
                    versionString = PackageTools.getVersionString(this),
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onOpenWebsite = { viewUrl(R.string.url_website) },
                    onOpenPrivacyPolicy = { viewUrl(R.string.url_privacy) },
                    onOpenCredits = { viewUrl(R.string.url_credits) },
                    onOpenTmdbTerms = { viewUrl(R.string.url_terms_tmdb) },
                    onOpenTmdbApiTerms = { viewUrl(R.string.url_terms_tmdb_api) },
                    onOpenTraktTerms = { viewUrl(R.string.url_terms_trakt) }
                )
            }
        }
    }

    @Composable
    fun SgTopAppBar(
        titleStringRes: Int,
        onBackPressed: () -> Unit,
        scrollBehavior: TopAppBarScrollBehavior
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = titleStringRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.navigate_up)
                    )
                }
            },
            // Colored text and icons
            colors = TopAppBarDefaults.topAppBarColors(
                actionIconContentColor = MaterialTheme.colorScheme.primary,
                navigationIconContentColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.primary
            ),
            scrollBehavior = scrollBehavior
        )
    }

    @Composable
    fun About(
        versionString: String,
        onBackPressed: () -> Unit,
        onOpenWebsite: () -> Unit,
        onOpenPrivacyPolicy: () -> Unit,
        onOpenCredits: () -> Unit,
        onOpenTmdbTerms: () -> Unit,
        onOpenTmdbApiTerms: () -> Unit,
        onOpenTraktTerms: () -> Unit
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SgTopAppBar(R.string.prefs_category_about, onBackPressed, scrollBehavior)
            }
        ) { scaffoldPadding ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                // (Ab)using LazyColumn to easily add content padding that is not clipped
                // for applying insets.
                LazyColumn(
                    // if wider than 600 dp center align
                    modifier = if (maxWidth > 600.dp) {
                        Modifier
                            .width(600.dp)
                            .align(Alignment.Center)
                    } else {
                        Modifier
                    },
                    contentPadding = scaffoldPadding + PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = versionString,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        FilledTonalButton(onClick = onOpenWebsite) {
                            Text(text = stringResource(id = R.string.website))
                        }
                        FilledTonalButton(onClick = onOpenPrivacyPolicy) {
                            Text(text = stringResource(id = R.string.privacy_policy))
                        }
                    }
                    item {
                        Text(
                            text = stringResource(id = R.string.about_open_source),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        FilledTonalButton(onClick = onOpenCredits) {
                            Text(text = stringResource(id = R.string.licences_and_credits))
                        }
                        Text(
                            text = stringResource(id = R.string.licence_themoviedb),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        FilledTonalButton(onClick = onOpenTmdbTerms) {
                            Text(text = stringResource(id = R.string.tmdb_terms))
                        }
                        FilledTonalButton(onClick = onOpenTmdbApiTerms) {
                            Text(text = stringResource(id = R.string.tmdb_api_terms))
                        }
                    }
                    item {
                        Text(
                            text = stringResource(id = R.string.licence_trakt),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        FilledTonalButton(onClick = onOpenTraktTerms) {
                            Text(text = stringResource(id = R.string.trakt_terms))
                        }
                    }
                }
            }
        }
    }

    @Preview()
    @Preview(device = Devices.PIXEL_C)
    @Composable
    fun AboutPreview() {
        SeriesGuideTheme {
            About(
                "v42 (Database v42)",
                {},
                {},
                {},
                {},
                {},
                {},
                {}
            )
        }
    }

    private fun viewUrl(@StringRes urlResId: Int) {
        WebTools.openInApp(this, getString(urlResId))
    }

}