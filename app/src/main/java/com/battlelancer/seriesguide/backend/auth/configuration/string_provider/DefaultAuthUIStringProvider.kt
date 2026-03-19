// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.string_provider

import android.content.Context
import android.content.res.Configuration
import com.battlelancer.seriesguide.R
import java.util.Locale

class DefaultAuthUIStringProvider(
    context: Context,
    locale: Locale? = null,
) : AuthUIStringProvider {
    /**
     * Allows overriding locale.
     */
    private val localizedContext = locale?.let { locale ->
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
        )
    } ?: context

    /**
     * Common Strings
     */
    override val initializing: String
        get() = "Initializing"

    /**
     * Auth Provider strings
     */
    override val emailProvider: String
        get() = localizedContext.getString(R.string.fui_idp_name_email)

    /**
     * Auth Provider Button Strings
     */
    override val signInWithGoogle: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_google)
    override val signInWithEmail: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_email)

    /**
     * Auth Provider "Continue With" Button Strings
     */
    override val continueWithGoogle: String
        get() = localizedContext.getString(R.string.fui_continue_with_google)
    override val continueWithEmail: String
        get() = localizedContext.getString(R.string.fui_continue_with_email)

    /**
     * Email Validator Strings
     */
    override val missingEmailAddress: String
        get() = localizedContext.getString(R.string.fui_missing_email_address)
    override val invalidEmailAddress: String
        get() = localizedContext.getString(R.string.fui_invalid_email_address)

    /**
     * Password Validator Strings
     */
    override val invalidPassword: String
        get() = localizedContext.getString(R.string.fui_error_invalid_password)
    override val passwordsDoNotMatch: String
        get() = localizedContext.getString(R.string.fui_passwords_do_not_match)

    override fun passwordTooShort(minimumLength: Int): String =
        localizedContext.getString(R.string.fui_error_password_too_short, minimumLength)

    override val passwordMissingUppercase: String
        get() = localizedContext.getString(R.string.fui_error_password_missing_uppercase)
    override val passwordMissingLowercase: String
        get() = localizedContext.getString(R.string.fui_error_password_missing_lowercase)
    override val passwordMissingDigit: String
        get() = localizedContext.getString(R.string.fui_error_password_missing_digit)
    override val passwordMissingSpecialCharacter: String
        get() = localizedContext.getString(R.string.fui_error_password_missing_special_character)

    /**
     * Email Authentication Strings
     */
    override val signupPageTitle: String
        get() = localizedContext.getString(R.string.fui_title_register_email)
    override val emailHint: String
        get() = localizedContext.getString(R.string.fui_email_hint)
    override val passwordHint: String
        get() = localizedContext.getString(R.string.fui_password_hint)
    override val confirmPasswordHint: String
        get() = localizedContext.getString(R.string.fui_confirm_password_hint)
    override val newPasswordHint: String
        get() = localizedContext.getString(R.string.fui_new_password_hint)
    override val nameHint: String
        get() = localizedContext.getString(R.string.fui_name_hint)
    override val buttonTextSave: String
        get() = localizedContext.getString(R.string.fui_button_text_save)
    override val welcomeBackEmailHeader: String
        get() = localizedContext.getString(R.string.fui_welcome_back_email_header)
    override val troubleSigningIn: String
        get() = localizedContext.getString(R.string.fui_trouble_signing_in)

    override val recoverPasswordPageTitle: String
        get() = localizedContext.getString(R.string.fui_title_recover_password_activity)

    override val sendButtonText: String
        get() = localizedContext.getString(R.string.fui_button_text_send)

    override val recoverPasswordLinkSentDialogTitle: String
        get() = localizedContext.getString(R.string.fui_title_confirm_recover_password)

    override fun recoverPasswordLinkSentDialogBody(email: String): String =
        localizedContext.getString(R.string.fui_confirm_recovery_body, email)

    override val emailSignInLinkSentDialogTitle: String
        get() = localizedContext.getString(R.string.fui_email_link_header)

    override fun emailSignInLinkSentDialogBody(email: String): String =
        localizedContext.getString(R.string.fui_email_link_email_sent, email)

    override val methodPickerTitle: String
        get() = localizedContext.getString(R.string.hexagon)

    override val methodPickerDescription: String
        get() = localizedContext.getString(R.string.hexagon_signin_choose)

    override val signInWithEmailLink: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_email_link)

    override val signInWithPassword: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_password)

    override val emailLinkPromptForEmailTitle: String
        get() = localizedContext.getString(R.string.fui_email_link_confirm_email_header)

    override val emailLinkPromptForEmailMessage: String
        get() = localizedContext.getString(R.string.fui_email_link_confirm_email_message)

    override val emailLinkWrongDeviceTitle: String
        get() = localizedContext.getString(R.string.fui_email_link_wrong_device_header)

    override val emailLinkWrongDeviceMessage: String
        get() = localizedContext.getString(R.string.fui_email_link_wrong_device_message)

    override val emailLinkDifferentAnonymousUserTitle: String
        get() = localizedContext.getString(R.string.fui_email_link_different_anonymous_user_header)

    override val emailLinkDifferentAnonymousUserMessage: String
        get() = localizedContext.getString(R.string.fui_email_link_different_anonymous_user_message)

    override fun emailLinkCrossDeviceLinkingMessage(providerName: String): String =
        localizedContext.getString(
            R.string.fui_email_link_cross_device_linking_text,
            providerName
        )

    override val emailLinkInvalidLinkTitle: String
        get() = localizedContext.getString(R.string.fui_email_link_invalid_link_header)

    override val emailLinkInvalidLinkMessage: String
        get() = localizedContext.getString(R.string.fui_email_link_invalid_link_message)

    override val emailMismatchMessage: String
        get() = localizedContext.getString(R.string.fui_error_unknown)

    override val missingVerificationCode: String
        get() = localizedContext.getString(R.string.fui_required_field)

    override val invalidVerificationCode: String
        get() = localizedContext.getString(R.string.fui_mfa_incorrect_code)

    /**
     * Multi-Factor Authentication Strings
     */
    override val enterTOTPCode: String
        get() = "Enter TOTP Code"

    /**
     * Provider Picker Strings
     */
    override val signInDefault: String
        get() = localizedContext.getString(R.string.fui_sign_in_default)
    override val continueText: String
        get() = localizedContext.getString(R.string.fui_continue)
    override val nextDefault: String
        get() = localizedContext.getString(R.string.fui_next_default)

    /**
     * General Error Messages
     */
    override val errorUnknown: String
        get() = localizedContext.getString(R.string.fui_error_unknown)
    override val requiredField: String
        get() = localizedContext.getString(R.string.fui_required_field)
    override val progressDialogLoading: String
        get() = localizedContext.getString(R.string.fui_progress_dialog_loading)

    override fun signedInAs(userIdentifier: String): String =
        localizedContext.getString(R.string.fui_signed_in_as, userIdentifier)

    override val manageMfaAction: String
        get() = localizedContext.getString(R.string.fui_manage_mfa_action)

    override val signOutAction: String
        get() = localizedContext.getString(R.string.fui_sign_out_action)

    // Temporarily don't require email verification, see notes in FirebaseAuthUI
//    override fun verifyEmailInstruction(email: String): String =
//        localizedContext.getString(R.string.fui_verify_email_instruction, email)
//
//    override val sendVerificationEmailAction: String
//        get() = localizedContext.getString(R.string.fui_send_verification_email_action)
//
//    override val verifiedEmailAction: String
//        get() = localizedContext.getString(R.string.fui_verified_email_action)

    override val profileCompletionMessage: String
        get() = localizedContext.getString(R.string.fui_profile_completion_message)

    override fun profileMissingFieldsMessage(fields: String): String =
        localizedContext.getString(R.string.fui_profile_missing_fields_message, fields)

    override val skipAction: String
        get() = localizedContext.getString(R.string.fui_skip_action)

    override val removeAction: String
        get() = localizedContext.getString(R.string.fui_remove_action)

    override val backAction: String
        get() = localizedContext.getString(R.string.fui_back_action)

    override val verifyAction: String
        get() = localizedContext.getString(R.string.fui_verify_action)

    override val recoveryCodesSavedAction: String
        get() = localizedContext.getString(R.string.fui_recovery_codes_saved_action)

    override val secretKeyLabel: String
        get() = localizedContext.getString(R.string.fui_secret_key_label)

    override val verificationCodeLabel: String
        get() = localizedContext.getString(R.string.fui_verification_code_label)

    override val identityVerifiedMessage: String
        get() = localizedContext.getString(R.string.fui_identity_verified_message)

    override val mfaManageFactorsTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_manage_factors_title)

    override val mfaManageFactorsDescription: String
        get() = localizedContext.getString(R.string.fui_mfa_manage_factors_description)

    override val mfaActiveMethodsTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_active_methods_title)

    override val mfaAddNewMethodTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_add_new_method_title)

    override val mfaAllMethodsEnrolledMessage: String
        get() = localizedContext.getString(R.string.fui_mfa_all_methods_enrolled_message)

    override val totpAuthenticationLabel: String
        get() = localizedContext.getString(R.string.fui_mfa_label_totp_authentication)

    override val unknownMethodLabel: String
        get() = localizedContext.getString(R.string.fui_mfa_label_unknown_method)

    override fun enrolledOnDateLabel(date: String): String =
        localizedContext.getString(R.string.fui_mfa_enrolled_on, date)

    override val setupAuthenticatorDescription: String
        get() = localizedContext.getString(R.string.fui_mfa_setup_authenticator_description)
    override val noInternet: String
        get() = localizedContext.getString(R.string.fui_no_internet)

    /**
     * Error Recovery Dialog Strings
     */
    override val errorDialogTitle: String
        get() = localizedContext.getString(R.string.fui_error_dialog_title)
    override val retryAction: String
        get() = localizedContext.getString(R.string.fui_error_retry_action)
    override val dismissAction: String
        get() = localizedContext.getString(R.string.fui_email_link_dismiss_button)
    override val networkErrorRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_no_internet)
    override val invalidCredentialsRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_invalid_password)
    override val userNotFoundRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_email_does_not_exist)
    override val weakPasswordRecoveryMessage: String
        get() = localizedContext.resources.getQuantityString(
            R.plurals.fui_error_weak_password,
            6,
            6
        )
    override val emailAlreadyInUseRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_email_alreay_in_use)
    override val mfaRequiredRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_mfa_required_message)
    override val accountLinkingRequiredRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_account_linking_required_message)
    override val authCancelledRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_auth_cancelled_message)
    override val unknownErrorRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_unknown)

    /**
     * MFA Enrollment Step Titles
     */
    override val mfaStepSelectFactorTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_select_factor_title)
    override val mfaStepConfigureTotpTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_configure_totp_title)
    override val mfaStepVerifyFactorTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_verify_factor_title)
    override val mfaStepShowRecoveryCodesTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_show_recovery_codes_title)

    /**
     * MFA Enrollment Helper Text
     */
    override val mfaStepSelectFactorHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_select_factor_helper)
    override val mfaStepConfigureTotpHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_configure_totp_helper)
    override val mfaStepVerifyFactorTotpHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_verify_factor_totp_helper)
    override val mfaStepVerifyFactorGenericHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_verify_factor_generic_helper)
    override val mfaStepShowRecoveryCodesHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_show_recovery_codes_helper)

    // MFA Error Messages
    override val mfaErrorRecentLoginRequired: String
        get() = localizedContext.getString(R.string.fui_mfa_error_recent_login_required)
    override val mfaErrorInvalidVerificationCode: String
        get() = localizedContext.getString(R.string.fui_mfa_error_invalid_verification_code)
    override val mfaErrorNetwork: String
        get() = localizedContext.getString(R.string.fui_mfa_error_network)
    override val mfaErrorGeneric: String
        get() = localizedContext.getString(R.string.fui_mfa_error_generic)

    override val reauthDialogTitle: String
        get() = localizedContext.getString(R.string.fui_reauth_dialog_title)

    override val reauthDialogMessage: String
        get() = localizedContext.getString(R.string.fui_reauth_dialog_message)

    override fun reauthAccountLabel(email: String): String =
        localizedContext.getString(R.string.fui_reauth_account_label, email)

    override val incorrectPasswordError: String
        get() = localizedContext.getString(R.string.fui_incorrect_password_error)

    override val reauthGenericError: String
        get() = localizedContext.getString(R.string.fui_reauth_generic_error)

    override val privacyPolicy: String
        get() = localizedContext.getString(R.string.privacy_policy)

    override fun privacyPolicyMessage(privacyPolicyLabel: String): String =
        localizedContext.getString(R.string.auth_privacy_policy_message, privacyPolicyLabel)

    override val newAccountsDisabledTooltip: String
        get() = localizedContext.getString(R.string.fui_new_accounts_disabled_tooltip)

    override val mfaDisabledTooltip: String
        get() = localizedContext.getString(R.string.fui_mfa_disabled_tooltip)
}
