package com.manekpay.auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kyc03_verifications")
public class Verification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "document_data")
    private byte[] documentData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "declared_data", columnDefinition = "jsonb")
    private String declaredData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_detail", columnDefinition = "jsonb")
    private String resultDetail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Verification() {
    }

    public Verification(UUID inquiryId, VerificationType type, byte[] documentData, String declaredData) {
        this.inquiryId = inquiryId;
        this.type = type;
        this.documentData = documentData;
        this.declaredData = declaredData;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInquiryId() {
        return inquiryId;
    }

    public VerificationType getType() {
        return type;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getDeclaredData() {
        return declaredData;
    }

    public String getResultDetail() {
        return resultDetail;
    }

    public void setResultDetail(String resultDetail) {
        this.resultDetail = resultDetail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
