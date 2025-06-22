package org.example.hasuravertxnetty.config;

import dev.failsafe.RetryPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class FailsafeConfig {

    @Bean
    public RetryPolicy<Object> playerServiceRetryPolicy() {
        return RetryPolicy.builder()
                .handle(Exception.class) // Retry on any exception
                .withMaxRetries(3)      // Up to 3 retries
                .withBackoff(Duration.ofMillis(100), Duration.ofMillis(1000)) // Exponential backoff
                .build();
    }

    @Bean
    public RetryPolicy<Object> battleServiceRetryPolicy() {
        return RetryPolicy.builder()
                .handle(Exception.class)
                .withMaxRetries(3)
                .withBackoff(Duration.ofMillis(100), Duration.ofMillis(1000))
                .build();
    }

    @Bean
    public RetryPolicy<Object> clientMessageRetryPolicy() {
        return RetryPolicy.builder()
                .handle(IOException.class) // Retry on I/O errors
                .withMaxRetries(2)
                .build();
    }

}
