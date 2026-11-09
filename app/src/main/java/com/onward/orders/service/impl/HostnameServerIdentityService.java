package com.onward.orders.service.impl;

import com.onward.orders.service.ServerIdentityService;
import java.net.InetAddress;
import org.springframework.stereotype.Service;

@Service
public class HostnameServerIdentityService implements ServerIdentityService {
    @Override
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
