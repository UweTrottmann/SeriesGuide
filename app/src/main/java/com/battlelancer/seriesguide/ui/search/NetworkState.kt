package com.battlelancer.seriesguide.ui.search

enum class Status {
    LOADING,
    LOADED,
    ERROR
}

data class NetworkState(val status: Status, val message: String? = null) {
    companion object {
        val LOADING = NetworkState(Status.LOADING)
        val LOADED = NetworkState(Status.LOADED)
        fun error(message: String) = NetworkState(Status.ERROR, message)
    }
}