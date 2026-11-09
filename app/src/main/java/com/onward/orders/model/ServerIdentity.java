package com.onward.orders.model;

/**
 * Which server answered: on EC2 this is the instance ID, Availability Zone and private IP.
 */
public record ServerIdentity(String serverId, String availabilityZone, String privateIp) {

    /** Short label shown in the UI and stored on orders, e.g. "i-0abc123 · eu-west-1a". */
    public String label() {
        return serverId + " · " + availabilityZone;
    }
}
