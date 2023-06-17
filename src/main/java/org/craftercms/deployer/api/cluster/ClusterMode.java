package org.craftercms.deployer.api.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents the possible states a cluster instance can be in.
 */
public enum ClusterMode {
    PRIMARY,
    REPLICA,
    // This means either that the actual value could not be retrieved
    // from studio at the time of the request or that the Studio instance
    // is initializing or transitioning from one mode to another.
    UNKNOWN;

    @JsonCreator
    public static ClusterMode fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ClusterMode mode : ClusterMode.values()) {
            if (value.equalsIgnoreCase(mode.name())) {
                return mode;
            }
        }
        return null;
    }
}
