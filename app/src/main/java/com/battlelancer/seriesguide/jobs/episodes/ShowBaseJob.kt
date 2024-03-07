// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2021, 2023, 2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

abstract class ShowBaseJob(
    override val showId: Long,
    flagValue: Int,
    action: JobAction
) : BaseEpisodesJob(flagValue, action)
