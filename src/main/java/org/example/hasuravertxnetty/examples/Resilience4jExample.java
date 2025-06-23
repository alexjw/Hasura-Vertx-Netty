package org.example.hasuravertxnetty.examples;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.Duration;

public class Resilience4jExample {
    public static void main(String[] args) throws InterruptedException {
        // Rules: Take a break if 2 out of 3 tries fail
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(66) // If 2/3 fail (66%), stop
                .slidingWindowSize(3)     // Check 3 tries
                .waitDurationInOpenState(Duration.ofMillis(1100)) // Wait 1.1 seconds
                .build();

        // Make a Circuit Breaker with these rules
        CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(config)
                .circuitBreaker("myBreaker");

        // Try 10 times
        for (int i = 1; i <= 10; i++) {
            System.out.println("Try #" + i);
            try {
                String result = circuitBreaker.executeSupplier(() -> {
                    // Randomly fail half the time
                    if (Math.random() > 0.5) {
                        throw new RuntimeException("Failed!");
                    }
                    return "Success!";
                });
                System.out.println("Got: " + result);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            Thread.sleep(1000); // Wait 1 second between tries
        }
    }
}
