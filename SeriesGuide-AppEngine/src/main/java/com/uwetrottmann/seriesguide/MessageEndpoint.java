package com.uwetrottmann.seriesguide;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.datanucleus.query.JPACursorHelper;


/**
 * A simple Cloud Endpoint that receives notifications from a web client (<server url>/index.html),
 * and broadcasts them to all of the devices that were registered with this application (via
 * DeviceInfoEndpoint).
 *
 * In order for this sample to work, you have to populate the API_KEY field with your key for server
 * apps. You can obtain this key from the API console (https://code.google.com/apis/console). You'll
 * first have to create a project and enable the Google Cloud Messaging Service for it, as described
 * in the javadoc for GCMIntentService.java (in your Android project).
 *
 * After creating the project in the API console, browse to the "API Access" section, and select the
 * option to "Create a New Server Key". The generated key is what you'll enter into the API_KEY
 * field.
 *
 * See the documentation at http://developers.google.com/eclipse/docs/cloud_endpoints for more
 * information.
 *
 * NOTE: This endpoint does not use any form of authorization or authentication! If this app is
 * deployed, anyone can access this endpoint! If you'd like to add authentication, take a look at
 * the documentation.
 */
@Api(name = "messageEndpoint",
        namespace = @ApiNamespace(ownerDomain = "uwetrottmann.com", ownerName = "uwetrottmann.com",
                packagePath = "seriesguide"))
// NO AUTHENTICATION; OPEN ENDPOINT!
public class MessageEndpoint {

    /*
     * TODO: Fill this in with the server key that you've obtained from the API
     * Console (https://code.google.com/apis/console). This is required for using
     * Google Cloud Messaging from your AppEngine application even if you are
     * using a App Engine's local development server.
     */
    private static final String API_KEY = "";

    private static final DeviceInfoEndpoint endpoint = new DeviceInfoEndpoint();

    /**
     * This function returns a list of messages starting with the newest message first and in
     * descending order from there
     *
     * @param cursorString for paging, empty for the first request, subsequent requests can use the
     *                     returned information from an earlier request to fill this parameter
     * @param limit        number of results returned for this query
     * @return A collection of MessageData items
     */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listMessages")
    public CollectionResponse<MessageData> listMessages(
            @Nullable @Named("cursor") String cursorString,
            @Nullable @Named("limit") Integer limit) {

        EntityManager mgr = null;
        Cursor cursor = null;
        List<MessageData> execute = null;

        try {
            mgr = getEntityManager();
            // query for messages, newest message first
            Query query = mgr
                    .createQuery("select from MessageData as MessageData order by timestamp desc");
            if (cursorString != null && cursorString != "") {
                cursor = Cursor.fromWebSafeString(cursorString);
                query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
            }

            if (limit != null) {
                query.setFirstResult(0);
                query.setMaxResults(limit);
            }

            execute = (List<MessageData>) query.getResultList();
            cursor = JPACursorHelper.getCursor(execute);
            if (cursor != null) {
                cursorString = cursor.toWebSafeString();
            }

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (MessageData obj : execute) {
                ;
            }
        } finally {
            mgr.close();
        }

        return CollectionResponse.<MessageData>builder().setItems(execute)
                .setNextPageToken(cursorString).build();
    }

    /**
     * This accepts a message and persists it in the AppEngine datastore, it will also broadcast the
     * message to upto 10 registered android devices via Google Cloud Messaging
     *
     * @param message the entity to be inserted.
     */
    @ApiMethod(name = "sendMessage")
    public void sendMessage(@Named("message") String message)
            throws IOException {
        Sender sender = new Sender(API_KEY);
        // create a MessageData entity with a timestamp of when it was
        // received, and persist it
        MessageData messageObj = new MessageData();
        messageObj.setMessage(message);
        messageObj.setTimestamp(System.currentTimeMillis());
        EntityManager mgr = getEntityManager();
        try {
            mgr.persist(messageObj);
        } finally {
            mgr.close();
        }
        // ping a max of 10 registered devices
        CollectionResponse<DeviceInfo> response = endpoint.listDeviceInfo(null,
                10);
        for (DeviceInfo deviceInfo : response.getItems()) {
            doSendViaGcm(message, sender, deviceInfo);
        }
    }

    /**
     * Sends the message using the Sender object to the registered device.
     *
     * @param message    the message to be sent in the GCM ping to the device.
     * @param sender     the Sender object to be used for ping,
     * @param deviceInfo the registration id of the device.
     * @return Result the result of the ping.
     */
    private static Result doSendViaGcm(String message, Sender sender,
            DeviceInfo deviceInfo) throws IOException {
        // Trim message if needed.
        if (message.length() > 1000) {
            message = message.substring(0, 1000) + "[...]";
        }

        // This message object is a Google Cloud Messaging object, it is NOT
        // related to the MessageData class
        Message msg = new Message.Builder().addData("message", message).build();
        Result result = sender.send(msg, deviceInfo.getDeviceRegistrationID(),
                5);
        if (result.getMessageId() != null) {
            String canonicalRegId = result.getCanonicalRegistrationId();
            if (canonicalRegId != null) {
                endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
                deviceInfo.setDeviceRegistrationID(canonicalRegId);
                endpoint.insertDeviceInfo(deviceInfo);
            }
        } else {
            String error = result.getErrorCodeName();
            if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
            }
        }

        return result;
    }

    private static EntityManager getEntityManager() {
        return EMF.get().createEntityManager();
    }
}
