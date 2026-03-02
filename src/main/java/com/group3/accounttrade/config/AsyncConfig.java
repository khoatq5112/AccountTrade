package com.group3.accounttrade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 2 threads always alive
        executor.setMaxPoolSize(10); // scale up to 10 under load
        executor.setQueueCapacity(50); // queue up to 50 pending emails
        executor.setThreadNamePrefix("email-async-");
        executor.initialize();
        return executor;
    }
}
