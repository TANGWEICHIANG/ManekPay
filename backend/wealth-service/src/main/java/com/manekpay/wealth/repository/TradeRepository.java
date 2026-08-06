package com.manekpay.wealth.repository;

import com.manekpay.wealth.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
}
