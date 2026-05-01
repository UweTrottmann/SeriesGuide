// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.firebase.ui.auth.configuration.validators

import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider

/**
 * An interface for validating input fields.
 */
interface FieldValidator {
    val stringProvider: AuthUIStringProvider

    /**
     * Returns true if the last validation failed.
     */
    val hasError: Boolean

    /**
     * The error message for the current state.
     */
    val errorMessage: String

    /**
     * Runs validation on a value and returns true if valid.
     */
    fun validate(value: String): Boolean
}
