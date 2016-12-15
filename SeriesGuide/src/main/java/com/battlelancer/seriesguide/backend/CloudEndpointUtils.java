package com.battlelancer.seriesguide.backend;

import android.content.Context;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.util.Utils;
import com.google.api.client.googleapis.services.AbstractGoogleClient;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import java.io.IOException;

/**
 * Common utilities for working with Cloud Endpoints.
 *
 * If you'd like to test using a locally-running version of your App Engine backend (i.e. running on
 * the Development App Server), you need to set LOCAL_ANDROID_RUN to 'true'.
 *
 * See https://cloud.google.com/appengine/docs/java/endpoints
 */
public class CloudEndpointUtils {

    /*
     * Change this to 'true' if you're running your backend locally using the DevAppServer.
     */
    @SuppressWarnings("PointlessBooleanExpression")
    protected static final boolean LOCAL_ANDROID_RUN = false && BuildConfig.DEBUG;

    /*
     * The root URL of where your DevAppServer is running (if you're running the
     * DevAppServer locally).
     */
    @SuppressWarnings("unused")
    protected static final String LOCAL_APP_ENGINE_SERVER_URL = "http://localhost:8080/";

    /*
     * The root URL of where your DevAppServer is running when it's being
     * accessed via the Android emulator (if you're running the DevAppServer
     * locally). In this case, you're running behind Android's virtual router.
     * See
     * http://developer.android.com/tools/devices/emulator.html#networkaddresses
     * for more information.
     */
    protected static final String LOCAL_APP_ENGINE_SERVER_URL_FOR_ANDROID = "http://10.0.2.2:8080";

    /**
     * Updates the Google client builder to connect the appropriate server based on whether
     * LOCAL_ANDROID_RUN is true or false and sets a custom user agent.
     *
     * @param builder Google client builder
     * @return same Google client builder
     */
    public static <B extends AbstractGoogleClient.Builder> B updateBuilder(Context context,
            B builder) {
        if (LOCAL_ANDROID_RUN) {
            builder.setRootUrl(LOCAL_APP_ENGINE_SERVER_URL_FOR_ANDROID + "/_ah/api/");
        }
        // used for user agent
        builder.setApplicationName("SeriesGuide " + Utils.getVersion(context));

        // only enable GZip when connecting to remote server
        final boolean enableGZip = builder.getRootUrl().startsWith("https:");

        builder.setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
            public void initialize(AbstractGoogleClientRequest<?> request)
                    throws IOException {
                if (!enableGZip) {
                    request.setDisableGZipContent(true);
                }
            }
        });

        return builder;
    }
}
