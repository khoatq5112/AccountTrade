package com.group3.accounttrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AccountTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountTradeApplication.class, args);
    }

}
