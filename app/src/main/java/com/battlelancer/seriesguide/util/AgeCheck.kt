// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util

import android.app.Activity
import android.content.Context
import com.battlelancer.seriesguide.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.AgeSignalsResult
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.google.android.play.agesignals.testing.FakeAgeSignalsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import timber.log.Timber
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Performs age checks and helps prevent using the app if not approved by a parent when installed by
 * stores and used in specific regions.
 *
 * - https://support.google.com/googleplay/android-developer/answer/16569691
 * - https://developer.amazon.com/docs/app-submission/user-age-verification.html
 */
class AgeCheck(private val context: Context) {

    /**
     * Whether the user should be prevented from using the app.
     */
    private var preventUsingApp: Boolean = false

    suspend fun run(installedBy: PackageTools.InstallingPackage) {
        withContext(Dispatchers.Default) {
            when (installedBy) {
                PackageTools.InstalledByAmazonStore -> {
                    Timber.i("Age check using Amazon App Store")
                    preventUsingApp = AmazonAgeCheck(context).run()
                }

                PackageTools.InstalledByPlayStore -> {
                    Timber.i("Age check using Google Play Store")
                    preventUsingApp = playAgeCheck()
                }

                else -> {
                    Timber.i("Age check not necessary")
                    preventUsingApp = false
                }
            }
        }
    }

    /**
     * Returns whether app access should be prevented.
     */
    private suspend fun playAgeCheck(): Boolean = suspendCoroutine { continuation ->
        try {
            val manager = if (TEST) {
                FakeAgeSignalsManager().apply {
                    // Test SUPERVISED_APPROVAL_DENIED
                    val fakeSupervisedApprovalDeniedUser = AgeSignalsResult.builder()
                        .setUserStatus(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED)
                        .setAgeLower(13)
                        .setAgeUpper(17)
                        .setMostRecentApprovalDate(
                            Date(
                                LocalDate.of(2025, 2, 1)
                                    .atStartOfDay(ZoneOffset.UTC)
                                    .toInstant()
                                    .toEpochMilli()
                            )
                        )
                        .setInstallId("fake_install_id")
                        .build()
                    setNextAgeSignalsResult(fakeSupervisedApprovalDeniedUser)
                    // Test network error
//                    setNextAgeSignalsException(
//                        AgeSignalsException(AgeSignalsErrorCode.NETWORK_ERROR)
//                    )
                }
            } else {
                AgeSignalsManagerFactory.create(context)
            }
            manager
                .checkAgeSignals(AgeSignalsRequest.builder().build())
                .addOnSuccessListener { ageSignalsResult ->
                    val preventUsingApp =
                        ageSignalsResult.userStatus() == AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED
                    continuation.resume(preventUsingApp)
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Google Play Age check failed")
                    continuation.resume(false)
                }
        } catch (e: Exception) {
            Timber.e(e, "Google Play Age check error")
            continuation.resume(false)
        }
    }

    fun showDialogIfUserDoesNotPassAgeCheck(activity: Activity) {
        if (!preventUsingApp) return

        Timber.i("User does not pass age check, prevent using app")

        // Note: currently not translating as it only affects US states
        MaterialAlertDialogBuilder(
            activity,
            R.style.ThemeOverlay_SeriesGuide_Dialog_PositiveButton_Primary
        )
            .setCancelable(false)
            .setTitle("Access denied")
            .setMessage("Your supervisor has denied access to this app.")
            .setPositiveButton("Close app") { dialog, which ->
                // While not perfect (when launching into for ex. the movies screen the app will
                // create the main activity and show the dialog again) it's a simple, working
                // solution.
                activity.finishAndRemoveTask()
            }
            .show()
    }

    companion object {
        private const val TEST = true
    }

}