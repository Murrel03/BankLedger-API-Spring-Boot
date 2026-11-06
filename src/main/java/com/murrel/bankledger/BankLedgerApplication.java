package com.murrel.bankledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. {@code @SpringBootApplication} enables component scanning,
 * auto-configuration and configuration-properties binding in one annotation.
 */
@SpringBootApplication
public class BankLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankLedgerApplication.class, args);
    }
}
