package com.manekpay.vaults.repository;

import com.manekpay.vaults.entity.RoundUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoundUpRepository extends JpaRepository<RoundUp, UUID> {
}
