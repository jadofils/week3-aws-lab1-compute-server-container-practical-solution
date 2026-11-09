package com.onward.orders.service.impl;

import com.onward.orders.config.IdentityProperties;
import com.onward.orders.model.ServerIdentity;
import com.onward.orders.service.ServerIdentityService;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads the instance ID, Availability Zone and private IP from the EC2 Instance Metadata
 * Service (IMDSv2, token required). Off EC2 (for example on a laptop) it falls back to the
 * host name. The identity never changes for a running server, so it is looked up once.
 */
@Service
public class Ec2MetadataServerIdentityService implements ServerIdentityService {
    private static final Logger log = LoggerFactory.getLogger(Ec2MetadataServerIdentityService.class);

    private final IdentityProperties properties;
    private volatile ServerIdentity cached;

    public Ec2MetadataServerIdentityService(IdentityProperties properties) {
        this.properties = properties;
    }

    @Override
    public ServerIdentity current() {
        ServerIdentity identity = cached;
        if (identity == null) {
            synchronized (this) {
                if (cached == null) {
                    cached = resolve();
                }
                identity = cached;
            }
        }
        return identity;
    }

    private ServerIdentity resolve() {
        if (properties.ec2MetadataEnabled()) {
            try {
                return fromInstanceMetadata();
            } catch (Exception ex) {
                log.info("EC2 instance metadata not available ({}); using host name", ex.getMessage());
            }
        }
        return fromHost();
    }

    private ServerIdentity fromInstanceMetadata() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(properties.metadataTimeout()).build();
        String base = properties.metadataEndpoint();

        HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create(base + "/latest/api/token"))
                .timeout(properties.metadataTimeout())
                .header("X-aws-ec2-metadata-token-ttl-seconds", "300")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        String token = send(client, tokenRequest);

        return new ServerIdentity(
                get(client, base, token, "instance-id"),
                get(client, base, token, "placement/availability-zone"),
                get(client, base, token, "local-ipv4"));
    }

    private String get(HttpClient client, String base, String token, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/latest/meta-data/" + path))
                .timeout(properties.metadataTimeout())
                .header("X-aws-ec2-metadata-token", token)
                .GET()
                .build();
        return send(client, request);
    }

    private static String send(HttpClient client, HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().isBlank()) {
            throw new IllegalStateException("metadata returned HTTP " + response.statusCode());
        }
        return response.body().trim();
    }

    private static ServerIdentity fromHost() {
        try {
            InetAddress local = InetAddress.getLocalHost();
            return new ServerIdentity(local.getHostName(), "local", local.getHostAddress());
        } catch (Exception ignored) {
            return new ServerIdentity("unknown-server", "unknown", "unknown");
        }
    }
}
