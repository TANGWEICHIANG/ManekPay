package com.manekpay.auth.repository;
import com.manekpay.auth.entity.Inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {
}
