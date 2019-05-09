package com.battlelancer.seriesguide.model

data class EpisodeWithShow (
    val episodeTvdbId: Int = 0,
    val episodetitle: String? = null,
    val episodenumber: Int = 0,
    val season: Int = 0,
    val episode_firstairedms: Long = 0,
    val watched: Int = 0,
    val episode_collected: Boolean = false,

    val showTvdbId: Int = 0,
    val seriestitle: String? = null,
    val network: String? = null,
    val poster: String? = null
)