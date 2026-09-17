// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration

/**
 * Configuration class for Multi-Factor Authentication (MFA) enrollment and verification behavior.
 *
 * This class controls which MFA factors are available to users, whether enrollment is mandatory,
 * and whether recovery codes are generated.
 *
 * @property allowedFactors List of MFA factors that users are permitted to enroll in.
 *                          Defaults to [MfaFactor.Totp].
 * @property requireEnrollment Whether MFA enrollment is mandatory for all users.
 *                             When true, users must enroll in at least one MFA factor.
 *                             Defaults to false.
 * @property enableRecoveryCodes Whether to generate and provide recovery codes to users
 *                               after successful MFA enrollment. These codes can be used
 *                               as a backup authentication method. Defaults to true.
 */
class MfaConfiguration(
    val allowedFactors: List<MfaFactor> = listOf(MfaFactor.Totp),
    val requireEnrollment: Boolean = false,
    val enableRecoveryCodes: Boolean = true
) {
    init {
        require(allowedFactors.isNotEmpty()) {
            "At least one MFA factor must be allowed"
        }
    }
}
