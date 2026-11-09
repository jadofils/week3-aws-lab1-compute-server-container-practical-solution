package com.onward.orders.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How the app discovers which server it is running on, bound from {@code app.identity.*}.
 */
@ConfigurationProperties(prefix = "app.identity")
public record IdentityProperties(
        @DefaultValue("true") boolean ec2MetadataEnabled,
        @DefaultValue("http://169.254.169.254") String metadataEndpoint,
        @DefaultValue("500ms") Duration metadataTimeout) {
}
