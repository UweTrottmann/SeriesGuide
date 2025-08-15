// SPDX-License-Identifier: Apache-2.0
// Copyright 2023-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.uwetrottmann.androidutils.AndroidUtils
import timber.log.Timber

/**
 * Helpers that work with [PackageManager].
 */
object PackageTools {

    private const val FLAVOR_AMAZON = "amazon"
    private const val PACKAGE_NAME_PLAY_STORE = "com.android.vending"

    /**
     * Check if this is a build for the Amazon app store.
     */
    @JvmStatic
    @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
    fun isAmazonVersion(): Boolean = FLAVOR_AMAZON == BuildConfig.FLAVOR

    fun getAppPackage(context: Context): PackageInfo {
        return context.packageManager.getPackageInfoCompat(context.packageName, 0)
    }

    /**
     * Get version name from this apps package.
     */
    fun getVersion(context: Context): String {
        val version = try {
            getAppPackage(context).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return version ?: "UnknownVersion"
    }

    /**
     * Return a version string like "v42 (Database v42)". On debug builds adds the version code.
     */
    fun getVersionString(context: Context): String {
        return if (BuildConfig.DEBUG) {
            context.getString(
                R.string.format_version_debug, getVersion(context),
                SgRoomDatabase.VERSION, BuildConfig.VERSION_CODE
            )
        } else {
            context.getString(
                R.string.format_version, getVersion(context),
                SgRoomDatabase.VERSION
            )
        }
    }

    /**
     * Returns if the user has a valid copy of the X Pass app installed.
     */
    @JvmStatic
    fun hasUnlockKeyInstalled(context: Context): Boolean {
        try {
            // Get our signing key
            val manager = context.packageManager
            @SuppressLint("PackageManagerGetSignatures") val appInfoSeriesGuide = manager
                .getPackageInfoCompat(
                    context.applicationContext.packageName,
                    PackageManager.GET_SIGNATURES
                )

            // Try to find the X signing key
            @SuppressLint("PackageManagerGetSignatures") val appInfoSeriesGuideX = manager
                .getPackageInfoCompat(
                    "com.battlelancer.seriesguide.x",
                    PackageManager.GET_SIGNATURES
                )
            val sgSignatures = appInfoSeriesGuide.signatures
            val xSignatures = appInfoSeriesGuideX.signatures
            if (sgSignatures != null && xSignatures != null
                && sgSignatures.size == xSignatures.size) {
                for (i in sgSignatures.indices) {
                    if (sgSignatures[i].toCharsString() != xSignatures[i].toCharsString()) {
                        return false // a signature does not match
                    }
                }
                return true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Expected exception that occurs if the package is not present.
            Timber.d("X Pass not found.")
        }
        return false
    }

    fun wasInstalledByPlayStore(context: Context): Boolean {
        return getInstallerPackageName(context) == PACKAGE_NAME_PLAY_STORE
    }

    private fun getInstallerPackageName(context: Context): String {
        val packageName = context.packageName
        val packageManager = context.packageManager
        return try {
            if (AndroidUtils.isAtLeastR) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            } ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Failed to get installer package name")
            ""
        }.also { Timber.d("installingPackageName = '%s'", it) }
    }

    fun isDeviceInEEA(context: Context): Boolean {
        @Suppress("DEPRECATION")
        val region = context.resources.configuration.locale.country
        return if (region.isEmpty()) {
            false
        } else {
            val eeaRegions = context.resources.getStringArray(R.array.eea_regions)
            eeaRegions.contains(region)
        }.also { Timber.d("region = '%s', isEEA = %s", region, it) }
    }

}

private fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo {
    return if (AndroidUtils.isAtLeastTiramisu) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags)
    }
}