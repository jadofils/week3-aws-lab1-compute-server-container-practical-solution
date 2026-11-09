package com.onward.orders.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onward.orders.config.LoadProperties;
import com.onward.orders.dto.response.LoadStatusResponse;
import com.onward.orders.exception.InvalidLoadRequestException;
import com.onward.orders.exception.LoadGeneratorDisabledException;
import com.onward.orders.model.ServerIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CpuSpikeLoadGeneratorServiceTest {
    private final ServerIdentity identity = new ServerIdentity("i-test", "eu-west-1a", "10.0.10.5");
    private CpuSpikeLoadGeneratorService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    void startsOneSpikeAndReportsItUntilStopped() {
        service = new CpuSpikeLoadGeneratorService(new LoadProperties(true, 30, 600), () -> identity);

        LoadStatusResponse first = service.start(30);
        assertThat(first.started()).isTrue();
        assertThat(first.active()).isTrue();
        assertThat(first.secondsRemaining()).isBetween(29L, 30L);
        assertThat(first.serverId()).isEqualTo("i-test");

        LoadStatusResponse second = service.start(60);
        assertThat(second.started()).as("a running spike is not stacked").isFalse();
        assertThat(second.active()).isTrue();

        assertThat(service.stop().active()).isFalse();
        assertThat(service.status().active()).isFalse();
    }

    @Test
    void rejectsLengthsOutsideLimits() {
        service = new CpuSpikeLoadGeneratorService(new LoadProperties(true, 30, 600), () -> identity);

        assertThatThrownBy(() -> service.start(29)).isInstanceOf(InvalidLoadRequestException.class);
        assertThatThrownBy(() -> service.start(601)).isInstanceOf(InvalidLoadRequestException.class);
        assertThatThrownBy(() -> service.start(null)).isInstanceOf(InvalidLoadRequestException.class);
    }

    @Test
    void refusesWhenDisabled() {
        service = new CpuSpikeLoadGeneratorService(new LoadProperties(false, 30, 600), () -> identity);

        assertThatThrownBy(() -> service.start(60)).isInstanceOf(LoadGeneratorDisabledException.class);
    }
}
