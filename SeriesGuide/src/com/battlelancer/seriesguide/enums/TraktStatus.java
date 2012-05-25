
package com.battlelancer.seriesguide.enums;

/**
 * Pseudo-enum (aka an interface) used to compare against returned status values
 * from the trakt api.
 */
public interface TraktStatus {
    String SUCCESS = "success";

    String FAILURE = "failure";
}
