package com.manekpay.auth.repository;

import com.manekpay.auth.entity.Verification;
import com.manekpay.auth.entity.VerificationStatus;
import com.manekpay.auth.entity.VerificationType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VerificationRepository extends JpaRepository<Verification, UUID> {
    List<Verification> findByInquiryId(UUID inquiryId);

    @Query("SELECT v FROM Verification v JOIN Inquiry i ON v.inquiryId = i.id " +
            "WHERE i.customerId = :customerId AND v.type = :type AND v.status = :status " +
            "ORDER BY v.createdAt DESC")
    List<Verification> findByCustomerIdAndTypeAndStatusOrderByCreatedAtDesc(
            @Param("customerId") UUID customerId,
            @Param("type") VerificationType type,
            @Param("status") VerificationStatus status);
}
