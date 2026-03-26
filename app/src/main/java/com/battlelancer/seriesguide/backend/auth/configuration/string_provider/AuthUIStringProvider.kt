// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.string_provider

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for providing [AuthUIStringProvider] throughout the Compose tree.
 *
 * This allows accessing localized strings without manually passing the provider through
 * every composable. The provider is set at the top level in FirebaseAuthScreen and can
 * be accessed anywhere in the auth UI using `LocalAuthUIStringProvider.current`.
 *
 * **Usage:**
 * ```kotlin
 * @Composable
 * fun MyAuthComponent() {
 *     val stringProvider = LocalAuthUIStringProvider.current
 *     Text(stringProvider.signInWithGoogle)
 * }
 * ```
 *
 * @since 10.0.0
 */
val LocalAuthUIStringProvider = staticCompositionLocalOf<AuthUIStringProvider> {
    error("No AuthUIStringProvider provided. Ensure FirebaseAuthScreen is used as the root composable.")
}

/**
 * An interface for providing localized string resources. This interface defines methods for all
 * user-facing strings allowing for localization of the UI.
 */
interface AuthUIStringProvider {
    /** Text for Email Provider */
    val emailProvider: String

    /** Button text for Google sign-in option */
    val signInWithGoogle: String

    /** Button text for Email sign-in option */
    val signInWithEmail: String

    /** Button text for Google continue option */
    val continueWithGoogle: String

    /** Button text for Email continue option */
    val continueWithEmail: String

    /** Error message when email address is invalid or empty */
    val requiredEmailAddress: String

    /** Generic error message for incorrect password during sign-in */
    val invalidPassword: String

    /** Error message when password confirmation doesn't match the original password */
    val passwordsDoNotMatch: String

    /** Error message when password doesn't meet minimum length requirement. Should support string formatting with minimum length parameter. */
    fun passwordTooShort(minimumLength: Int): String

    /** Error message when password is missing at least one uppercase letter (A-Z) */
    val passwordMissingUppercase: String

    /** Error message when password is missing at least one lowercase letter (a-z) */
    val passwordMissingLowercase: String

    /** Error message when password is missing at least one numeric digit (0-9) */
    val passwordMissingDigit: String

    /** Error message when password is missing at least one special character */
    val passwordMissingSpecialCharacter: String

    // Email Authentication Strings
    /** Title for email signup form */
    val signupPageTitle: String

    /** Hint for email input field */
    val emailHint: String

    /** Hint for password input field */
    val passwordHint: String

    /** Hint for confirm password input field */
    val confirmPasswordHint: String

    /** Hint for name input field */
    val nameHint: String

    /** Trouble signing in link text */
    val troubleSigningIn: String

    /** Title for recover password page */
    val recoverPasswordPageTitle: String

    /** Button text for reset password */
    val sendButtonText: String

    /** Title for recover password link sent dialog */
    val recoverPasswordLinkSentDialogTitle: String

    /** Body for recover password link sent dialog */
    fun recoverPasswordLinkSentDialogBody(email: String): String

    /** Title for email sign in link sent dialog */
    val emailSignInLinkSentDialogTitle: String

    /** Body for email sign in link sent dialog */
    fun emailSignInLinkSentDialogBody(email: String): String

    /** Title text in auth method picker screen */
    val methodPickerTitle: String

    /** Description text in auth method picker screen */
    val methodPickerDescription: String

    /** Button text to sign in with email link */
    val signInWithEmailLink: String

    /** Button text to sign in with password */
    val signInWithPassword: String

    /** Message shown when prompting the user to confirm their email for cross-device flows */
    val emailLinkPromptForEmailMessage: String

    /** Message shown when email link must be opened on same device */
    val emailLinkWrongDeviceMessage: String

    /** Message shown for cross-device linking flows with the provider name */
    fun emailLinkCrossDeviceLinkingMessage(providerName: String): String

    /** Message shown when email link is invalid */
    val emailLinkInvalidLinkMessage: String

    /** Message shown when email mismatch occurs */
    val emailMismatchMessage: String

    /** Missing verification code error */
    val missingVerificationCode: String

    /** Invalid verification code error */
    val invalidVerificationCode: String

    // Provider Picker Strings
    /** Common button text for sign in */
    val signInDefault: String

    /** Common button text for continue */
    val continueText: String

    // General Error Messages
    /** Required field error */
    val requiredField: String

    /** Loading progress text */
    val progressDialogLoading: String

    /** Label shown when the user is signed in. String should contain a single %s placeholder. */
    fun signedInAs(userIdentifier: String): String

    /** Action text for managing multi-factor authentication settings. */
    val manageMfaAction: String

    /** Action text for signing out. */
    val signOutAction: String

    /** Instruction shown when the user must verify their email. Accepts the email value. */
    fun verifyEmailInstruction(email: String): String

    /** Action text for sending the verification email. */
    val sendVerificationEmailAction: String

    /** Action text once the user has verified their email. */
    val verifiedEmailAction: String

    /** Action text for skipping an optional step. */
    val skipAction: String

    /** Action text for removing an item (for example, an MFA factor). */
    val removeAction: String

    /** Action text for navigating back. */
    val backAction: String

    /** Action text for confirming verification. */
    val verifyAction: String

    /** Action text for confirming recovery codes have been saved. */
    val recoveryCodesSavedAction: String

    /** Label for secret key text displayed during TOTP setup. */
    val secretKeyLabel: String

    /** Label for verification code input fields. */
    val verificationCodeLabel: String

    /** Generic identity verified confirmation message. */
    val identityVerifiedMessage: String

    /** Title for the manage MFA screen. */
    val mfaManageFactorsTitle: String

    /** Helper description for the manage MFA screen. */
    val mfaManageFactorsDescription: String

    /** Header for the list of currently enrolled MFA factors. */
    val mfaActiveMethodsTitle: String

    /** Header for the list of available MFA factors to enroll. */
    val mfaAddNewMethodTitle: String

    /** Message shown when all factors are already enrolled. */
    val mfaAllMethodsEnrolledMessage: String

    /** Label for authenticator-app MFA factor. */
    val totpAuthenticationLabel: String

    /** Label used when the factor type is unknown. */
    val unknownMethodLabel: String

    /** Label describing the enrollment date. Accepts a formatted date string. */
    fun enrolledOnDateLabel(date: String): String

    /** Description displayed during authenticator app setup. */
    val setupAuthenticatorDescription: String

    // Error Recovery Dialog Strings
    /** Error dialog title */
    val errorDialogTitle: String

    /** Retry action button text */
    val retryAction: String

    /** Dismiss action button text */
    val dismissAction: String

    /** Network error recovery message */
    val networkErrorRecoveryMessage: String

    /** Invalid credentials recovery message */
    val invalidCredentialsRecoveryMessage: String

    /** User not found recovery message */
    val userNotFoundRecoveryMessage: String

    /** Weak password recovery message */
    val weakPasswordRecoveryMessage: String

    /** Email already in use recovery message */
    val emailAlreadyInUseRecoveryMessage: String

    /** MFA required recovery message */
    val mfaRequiredRecoveryMessage: String

    /** Account linking required recovery message */
    val accountLinkingRequiredRecoveryMessage: String

    /** Auth cancelled recovery message */
    val authCancelledRecoveryMessage: String

    /** Unknown error recovery message */
    val unknownErrorRecoveryMessage: String

    // MFA Enrollment Step Titles
    /** Title for MFA factor selection step */
    val mfaStepSelectFactorTitle: String

    /** Title for TOTP MFA configuration step */
    val mfaStepConfigureTotpTitle: String

    /** Title for MFA verification step */
    val mfaStepVerifyFactorTitle: String

    /** Title for recovery codes step */
    val mfaStepShowRecoveryCodesTitle: String

    // MFA Enrollment Helper Text
    /** Helper text for selecting MFA factor */
    val mfaStepSelectFactorHelper: String

    /** Helper text for TOTP configuration */
    val mfaStepConfigureTotpHelper: String

    /** Helper text for TOTP verification */
    val mfaStepVerifyFactorTotpHelper: String

    /** Generic helper text for factor verification */
    val mfaStepVerifyFactorGenericHelper: String

    /** Helper text for recovery codes */
    val mfaStepShowRecoveryCodesHelper: String

    // MFA Error Messages
    /** Error message when MFA enrollment requires recent authentication */
    val mfaErrorRecentLoginRequired: String

    /** Error message when MFA enrollment fails due to invalid verification code */
    val mfaErrorInvalidVerificationCode: String

    /** Generic error message for MFA enrollment failures */
    val mfaErrorGeneric: String

    // Re-authentication Dialog
    /** Title displayed in the re-authentication dialog. */
    val reauthDialogTitle: String

    /** Descriptive message shown in the re-authentication dialog. */
    val reauthDialogMessage: String

    /** Label showing the account email being re-authenticated. */
    fun reauthAccountLabel(email: String): String

    /** Error message shown when the provided password is incorrect. */
    val incorrectPasswordError: String

    /** General error message for re-authentication failures. */
    val reauthGenericError: String

    /** Privacy Policy link text */
    val privacyPolicy: String

    /** Privacy Policy message */
    fun privacyPolicyMessage(privacyPolicyLabel: String): String

    /** Tooltip message shown when new account sign-up is disabled */
    val newAccountsDisabledTooltip: String

}
