// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.validators

import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.AuthUIStringProvider

internal class VerificationCodeValidator(override val stringProvider: AuthUIStringProvider) :
    FieldValidator {
    private var _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)

    override val hasError: Boolean
        get() = _validationStatus.hasError

    override val errorMessage: String
        get() = _validationStatus.errorMessage ?: ""

    override fun validate(value: String): Boolean {
        val isInvalid = if (value.isEmpty()) {
            true
        } else {
            val digitsOnly = value.replace(Regex("[^0-9]"), "")
            digitsOnly.length != 6
        }

        return if (isInvalid) {
            _validationStatus = FieldValidationStatus(
                hasError = true,
                errorMessage = stringProvider.requiredVerificationCode
            )
            false
        } else {
            _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)
            true
        }
    }
}
