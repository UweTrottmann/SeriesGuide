// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.validators

import com.battlelancer.seriesguide.backend.auth.configuration.PasswordRule
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.AuthUIStringProvider

internal class PasswordValidator(
    override val stringProvider: AuthUIStringProvider,
    private val rules: List<PasswordRule>
) : FieldValidator {
    private var _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)

    override val hasError: Boolean
        get() = _validationStatus.hasError

    override val errorMessage: String
        get() = _validationStatus.errorMessage ?: ""

    override fun validate(value: String): Boolean {
        // If there are no rules (such as when signing in) at least verify the password is not empty
        if (value.isEmpty()) {
            _validationStatus = FieldValidationStatus(
                hasError = true,
                errorMessage = stringProvider.requiredField
            )
            return false
        }

        for (rule in rules) {
            if (!rule.isValid(value)) {
                _validationStatus = FieldValidationStatus(
                    hasError = true,
                    errorMessage = rule.getErrorMessage(stringProvider)
                )
                return false
            }
        }

        _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)
        return true
    }
}
