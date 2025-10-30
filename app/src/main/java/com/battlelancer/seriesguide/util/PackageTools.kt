// SPDX-License-Identifier: Apache-2.0
// Copyright 2023-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.uwetrottmann.androidutils.AndroidUtils
import timber.log.Timber
import java.util.Locale

/**
 * Helpers that work with [PackageManager].
 */
object PackageTools {

    private const val FLAVOR_AMAZON = "amazon"
    private const val PACKAGE_NAME_PASS = "com.battlelancer.seriesguide.x"
    private const val SIGNATURE_HASH_PASS = 528716598
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
     * Returns if the unlock app is installed.
     */
    fun hasUnlockKeyInstalled(context: Context): Boolean {
        try {
            // Get signing info from unlock app (if it's installed, otherwise throws)
            if (AndroidUtils.isAtLeastPie) {
                context.packageManager.getPackageInfoCompat(
                    PACKAGE_NAME_PASS,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfoCompat(
                    PACKAGE_NAME_PASS,
                    PackageManager.GET_SIGNATURES
                ).signatures
            }?.let {
                for (signature in it) {
                    // One of the signatures needs to match the expected one
                    if (signature.hashCode() == SIGNATURE_HASH_PASS) return true
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
            // Expected exception that occurs if the pass app is not installed
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

    @JvmInline
    value class DeviceRegion(val code: String)

    fun getDeviceRegion(context: Context): DeviceRegion {
        val region = if (AndroidUtils.isNougatOrHigher) {
            context.resources.configuration.locales[0].country
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.country
        }
        return DeviceRegion(region)
    }

    fun DeviceRegion.isEuropeanEconomicArea(context: Context): Boolean {
        return if (code.isEmpty()) {
            false
        } else {
            val eeaRegions = context.resources.getStringArray(R.array.eea_regions)
            eeaRegions.contains(code)
        }
    }

    fun DeviceRegion.isUnitedStates() : Boolean {
        return code == Locale.US.country
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