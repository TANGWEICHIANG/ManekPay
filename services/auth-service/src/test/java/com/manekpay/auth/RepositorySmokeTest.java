package com.manekpay.auth;

import com.manekpay.auth.customer.Customer;
import com.manekpay.auth.customer.CustomerRepository;
import com.manekpay.auth.identity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RepositorySmokeTest {

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
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private VerificationRepository verificationRepository;

    @Test
    void savesAndLoadsCustomerInquiryAndVerification() {
        Customer customer = customerRepository.save(new Customer("test@example.com", "hash", "Test User"));
        assertThat(customer.getId()).isNotNull();

        Optional<Customer> found = customerRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getKycStatus()).isEqualTo(com.manekpay.auth.customer.KycStatus.PENDING);

        Inquiry inquiry = inquiryRepository.save(new Inquiry(customer.getId()));
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.CREATED);

        Verification verification = verificationRepository.save(
                new Verification(inquiry.getId(), VerificationType.GOVERNMENT_ID, new byte[]{1, 2, 3}, "{\"nric\":\"900101-01-1234\"}")
        );
        assertThat(verification.getId()).isNotNull();

        List<Verification> byInquiry = verificationRepository.findByInquiryId(inquiry.getId());
        assertThat(byInquiry).hasSize(1);
    }
}
