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

    fun getAppPackage(context: Context): PackageInfo {
        return context.packageManager.getPackageInfoCompat(context.packageName, 0)
    }

    /**
     * Get version name from this apps package.
     */
    fun getVersion(context: Context): String {
        return try {
            getAppPackage(context).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "UnknownVersion"
        }
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
     * Returns if the user has a valid copy of X Pass installed.
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
            if (sgSignatures.size == xSignatures.size) {
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
}

private fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo {
    return if (AndroidUtils.isAtLeastTiramisu) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags)
    }
}