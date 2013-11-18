package com.uwetrottmann.seriesguide;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;

import com.uwetrottmann.seriesguide.deviceinfoendpoint.Deviceinfoendpoint;
import com.uwetrottmann.seriesguide.deviceinfoendpoint.model.DeviceInfo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * This class is started up as a service of the Android application. It listens
 * for Google Cloud Messaging (GCM) messages directed to this device.
 *
 * When the device is successfully registered for GCM, a message is sent to the
 * App Engine backend via Cloud Endpoints, indicating that it wants to receive
 * broadcast messages from the it.
 *
 * Before registering for GCM, you have to create a project in Google's Cloud
 * Console (https://code.google.com/apis/console). In this project, you'll have
 * to enable the "Google Cloud Messaging for Android" Service.
 *
 * Once you have set up a project and enabled GCM, you'll have to set the
 * PROJECT_NUMBER field to the project number mentioned in the "Overview" page.
 *
 * See the documentation at
 * http://developers.google.com/eclipse/docs/cloud_endpoints for more
 * information.
 */
public class GCMIntentService extends GCMBaseIntentService {
  private final Deviceinfoendpoint endpoint;

  /*
   * TODO: Set this to a valid project number. See
   * http://developers.google.com/eclipse/docs/cloud_endpoint for more
   * information.
   */
  protected static final String PROJECT_NUMBER = "137959300653";

  /**
   * Register the device for GCM.
   *
   * @param mContext
   *            the activity's context.
   */
  public static void register(Context mContext) {
    GCMRegistrar.checkDevice(mContext);
    GCMRegistrar.checkManifest(mContext);
    GCMRegistrar.register(mContext, PROJECT_NUMBER);
  }

  /**
   * Unregister the device from the GCM service.
   *
   * @param mContext
   *            the activity's context.
   */
  public static void unregister(Context mContext) {
    GCMRegistrar.unregister(mContext);
  }

  public GCMIntentService() {
    super(PROJECT_NUMBER);
    Deviceinfoendpoint.Builder endpointBuilder = new Deviceinfoendpoint.Builder(
        AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
        new HttpRequestInitializer() {
          public void initialize(HttpRequest httpRequest) {
          }
        });
    endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
  }

  /**
   * Called on registration error. This is called in the context of a Service
   * - no dialog or UI.
   *
   * @param context
   *            the Context
   * @param errorId
   *            an error message
   */
  @Override
  public void onError(Context context, String errorId) {

    sendNotificationIntent(
        context,
        "Registration with Google Cloud Messaging...FAILED!\n\n"
            + "A Google Cloud Messaging registration error occured (errorid: "
            + errorId
            + "). "
            + "Do you have your project number ("
            + ("".equals(PROJECT_NUMBER) ? "<unset>"
                : PROJECT_NUMBER)
            + ")  set correctly, and do you have Google Cloud Messaging enabled for the "
            + "project?", true, true);
  }

  /**
   * Called when a cloud message has been received.
   */
  @Override
  public void onMessage(Context context, Intent intent) {
    sendNotificationIntent(
        context,
        "Message received via Google Cloud Messaging:\n\n"
            + intent.getStringExtra("message"), true, false);
  }

  /**
   * Called back when a registration token has been received from the Google
   * Cloud Messaging service.
   *
   * @param context
   *            the Context
   */
  @Override
  public void onRegistered(Context context, String registration) {
    /*
     * This is some special exception-handling code that we're using to work around a problem
     * with the DevAppServer and methods that return null in App Engine 1.7.5.
     */
    boolean alreadyRegisteredWithEndpointServer = false;

    try {

      /*
       * Using cloud endpoints, see if the device has already been
       * registered with the backend
       */
      DeviceInfo existingInfo = endpoint.getDeviceInfo(registration)
          .execute();

      if (existingInfo != null && registration.equals(existingInfo.getDeviceRegistrationID())) {
        alreadyRegisteredWithEndpointServer = true;
      }
    } catch (IOException e) {
      // Ignore
    }

    try {
      if (!alreadyRegisteredWithEndpointServer) {
        /*
         * We are not registered as yet. Send an endpoint message
         * containing the GCM registration id and some of the device's
         * product information over to the backend. Then, we'll be
         * registered.
         */
        DeviceInfo deviceInfo = new DeviceInfo();
        endpoint.insertDeviceInfo(
            deviceInfo
                .setDeviceRegistrationID(registration)
                .setTimestamp(System.currentTimeMillis())
                .setDeviceInformation(
                    URLEncoder
                        .encode(android.os.Build.MANUFACTURER
                            + " "
                            + android.os.Build.PRODUCT,
                            "UTF-8"))).execute();
      }
    } catch (IOException e) {
      Log.e(GCMIntentService.class.getName(),
          "Exception received when attempting to register with server at "
              + endpoint.getRootUrl(), e);

      sendNotificationIntent(
          context,
          "1) Registration with Google Cloud Messaging...SUCCEEDED!\n\n"
              + "2) Registration with Endpoints Server...FAILED!\n\n"
              + "Unable to register your device with your Cloud Endpoints server running at "
              + endpoint.getRootUrl()
              + ". Either your Cloud Endpoints server is not deployed to App Engine, or "
              + "your settings need to be changed to run against a local instance "
              + "by setting LOCAL_ANDROID_RUN to 'true' in CloudEndpointUtils.java.",
          true, true);
      return;
    }

    sendNotificationIntent(
        context,
        "1) Registration with Google Cloud Messaging...SUCCEEDED!\n\n"
            + "2) Registration with Endpoints Server...SUCCEEDED!\n\n"
            + "Device registration with Cloud Endpoints Server running at  "
            + endpoint.getRootUrl()
            + " succeeded!\n\n"
            + "To send a message to this device, "
            + "open your browser and navigate to the sample application at "
            + getWebSampleUrl(endpoint.getRootUrl()), false, true);
  }

  /**
   * Called back when the Google Cloud Messaging service has unregistered the
   * device.
   *
   * @param context
   *            the Context
   */
  @Override
  protected void onUnregistered(Context context, String registrationId) {

    if (registrationId != null && registrationId.length() > 0) {

      try {
        endpoint.removeDeviceInfo(registrationId).execute();
      } catch (IOException e) {
        Log.e(GCMIntentService.class.getName(),
            "Exception received when attempting to unregister with server at "
                + endpoint.getRootUrl(), e);
        sendNotificationIntent(
            context,
            "1) De-registration with Google Cloud Messaging....SUCCEEDED!\n\n"
                + "2) De-registration with Endpoints Server...FAILED!\n\n"
                + "We were unable to de-register your device from your Cloud "
                + "Endpoints server running at "
                + endpoint.getRootUrl() + "."
                + "See your Android log for more information.",
            true, true);
        return;
      }
    }

    sendNotificationIntent(
        context,
        "1) De-registration with Google Cloud Messaging....SUCCEEDED!\n\n"
            + "2) De-registration with Endpoints Server...SUCCEEDED!\n\n"
            + "Device de-registration with Cloud Endpoints server running at  "
            + endpoint.getRootUrl() + " succeeded!", false, true);
  }

  /**
   * Generate a notification intent and dispatch it to the RegisterActivity.
   * This is how we get information from this service (non-UI) back to the
   * activity.
   *
   * For this to work, the 'android:launchMode="singleTop"' attribute needs to
   * be set for the RegisterActivity in AndroidManifest.xml.
   *
   * @param context
   *            the application context
   * @param message
   *            the message to send
   * @param isError
   *            true if the message is an error-related message; false
   *            otherwise
   * @param isRegistrationMessage
   *            true if this message is related to registration/unregistration
   */
  private void sendNotificationIntent(Context context, String message,
      boolean isError, boolean isRegistrationMessage) {
    Intent notificationIntent = new Intent(context, RegisterActivity.class);
    notificationIntent.putExtra("gcmIntentServiceMessage", true);
    notificationIntent.putExtra("registrationMessage",
        isRegistrationMessage);
    notificationIntent.putExtra("error", isError);
    notificationIntent.putExtra("message", message);
    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(notificationIntent);
  }

  private String getWebSampleUrl(String endpointUrl) {
    // Not the most elegant solution; we'll improve this in the future
    if (CloudEndpointUtils.LOCAL_ANDROID_RUN) {
      return CloudEndpointUtils.LOCAL_APP_ENGINE_SERVER_URL
          + "index.html";
    }
    return endpointUrl.replace("/_ah/api/", "/index.html");
  }
}
