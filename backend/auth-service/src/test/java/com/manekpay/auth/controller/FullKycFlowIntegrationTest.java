package com.manekpay.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class FullKycFlowIntegrationTest {

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

    @Test
    void fullFlowApprovesCustomer() throws Exception {
        String email = "maria@example.com";
        mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correcthorsebatterystaple", "fullName", "Maria Wong"))))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correcthorsebatterystaple"))))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        String inquiryResponse = mockMvc.perform(post("/inquiries").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String inquiryId = objectMapper.readTree(inquiryResponse).get("inquiryId").asText();

        MockMultipartFile idImage = new MockMultipartFile("image", "id.jpg", "image/jpeg", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/inquiries/" + inquiryId + "/verifications/government-id")
                        .file(idImage)
                        .param("nric", "900101-14-5678")
                        .param("dob", "1990-01-01")
                        .param("nationality", "Malaysian")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PASSED"));

        mockMvc.perform(get("/inquiries/" + inquiryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));

        MockMultipartFile selfieImage = new MockMultipartFile("image", "selfie.jpg", "image/jpeg", new byte[]{4, 5, 6});
        mockMvc.perform(multipart("/inquiries/" + inquiryId + "/verifications/selfie")
                        .file(selfieImage)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PASSED"));

        mockMvc.perform(get("/inquiries/" + inquiryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("APPROVED"));
    }

    @Test
    void failedGovernmentIdDoesNotApproveInquiry() throws Exception {
        String email = "nina@example.com";
        mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correcthorsebatterystaple", "fullName", "Nina Ismail"))))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "correcthorsebatterystaple"))))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        String inquiryResponse = mockMvc.perform(post("/inquiries").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String inquiryId = objectMapper.readTree(inquiryResponse).get("inquiryId").asText();

        MockMultipartFile badIdImage = new MockMultipartFile("image", "id.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/inquiries/" + inquiryId + "/verifications/government-id")
                        .file(badIdImage)
                        .param("nric", "900101-14-567")
                        .param("dob", "1990-01-01")
                        .param("nationality", "Malaysian")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));

        mockMvc.perform(get("/inquiries/" + inquiryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
