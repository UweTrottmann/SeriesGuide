// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.string_provider

import android.content.Context
import com.battlelancer.seriesguide.R

class DefaultAuthUIStringProvider(
    private val context: Context,
) : AuthUIStringProvider {

    /**
     * Auth Provider strings
     */
    override val emailProvider: String
        get() = context.getString(R.string.auth_idp_name_email)

    /**
     * Auth Provider Button Strings
     */
    override val signInWithGoogle: String
        get() = context.getString(R.string.auth_action_sign_in_with_google)
    override val signInWithEmail: String
        get() = context.getString(R.string.auth_action_sign_in_with_email)

    /**
     * Auth Provider "Continue With" Button Strings
     */
    override val continueWithGoogle: String
        get() = context.getString(R.string.auth_action_continue_with_google)
    override val continueWithEmail: String
        get() = context.getString(R.string.auth_action_continue_with_email)

    /**
     * Email Validator Strings
     */
    override val requiredEmailAddress: String
        get() = context.getString(R.string.auth_hint_email_required)

    /**
     * Password Validator Strings
     */
    override val passwordsDoNotMatch: String
        get() = context.getString(R.string.auth_hint_confirm_password_does_not_match)
    override val weakPasswordRecoveryMessage: String
        get() = context.getString(R.string.auth_error_weak_password)
    override fun passwordTooShort(minimumLength: Int): String =
        context.getString(R.string.auth_error_password_too_short, minimumLength)
    override val passwordMissingUppercase: String
        get() = context.getString(R.string.auth_error_password_missing_uppercase)
    override val passwordMissingLowercase: String
        get() = context.getString(R.string.auth_error_password_missing_lowercase)
    override val passwordMissingDigit: String
        get() = context.getString(R.string.auth_error_password_missing_digit)
    override val passwordMissingSpecialCharacter: String
        get() = context.getString(R.string.auth_error_password_missing_special_character)

    /**
     * Email Authentication Strings
     */
    override val signupPageTitle: String
        get() = context.getString(R.string.auth_action_sign_up)
    override val emailHint: String
        get() = context.getString(R.string.auth_hint_email)
    override val passwordHint: String
        get() = context.getString(R.string.auth_hint_password)
    override val confirmPasswordHint: String
        get() = context.getString(R.string.auth_hint_confirm_password)
    override val nameHint: String
        get() = context.getString(R.string.auth_hint_name)
    override val showPassword: String
        get() = context.getString(R.string.auth_action_show_password)

    override val resetPasswordAction: String
        get() = context.getString(R.string.auth_action_reset_password)

    override val sendButtonText: String
        get() = context.getString(R.string.feedback)

    override val recoverPasswordLinkSentDialogTitle: String
        get() = context.getString(R.string.auth_title_reset_password_instructions)

    override val recoverPasswordLinkSentDialogBody: String
        get() = context.getString(R.string.auth_message_reset_password_instructions)

    override val emailSignInLinkSentDialogTitle: String
        get() = context.getString(R.string.auth_email_link_header)

    override fun emailSignInLinkSentDialogBody(email: String): String =
        context.getString(R.string.auth_email_link_email_sent, email)

    override val methodPickerTitle: String
        get() = context.getString(R.string.hexagon)

    override val methodPickerDescription: String
        get() = context.getString(R.string.hexagon_signin_choose)

    override val signInWithEmailLink: String
        get() = context.getString(R.string.auth_sign_in_with_email_link)

    override val signInWithPassword: String
        get() = context.getString(R.string.auth_sign_in_with_password)

    override val emailLinkPromptForEmailMessage: String
        get() = context.getString(R.string.auth_email_link_confirm_email_message)

    override val emailLinkWrongDeviceMessage: String
        get() = context.getString(R.string.auth_email_link_wrong_device_message)

    override fun emailLinkCrossDeviceLinkingMessage(providerName: String): String =
        context.getString(
            R.string.auth_email_link_cross_device_linking_text,
            providerName
        )

    override val emailLinkInvalidLinkMessage: String
        get() = context.getString(R.string.auth_email_link_invalid_link_message)

    override val emailMismatchMessage: String
        get() = context.getString(R.string.auth_error_unknown)

    override val requiredVerificationCode: String
        get() = context.getString(R.string.auth_mfa_required_code)

    /**
     * Provider Picker Strings
     */
    override val signInDefault: String
        get() = context.getString(R.string.hexagon_signin)
    override val continueText: String
        get() = context.getString(R.string.auth_action_continue)

    /**
     * General Error Messages
     */
    override val requiredField: String
        get() = context.getString(R.string.auth_hint_required)

    override val manageMfaAction: String
        get() = context.getString(R.string.auth_manage_mfa_action)

    override val signOutAction: String
        get() = context.getString(R.string.hexagon_signout)

    override fun verifyEmailInstruction(email: String): String =
        context.getString(R.string.auth_verify_email_instruction, email)

    override val sendVerificationEmailAction: String
        get() = context.getString(R.string.auth_send_verification_email_action)

    override val verifiedEmailAction: String
        get() = context.getString(R.string.auth_verified_email_action)

    override val skipAction: String
        get() = context.getString(R.string.auth_skip_action)

    override val removeAction: String
        get() = context.getString(R.string.auth_remove_action)

    override val backAction: String
        get() = context.getString(R.string.auth_action_back)

    override val verifyAction: String
        get() = context.getString(R.string.auth_verify_action)

    override val recoveryCodesSavedAction: String
        get() = context.getString(R.string.auth_recovery_codes_saved_action)

    override val secretKeyLabel: String
        get() = context.getString(R.string.auth_secret_key_label)

    override val verificationCodeLabel: String
        get() = context.getString(R.string.auth_verification_code_label)

    override val identityVerifiedMessage: String
        get() = context.getString(R.string.auth_identity_verified_message)

    override val mfaManageFactorsTitle: String
        get() = context.getString(R.string.auth_mfa_manage_factors_title)

    override val mfaManageFactorsDescription: String
        get() = context.getString(R.string.auth_mfa_manage_factors_description)

    override val mfaActiveMethodsTitle: String
        get() = context.getString(R.string.auth_mfa_active_methods_title)

    override val mfaAddNewMethodTitle: String
        get() = context.getString(R.string.auth_mfa_add_new_method_title)

    override val mfaAllMethodsEnrolledMessage: String
        get() = context.getString(R.string.auth_mfa_all_methods_enrolled_message)

    override val totpAuthenticationLabel: String
        get() = context.getString(R.string.auth_mfa_label_totp_authentication)

    override val unknownMethodLabel: String
        get() = context.getString(R.string.auth_mfa_label_unknown_method)

    override fun enrolledOnDateLabel(date: String): String =
        context.getString(R.string.auth_mfa_enrolled_on, date)

    override val setupAuthenticatorDescription: String
        get() = context.getString(R.string.auth_mfa_setup_authenticator_description)

    /**
     * Error Recovery Dialog Strings
     */
    override val errorDialogTitle: String
        get() = context.getString(R.string.auth_error_dialog_title)
    override val retryAction: String
        get() = context.getString(R.string.action_try_again)
    override val dismissAction: String
        get() = context.getString(R.string.dismiss)
    override val networkErrorRecoveryMessage: String
        get() = context.getString(R.string.api_error_generic, context.getString(R.string.hexagon))
    override val invalidCredentialsRecoveryMessage: String
        get() = context.getString(R.string.auth_error_invalid_password)
    override val emailAlreadyInUseRecoveryMessage: String
        get() = context.getString(R.string.auth_error_email_alreay_in_use)
    override val mfaRequiredRecoveryMessage: String
        get() = context.getString(R.string.auth_error_mfa_required_message)
    override val accountLinkingRequiredRecoveryMessage: String
        get() = context.getString(R.string.auth_error_account_linking_required)
    override val noGoogleAccountAvailableMessage: String
        get() = context.getString(R.string.auth_error_no_google_account_available)
    override val unknownErrorRecoveryMessage: String
        get() = context.getString(R.string.auth_error_unknown)

    /**
     * MFA Enrollment Step Titles
     */
    override val mfaStepSelectFactorTitle: String
        get() = context.getString(R.string.auth_mfa_step_select_factor_title)
    override val mfaStepConfigureTotpTitle: String
        get() = context.getString(R.string.auth_mfa_step_configure_totp_title)
    override val mfaStepVerifyFactorTitle: String
        get() = context.getString(R.string.auth_mfa_step_verify_factor_title)
    override val mfaStepShowRecoveryCodesTitle: String
        get() = context.getString(R.string.auth_mfa_step_show_recovery_codes_title)

    /**
     * MFA Enrollment Helper Text
     */
    override val mfaStepSelectFactorHelper: String
        get() = context.getString(R.string.auth_mfa_step_select_factor_helper)
    override val mfaStepConfigureTotpHelper: String
        get() = context.getString(R.string.auth_mfa_step_configure_totp_helper)
    override val mfaStepVerifyFactorTotpHelper: String
        get() = context.getString(R.string.auth_mfa_step_verify_factor_totp_helper)
    override val mfaStepVerifyFactorGenericHelper: String
        get() = context.getString(R.string.auth_mfa_step_verify_factor_generic_helper)
    override val mfaStepShowRecoveryCodesHelper: String
        get() = context.getString(R.string.auth_mfa_step_show_recovery_codes_helper)

    // MFA Error Messages
    override val mfaErrorRecentLoginRequired: String
        get() = context.getString(R.string.auth_mfa_error_recent_login_required)
    override val mfaErrorInvalidVerificationCode: String
        get() = context.getString(R.string.auth_mfa_error_invalid_verification_code)
    override val mfaErrorGeneric: String
        get() = context.getString(R.string.auth_mfa_error_generic)

    override val reauthDialogTitle: String
        get() = context.getString(R.string.auth_reauth_dialog_title)

    override val reauthDialogMessage: String
        get() = context.getString(R.string.auth_reauth_dialog_message)

    override fun reauthAccountLabel(email: String): String =
        context.getString(R.string.auth_reauth_account_label, email)

    override val reauthGenericError: String
        get() = context.getString(R.string.auth_reauth_generic_error)

    override val privacyPolicy: String
        get() = context.getString(R.string.privacy_policy)

    override fun privacyPolicyMessage(privacyPolicyLabel: String): String =
        context.getString(R.string.auth_message_privacy_policy, privacyPolicyLabel)

    override val newAccountsDisabled: String
        get() = context.getString(R.string.auth_message_new_accounts_disabled)

}
