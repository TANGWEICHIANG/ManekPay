package com.manekpay.auth.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class InquiryIntegrationTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetAccessToken(String email) throws Exception {
        mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correcthorsebatterystaple", "fullName", "Inquiry Test"))))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correcthorsebatterystaple"))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void createAndFetchInquiry() throws Exception {
        String token = loginAndGetAccessToken("ivan@example.com");

        String createResponse = mockMvc.perform(post("/inquiries").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.verifications").isEmpty())
                .andReturn().getResponse().getContentAsString();

        String inquiryId = objectMapper.readTree(createResponse).get("inquiryId").asText();

        mockMvc.perform(get("/inquiries/" + inquiryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inquiryId").value(inquiryId));
    }

    @Test
    void cannotAccessAnotherCustomersInquiry() throws Exception {
        String ownerToken = loginAndGetAccessToken("julia@example.com");
        String otherToken = loginAndGetAccessToken("kevin@example.com");

        String createResponse = mockMvc.perform(post("/inquiries").header("Authorization", "Bearer " + ownerToken))
                .andReturn().getResponse().getContentAsString();
        String inquiryId = objectMapper.readTree(createResponse).get("inquiryId").asText();

        mockMvc.perform(get("/inquiries/" + inquiryId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownInquiryReturns404() throws Exception {
        String token = loginAndGetAccessToken("laura@example.com");
        mockMvc.perform(get("/inquiries/00000000-0000-0000-0000-000000000000").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
