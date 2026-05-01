// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.firebase.ui.auth.configuration.validators

import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider

internal class EmailValidator(override val stringProvider: AuthUIStringProvider) : FieldValidator {
    private var _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)

    override val hasError: Boolean
        get() = _validationStatus.hasError

    override val errorMessage: String
        get() = _validationStatus.errorMessage ?: ""

    override fun validate(value: String): Boolean {
        if (value.isEmpty()) {
            _validationStatus = FieldValidationStatus(
                hasError = true,
                errorMessage = stringProvider.missingEmailAddress
            )
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            _validationStatus = FieldValidationStatus(
                hasError = true,
                errorMessage = stringProvider.invalidEmailAddress
            )
            return false
        }

        _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)
        return true
    }
}
