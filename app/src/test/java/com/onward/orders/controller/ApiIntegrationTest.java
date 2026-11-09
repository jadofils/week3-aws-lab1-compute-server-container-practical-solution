package com.onward.orders.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the HTTP contract used by the browser UI and the load balancer health check.
 * EC2 metadata is switched off so the tests behave the same on any machine.
 */
@SpringBootTest(properties = "app.identity.ec2-metadata-enabled=false")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthReportsUpWithServerIdentity() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.server").isNotEmpty())
                .andExpect(jsonPath("$.availabilityZone").value("local"));
    }

    @Test
    void infoShowsStudentLabAndAnsweringServer() throws Exception {
        mvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Jado Fils SEZIKEYE"))
                .andExpect(jsonPath("$.labName").value("Lab 1 - Handling Sales Day Demand"))
                .andExpect(jsonPath("$.serverId").isNotEmpty())
                .andExpect(jsonPath("$.availabilityZone").isNotEmpty())
                .andExpect(jsonPath("$.privateIp").isNotEmpty());
    }

    @Test
    void createsAndListsOrder() throws Exception {
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\" Ana \",\"item\":\"Parcel\",\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Ana"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.servedBy").value(containsString(" · local")));

        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("Ana"));
    }

    @Test
    void rejectsInvalidOrderWithProblemDetail() throws Exception {
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Ana\",\"item\":\"Parcel\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Quantity must be between 1 and 100."));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request body must be valid JSON."));
    }

    @Test
    void rejectsSpikeOutsideAllowedLength() throws Exception {
        mvc.perform(post("/api/load/spike").contentType(MediaType.APPLICATION_JSON).content("{\"seconds\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Spike length must be between 30 and 600 seconds."));
        mvc.perform(post("/api/load/spike").contentType(MediaType.APPLICATION_JSON).content("{\"seconds\":3600}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loadStatusIsIdleByDefault() throws Exception {
        mvc.perform(get("/api/load/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.workerThreads").isNumber());
    }

    @Test
    void servesUserInterface() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sales-day CPU spike")));
    }
}
