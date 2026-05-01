// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.validators.EmailValidator
import com.battlelancer.seriesguide.backend.auth.configuration.validators.FieldValidator

/**
 * A customizable input field with built-in validation display.
 */
@Composable
fun AuthTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isSecureTextField: Boolean = false,
    textVisible: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean? = null,
    errorMessage: String? = null,
    validator: FieldValidator? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    @DrawableRes leadingIcon: Int? = null
) {
    // Automatically set the correct keyboard type based on validator or field type
    val resolvedKeyboardOptions = remember(validator, isSecureTextField, keyboardOptions) {
        when {
            keyboardOptions != KeyboardOptions.Default -> keyboardOptions
            validator is EmailValidator -> KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            isSecureTextField -> KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )

            else -> keyboardOptions
        }
    }

    TextField(
        modifier = modifier
            .fillMaxWidth(),
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
            validator?.validate(newValue)
        },
        label = label,
        singleLine = true,
        enabled = enabled,
        isError = isError ?: validator?.hasError ?: false,
        supportingText = {
            if (validator?.hasError ?: false) {
                Text(text = errorMessage ?: validator.errorMessage)
            }
        },
        keyboardOptions = resolvedKeyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (isSecureTextField && !textVisible)
            PasswordVisualTransformation() else visualTransformation,
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null /* TextField has label */
                )
            }
        }
    )
}

@Composable
fun AuthEmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    validator: FieldValidator? = null
) {
    val stringProvider = LocalAuthUIStringProvider.current
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(stringProvider.emailHint)
        },
        enabled = enabled,
        validator = validator,
        leadingIcon = R.drawable.ic_email_control_24dp
    )
}

@Composable
fun AuthPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    textVisible: Boolean,
    validator: FieldValidator? = null
) {
    val stringProvider = LocalAuthUIStringProvider.current
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: {
            Text(stringProvider.passwordHint)
        },
        enabled = enabled,
        validator = validator,
        isSecureTextField = true,
        textVisible = textVisible,
        leadingIcon = R.drawable.ic_rounded_password_control_24dp
    )
}

@Preview(showBackground = true)
@Composable
internal fun PreviewAuthTextField() {
    val context = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(context)
    CompositionLocalProvider(
        LocalAuthUIStringProvider provides stringProvider
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthTextField(
                value = "Base variant",
                label = {
                    Text("Base variant")
                },
                onValueChange = { _ ->
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthEmailTextField(
                value = "AuthEmailTextField",
                onValueChange = { _ ->
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthPasswordTextField(
                value = "example value",
                textVisible = false,
                onValueChange = { _ ->
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthPasswordTextField(
                value = "example value",
                textVisible = true,
                onValueChange = { _ ->
                }
            )
        }
    }
}