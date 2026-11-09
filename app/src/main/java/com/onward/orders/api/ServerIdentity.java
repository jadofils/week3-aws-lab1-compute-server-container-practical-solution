package com.onward.orders.api;

import java.net.InetAddress;
import org.springframework.stereotype.Component;

@Component
public class ServerIdentity {
    public String currentServer() {
        String configuredHost = System.getenv("HOSTNAME");
        if (configuredHost != null && !configuredHost.isBlank()) {
            return configuredHost;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "unknown-server";
        }
    }
}
