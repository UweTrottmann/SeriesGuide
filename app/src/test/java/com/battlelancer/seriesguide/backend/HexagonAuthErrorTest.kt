// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.backend

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HexagonAuthErrorTest {

    @Test
    fun isSignInRequiredError() {
        val testException = ApiException(Status(CommonStatusCodes.SIGN_IN_REQUIRED))
        val hexagonError = HexagonAuthError.build("Test action", testException)

        // Status code is extracted.
        assertThat(hexagonError.statusCode).isEqualTo(CommonStatusCodes.SIGN_IN_REQUIRED)
        assertThat(hexagonError.action).isEqualTo("Test action")
        // Detects ApiException with sign-in required code.
        assertThat(hexagonError.isSignInRequiredError()).isTrue()
    }
}