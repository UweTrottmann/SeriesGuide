// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.firebase.ui.auth.configuration.validators

import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.data.CountryData
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

internal class PhoneNumberValidator(
    override val stringProvider: AuthUIStringProvider,
    val selectedCountry: CountryData,
) :
    FieldValidator {
    private var _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()

    override val hasError: Boolean
        get() = _validationStatus.hasError

    override val errorMessage: String
        get() = _validationStatus.errorMessage ?: ""

    override fun validate(value: String): Boolean {
        if (value.isEmpty()) {
            _validationStatus = FieldValidationStatus(
                hasError = true,
                errorMessage = stringProvider.missingPhoneNumber
            )
            return false
        }

        try {
            val phoneNumber = phoneNumberUtil.parse(value, selectedCountry.countryCode)
            val isValid = phoneNumberUtil.isValidNumber(phoneNumber)

            if (!isValid) {
                _validationStatus = FieldValidationStatus(
                    hasError = true,
                    errorMessage = stringProvider.invalidPhoneNumber
                )
                return false
            }
        } catch (_: NumberParseException) {
            _validationStatus = FieldValidationStatus(
                hasError = true,
                errorMessage = stringProvider.invalidPhoneNumber
            )
            return false
        }

        _validationStatus = FieldValidationStatus(hasError = false, errorMessage = null)
        return true
    }
}
