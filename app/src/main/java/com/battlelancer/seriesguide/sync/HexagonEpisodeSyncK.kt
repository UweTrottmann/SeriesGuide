package com.battlelancer.seriesguide.sync

data class DownloadFlagsResult(
    val success: Boolean,
    val noData: Boolean,
    val lastWatchedMs: Long?
) {
    companion object {
        @JvmField
        val FAILED = DownloadFlagsResult(success = false, noData = false, lastWatchedMs = null)
        @JvmField
        val NO_DATA = DownloadFlagsResult(success = true, noData = true, lastWatchedMs = null)
    }
}

data class ShowLastWatchedInfo(
    val lastWatchedMs: Long,
    val episodeSeason: Int,
    val episodeNumber: Int
)
