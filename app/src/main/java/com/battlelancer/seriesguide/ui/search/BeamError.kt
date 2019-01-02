package com.battlelancer.seriesguide.ui.search

/**
 * Error for tracking Android Beam failures.
 */
class BeamError(action: String, failure: String) : Throwable("$action: $failure")