// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/**
 * Runs an age check against an Amazon App Store content provider.
 *
 * - https://developer.amazon.com/docs/app-submission/user-age-verification.html#getuseragedata-api
 */
class AmazonAgeCheck(
    private val context: Context
) {

    /**
     * Returns whether app access should be prevented.
     */
    suspend fun run(): Boolean {
        val signal = CancellationSignal()
        try {
            return withTimeout(5000) {
                val userData = queryUserDataInternal(signal)
                if (userData != null) {
                    // TODO Is UNKNOWN even the correct state to deny access? Is it possible this
                    //  API doesn't have a state to deny access (unlike the Play API)?
                    return@withTimeout userData.responseStatus == "SUCCESS"
                            && userData.userStatus == "UNKNOWN"
                }
                return@withTimeout false
            }
        } finally {
            signal.cancel()
        }
    }

    private fun queryUserDataInternal(signal: CancellationSignal): UserData? {
        try {
            context.contentResolver
                .query(
                    CONTENT_URI,
                    null,
                    null,
                    null,
                    null,
                    signal
                )
                .use { cursor ->
                    if (cursor == null || !cursor.moveToFirst()) {
                        Timber.e("Amazon Age check returned no data")
                        return null
                    }

                    val ageLowerColumnIndex =
                        cursor.getColumnIndex(UserAgeDataResponse.COLUMN_AGE_LOWER)
                    val ageUpperColumnIndex =
                        cursor.getColumnIndex(UserAgeDataResponse.COLUMN_AGE_UPPER)

                    return UserData(
                        cursor.getString(cursor.getColumnIndexOrThrow(UserAgeDataResponse.COLUMN_RESPONSE_STATUS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(UserAgeDataResponse.COLUMN_USER_STATUS)),
                        if (!cursor.isNull(ageLowerColumnIndex)) cursor.getInt(ageLowerColumnIndex) else null,
                        if (!cursor.isNull(ageUpperColumnIndex)) cursor.getInt(ageUpperColumnIndex) else null,
                        cursor.getString(cursor.getColumnIndexOrThrow(UserAgeDataResponse.COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(UserAgeDataResponse.COLUMN_MOST_RECENT_APPROVAL_DATE))
                    )
                }
        } catch (e: Exception) {
            Timber.e(e, "Amazon Age check failed")
            return null
        }
    }

    companion object {
        private const val AUTHORITY: String = "amzn_appstore"
        private const val PATH = "/getUserAgeData"
        private val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY$PATH")
    }
}

object UserAgeDataResponse {
    const val COLUMN_RESPONSE_STATUS: String = "responseStatus"
    const val COLUMN_USER_STATUS: String = "userStatus"
    const val COLUMN_USER_ID: String = "userId"
    const val COLUMN_MOST_RECENT_APPROVAL_DATE: String = "mostRecentApprovalDate"
    const val COLUMN_AGE_LOWER: String = "ageLower"
    const val COLUMN_AGE_UPPER: String = "ageUpper"
}

data class UserData(
    val responseStatus: String?,
    val userStatus: String?,
    val ageLower: Int?,
    val ageUpper: Int?,
    val userId: String?,
    val mostRecentApprovalDate: String?
)


