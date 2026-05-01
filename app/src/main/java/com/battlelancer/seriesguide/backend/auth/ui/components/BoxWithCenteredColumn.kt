// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.backend.auth.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BoxWithCenteredColumn(
    insetPadding: PaddingValues,
    content: @Composable (ColumnScope.() -> Unit)
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        // If wider than 400 dp center align, use padding so whole screen remains scrollable
        val maxContentWidth = 400.dp
        val defaultContentPadding = 16.dp
        val contentCenteredPadding =
            if (maxWidth > defaultContentPadding + maxContentWidth + defaultContentPadding) {
                val horizontalPadding = (maxWidth - maxContentWidth) / 2
                PaddingValues(
                    horizontal = horizontalPadding,
                    vertical = defaultContentPadding
                )
            } else {
                PaddingValues(defaultContentPadding)
            }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(insetPadding)
                .padding(contentCenteredPadding),
            content = content
        )
    }
}