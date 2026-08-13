package com.manekpay.vaults.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class GoalControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("manekpay")
            .withUsername("manekpay")
            .withPassword("manekpay");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsListsAndPatchesAGoal() throws Exception {
        String subject = UUID.randomUUID().toString();
        String createBody = """
                {"name":"Emergency Fund","currency":"MYR","targetAmount":"5000.00","sweepAmount":"50.00","sweepFrequency":"WEEKLY"}
                """;

        var createResult = mockMvc.perform(post("/goals").with(jwt().jwt(j -> j.subject(subject)))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Emergency Fund"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.sweepActive").value(true))
                .andReturn();

        String goalId = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/goals").with(jwt().jwt(j -> j.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Emergency Fund"));

        mockMvc.perform(patch("/goals/" + goalId).with(jwt().jwt(j -> j.subject(subject)))
                        .contentType("application/json").content("{\"sweepActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sweepActive").value(false));
    }

    @Test
    void patchingAGoalThatBelongsToAnotherCustomerReturns404() throws Exception {
        String owner = UUID.randomUUID().toString();
        String createBody = """
                {"name":"Vacation","currency":"MYR","targetAmount":"1000.00","sweepAmount":"20.00","sweepFrequency":"DAILY"}
                """;
        var createResult = mockMvc.perform(post("/goals").with(jwt().jwt(j -> j.subject(owner)))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        String goalId = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        String intruder = UUID.randomUUID().toString();
        mockMvc.perform(patch("/goals/" + goalId).with(jwt().jwt(j -> j.subject(intruder)))
                        .contentType("application/json").content("{\"sweepActive\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingAGoalWithABlankNameReturns400() throws Exception {
        String subject = UUID.randomUUID().toString();
        String createBody = """
                {"name":"","currency":"MYR","targetAmount":"1000.00","sweepAmount":"20.00","sweepFrequency":"DAILY"}
                """;

        mockMvc.perform(post("/goals").with(jwt().jwt(j -> j.subject(subject)))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creatingADuplicateGoalNameForTheSameCustomerReturns409() throws Exception {
        String subject = UUID.randomUUID().toString();
        String createBody = """
                {"name":"House Deposit","currency":"MYR","targetAmount":"1000.00","sweepAmount":"20.00","sweepFrequency":"DAILY"}
                """;

        mockMvc.perform(post("/goals").with(jwt().jwt(j -> j.subject(subject)))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/goals").with(jwt().jwt(j -> j.subject(subject)))
                        .contentType("application/json").content(createBody))
                .andExpect(status().isConflict());
    }
}
