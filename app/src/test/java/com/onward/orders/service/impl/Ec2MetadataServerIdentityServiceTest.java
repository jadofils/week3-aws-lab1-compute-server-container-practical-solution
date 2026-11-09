package com.onward.orders.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.onward.orders.config.IdentityProperties;
import com.onward.orders.model.ServerIdentity;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Ec2MetadataServerIdentityServiceTest {
    private HttpServer fakeImds;

    @AfterEach
    void stop() {
        if (fakeImds != null) {
            fakeImds.stop(0);
        }
    }

    @Test
    void readsIdentityFromImdsV2WithToken() throws Exception {
        String endpoint = startFakeImds(Map.of(
                "/latest/meta-data/instance-id", "i-0abc123",
                "/latest/meta-data/placement/availability-zone", "eu-west-1b",
                "/latest/meta-data/local-ipv4", "10.0.11.42"));

        ServerIdentity identity = new Ec2MetadataServerIdentityService(
                new IdentityProperties(true, endpoint, Duration.ofSeconds(2))).current();

        assertThat(identity).isEqualTo(new ServerIdentity("i-0abc123", "eu-west-1b", "10.0.11.42"));
        assertThat(identity.label()).isEqualTo("i-0abc123 · eu-west-1b");
    }

    @Test
    void fallsBackToHostNameWhenMetadataIsUnavailable() {
        ServerIdentity identity = new Ec2MetadataServerIdentityService(
                new IdentityProperties(true, "http://127.0.0.1:9", Duration.ofMillis(300))).current();

        assertThat(identity.availabilityZone()).isEqualTo("local");
        assertThat(identity.serverId()).isNotBlank();
    }

    /** Minimal IMDSv2: PUT /latest/api/token returns a token; metadata GETs require it. */
    private String startFakeImds(Map<String, String> metadata) throws Exception {
        fakeImds = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeImds.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            int status;
            String body;
            if (path.equals("/latest/api/token") && exchange.getRequestMethod().equals("PUT")) {
                status = 200;
                body = "test-token";
            } else if (!"test-token".equals(exchange.getRequestHeaders().getFirst("X-aws-ec2-metadata-token"))) {
                status = 401;
                body = "";
            } else if (metadata.containsKey(path)) {
                status = 200;
                body = metadata.get(path);
            } else {
                status = 404;
                body = "";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        fakeImds.start();
        return "http://127.0.0.1:" + fakeImds.getAddress().getPort();
    }
}
