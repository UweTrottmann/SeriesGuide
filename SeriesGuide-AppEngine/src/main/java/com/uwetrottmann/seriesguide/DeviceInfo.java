package com.uwetrottmann.seriesguide;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * An entity for Android device information.
 *
 * Its associated endpoint, DeviceInfoEndpoint.java, was directly generated from this class - the
 * Google Plugin for Eclipse allows you to generate endpoints directly from entities!
 *
 * DeviceInfoEndpoint.java will be used for registering devices with this App Engine application.
 * Registered devices will receive messages broadcast by this application over Google Cloud
 * Messaging (GCM). If you'd like to take a look at the broadcasting code, check out
 * MessageEndpoint.java.
 *
 * For more information, see http://developers.google.com/eclipse/docs/cloud_endpoints.
 *
 * NOTE: This DeviceInfoEndpoint.java does not use any form of authorization or authentication! If
 * this app is deployed, anyone can access this endpoint! If you'd like to add authentication, take
 * a look at the documentation.
 */
@Entity
// DeviceInfoEndpoint has NO AUTHENTICATION - it is an OPEN ENDPOINT!
public class DeviceInfo {

    /*
     * The Google Cloud Messaging registration token for the device. This token
     * indicates that the device is able to receive messages sent via GCM.
     */
    @Id
    private String deviceRegistrationID;

    /*
     * Some identifying information about the device, such as its manufacturer
     * and product name.
     */
    private String deviceInformation;

    /*
     * Timestamp indicating when this device registered with the application.
     */
    private long timestamp;

    public String getDeviceRegistrationID() {
        return deviceRegistrationID;
    }

    public String getDeviceInformation() {
        return this.deviceInformation;
    }

    public void setDeviceRegistrationID(String deviceRegistrationID) {
        this.deviceRegistrationID = deviceRegistrationID;
    }

    public void setDeviceInformation(String deviceInformation) {
        this.deviceInformation = deviceInformation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
