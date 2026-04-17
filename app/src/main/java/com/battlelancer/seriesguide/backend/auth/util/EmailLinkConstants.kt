// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.util

/**
 * Constants for email link authentication.
 *
 * ## Usage Example:
 *
 * Check for email link in your MainActivity:
 * ```kotlin
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     
 *     val authUI = FirebaseAuthUI.getInstance()
 *     
 *     // Check if intent contains email link (from deep link)
 *     var emailLink: String? = null
 *     
 *     if (authUI.canHandleIntent(intent)) {
 *         emailLink = intent.data?.toString()
 *     }
 *     
 *     if (emailLink != null) {
 *         // Handle email link sign-in
 *         // Pass to FirebaseAuthScreen or handle manually
 *     }
 * }
 * ```
 *
 * @since 10.0.0
 */
object EmailLinkConstants {
    
    /**
     * Intent extra key for the email link.
     *
     * Use this constant when passing email links between activities via Intent extras.
     *
     * **Example:**
     * ```kotlin
     * // Sending activity
     * val intent = Intent(this, MainActivity::class.java)
     * intent.putExtra(EmailLinkConstants.EXTRA_EMAIL_LINK, emailLink)
     * startActivity(intent)
     * 
     * // Receiving activity
     * val emailLink = intent.getStringExtra(EmailLinkConstants.EXTRA_EMAIL_LINK)
     * ```
     */
    const val EXTRA_EMAIL_LINK = "seriesguide.auth.EXTRA_EMAIL_LINK"
}
