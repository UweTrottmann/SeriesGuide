package com.battlelancer.seriesguide.backend

import android.content.Context
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.util.Utils
import com.google.api.client.googleapis.services.AbstractGoogleClient
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer

/**
 * Common utilities for working with Cloud Endpoints.
 *
 * If you'd like to test using a locally-running version of your App Engine backend (i.e. running on
 * the Development App Server), you need to set USE_LOCAL_VERSION to 'true'.
 *
 * See https://cloud.google.com/appengine/docs/java/endpoints
 */
object CloudEndpointUtils {

    private const val PATH_API = "/_ah/api/"

    /**
     * Change this to 'true' if you're running your backend locally using the DevAppServer.
     */
    @Suppress("SimplifyBooleanWithConstants")
    private val USE_LOCAL_VERSION = false && BuildConfig.DEBUG

    @Suppress("SimplifyBooleanWithConstants")
    private val USE_STAGING_VERSION = false && BuildConfig.DEBUG

    private const val ROOT_URL_STAGING = "https://staging-dot-optical-hexagon-364.appspot.com"

    /**
     * The root URL of where your DevAppServer is running (if you're running the DevAppServer
     * locally).
     */
    private const val ROOT_URL_LOCALHOST = "http://localhost:8080/"

    /**
     * The root URL of where your DevAppServer is running when it's being accessed via the Android
     * emulator (if you're running the DevAppServer locally). In this case, you're running behind
     * Android's virtual router. See http://developer.android.com/tools/devices/emulator.html#networkaddresses
     * for more information.
     */
    private const val ROOT_URL_LOCALHOST_ANDROID = "http://10.0.2.2:8080"

    /**
     * Updates the Google client builder to connect the appropriate server based on whether
     * USE_LOCAL_VERSION is true or false and sets a custom user agent.
     *
     * @param builder Google client builder
     * @return same Google client builder
     */
    @JvmStatic
    fun <B : AbstractGoogleClient.Builder> updateBuilder(
        context: Context,
        builder: B
    ): B {
        if (USE_LOCAL_VERSION) {
            builder.rootUrl = ROOT_URL_LOCALHOST_ANDROID + PATH_API
        } else if (USE_STAGING_VERSION) {
            builder.rootUrl = ROOT_URL_STAGING + PATH_API
        }
        // used for user agent
        builder.applicationName = "SeriesGuide " + Utils.getVersion(context)

        // only enable GZip when connecting to remote server
        val enableGZip = builder.rootUrl.startsWith("https:")

        builder.googleClientRequestInitializer =
            GoogleClientRequestInitializer { request: AbstractGoogleClientRequest<*> ->
                if (!enableGZip) {
                    request.disableGZipContent = true
                }
            }

        return builder
    }
}