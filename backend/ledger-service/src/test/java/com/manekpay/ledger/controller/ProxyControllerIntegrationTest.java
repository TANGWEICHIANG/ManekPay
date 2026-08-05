package com.manekpay.ledger.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProxyControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("manekpay")
            .withUsername("manekpay")
            .withPassword("manekpay");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void linksListsAndDeletesAProxy() throws Exception {
        String subject = "22222222-2222-2222-2222-222222222222";
        String body = mockMvc.perform(post("/accounts/me/proxies").with(jwt().jwt(j -> j.subject(subject)))
                        .contentType("application/json")
                        .content("{\"type\":\"MOBILE\",\"value\":\"0198765432\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proxyId").exists())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/accounts/me/proxies").with(jwt().jwt(j -> j.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxies[0].value").value("0198765432"));

        String proxyId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("proxyId").asText();
        mockMvc.perform(delete("/accounts/me/proxies/" + proxyId).with(jwt().jwt(j -> j.subject(subject))))
                .andExpect(status().isNoContent());
    }

    @Test
    void linkingTheSameMobileNumberTwiceIsRejected() throws Exception {
        mockMvc.perform(post("/accounts/me/proxies").with(jwt().jwt(j -> j.subject("33333333-3333-3333-3333-333333333333")))
                        .contentType("application/json")
                        .content("{\"type\":\"MOBILE\",\"value\":\"0111222333\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/me/proxies").with(jwt().jwt(j -> j.subject("44444444-4444-4444-4444-444444444444")))
                        .contentType("application/json")
                        .content("{\"type\":\"MOBILE\",\"value\":\"0111222333\"}"))
                .andExpect(status().isConflict());
    }
}
