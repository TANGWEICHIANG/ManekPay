package com.manekpay.vaults;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class VaultsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VaultsServiceApplication.class, args);
    }
}
