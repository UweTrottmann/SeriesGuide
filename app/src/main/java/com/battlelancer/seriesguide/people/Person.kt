// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.people

data class Credits(
    val tmdbId: Int,
    val cast: List<Person>,
    val crew: List<Person>
)

data class Person(
    val tmdbId: Int,
    val name: String,
    val description: String?,
    val profilePath: String?,
    /**
     * Only for crew members.
     */
    val department: String? = null
)
