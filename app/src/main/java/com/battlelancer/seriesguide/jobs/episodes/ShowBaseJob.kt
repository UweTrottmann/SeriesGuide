// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2017, 2018, 2021, 2023, 2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

abstract class ShowBaseJob(
    override val showId: Long,
    flagValue: Int,
    action: JobAction
) : BaseEpisodesJob(flagValue, action)
