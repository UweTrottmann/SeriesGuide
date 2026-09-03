// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.CommonExtension

fun CommonExtension.configureAndroid() {
    compileSdk = 36 // Android 16 (BAKLAVA)
    defaultConfig.minSdk = 23 // Android 6 (M)

    compileOptions.apply {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // For CI: print reports to standard output (report files are not public)
    lint.printTextReport = true
}

extensions.findByType<ApplicationExtension>()?.apply {
    configureAndroid()
    defaultConfig.targetSdk = 36 // Android 16 (BAKLAVA)
}

extensions.findByType<LibraryExtension>()?.apply {
    configureAndroid()
    // Should match with application configuration
    lint.targetSdk = 35
    testOptions.targetSdk = 35
}
