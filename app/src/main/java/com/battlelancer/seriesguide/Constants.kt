package com.battlelancer.seriesguide

object Constants {
    /**
     * See [com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes.FIRSTAIREDMS].
     */
    const val EPISODE_UNKNOWN_RELEASE = -1

    enum class SeasonSorting(val index: Int, private val value: String) {
        LATEST_FIRST(0, "latestfirst"),
        OLDEST_FIRST(1, "oldestfirst");

        override fun toString(): String {
            return value
        }

        companion object {
            fun fromValue(value: String?): SeasonSorting {
                if (value != null) {
                    for (sorting in values()) {
                        if (sorting.value == value) {
                            return sorting
                        }
                    }
                }
                return LATEST_FIRST
            }
        }
    }
}