// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.util

import androidx.annotation.RestrictTo
import com.battlelancer.seriesguide.backend.auth.util.CountryUtils

/**
 * Phone number validation utilities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PhoneNumberUtils {

    /**
     * Validates if a string starts with a valid dial code.
     * Accepts dial codes like "+1" or phone numbers like "+14155552671".
     * Does NOT validate the full phone number, only checks if it starts with a valid country code.
     *
     * @param number The dial code or phone number to validate (should start with "+")
     * @return true if the string starts with a valid country dial code, false otherwise
     */
    fun isValid(number: String): Boolean {
        if (!number.startsWith("+")) return false

        // Try to extract country code from the beginning (1-3 digits)
        val digitsOnly = number.drop(1).takeWhile { it.isDigit() }
        if (digitsOnly.isEmpty()) return false

        // Check if any prefix (1-3 digits) is a valid dial code
        for (length in 1..minOf(3, digitsOnly.length)) {
            val dialCode = "+${digitsOnly.take(length)}"
            if (CountryUtils.findByDialCode(dialCode).isNotEmpty()) {
                return true
            }
        }
        return false
    }

    /**
     * Validates if a country ISO code or dial code is valid.
     * Accepts both ISO codes (e.g., "US", "us") and dial codes (e.g., "+1").
     *
     * @param code The ISO 3166-1 alpha-2 country code or E.164 dial code
     * @return true if the code is a valid ISO code or dial code, false otherwise
     */
    fun isValidIso(code: String?): Boolean {
        if (code == null) return false

        // Check if it's a valid ISO country code (e.g., "US", "GB")
        if (CountryUtils.findByCountryCode(code) != null) {
            return true
        }

        // Check if it's a valid dial code (e.g., "+1", "+44")
        if (code.startsWith("+")) {
            return CountryUtils.findByDialCode(code).isNotEmpty()
        }

        return false
    }
}