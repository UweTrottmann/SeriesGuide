// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.backend.auth.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider

@Composable
fun AuthShowPasswordToggle(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    val stringProvider = LocalAuthUIStringProvider.current
    Row(
        modifier = Modifier.toggleable(
            value = value,
            role = Role.Checkbox,
            onValueChange = onValueChange
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            modifier = Modifier.minimumInteractiveComponentSize(),
            checked = value,
            onCheckedChange = null
        )
        Text(
            modifier = Modifier.padding(end = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            text = stringProvider.showPassword,
        )
    }
}