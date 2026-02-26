// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration

/**
 * Represents the different Multi-Factor Authentication (MFA) factors that can be used
 * for enrollment and verification.
 */
enum class MfaFactor {
    /**
     * SMS-based authentication factor.
     * Users receive a verification code via text message to their registered phone number.
     */
    Sms,

    /**
     * Time-based One-Time Password (TOTP) authentication factor.
     * Users generate verification codes using an authenticator app.
     */
    Totp
}
