// SPDX-License-Identifier: AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.backend.auth.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme

@Composable
fun AuthTopAppBar(
    title: String,
    onNavigateBack: (() -> Unit)?,
) {
    val stringProvider = LocalAuthUIStringProvider.current

    TopAppBar(
        title = {
            Text(title)
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(com.battlelancer.seriesguide.R.drawable.ic_arrow_back_control_24dp),
                        contentDescription = stringProvider.backAction
                    )
                }
            }
        },
        colors = AuthUITheme.topAppBarColors
    )
}

@Preview
@Composable
fun PreviewAuthTopAppBar() {
    val applicationContext = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(applicationContext)

    AuthUITheme {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides stringProvider
        ) {
            Scaffold(
                topBar = {
                    AuthTopAppBar(
                        title = "This Is A Title",
                        onNavigateBack = {}
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                ) { }
            }
        }
    }
}
