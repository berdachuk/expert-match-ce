package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for async ingestion API (POST /ingestion/database/async, GET /ingestion/progress, POST /ingestion/cancel).
 * When external database ingestion is disabled (default), POST async returns 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IngestionAsyncControllerIT extends BaseIntegrationTest {

    private static final MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void startIngestionFromDatabaseAsync_whenExternalDbDisabled_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/database/async")
                        .param("batchSize", "1000")
                        .param("clear", "false")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("External database ingestion is not enabled"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getIngestionProgress_unknownJobId_returns404() throws Exception {
        String jobId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/ingestion/progress/" + jobId).accept(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelIngestionJob_unknownJobId_returns404() throws Exception {
        String jobId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/ingestion/cancel/" + jobId).accept(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listIngestionJobs_returns200WithJobsArray() throws Exception {
        mockMvc.perform(get("/api/v1/ingestion/jobs").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").isArray());
    }
}
