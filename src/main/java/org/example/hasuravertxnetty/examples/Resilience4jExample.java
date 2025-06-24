package org.example.hasuravertxnetty.examples;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.time.Duration;
import java.util.concurrent.*;

public class Resilience4jExample {
    public static void main(String[] args) throws Exception {
        // 1. CircuitBreaker Example
        demonstrateCircuitBreaker();

        // 2. Retry Example
        demonstrateRetry();

        // 3. RateLimiter Example
        demonstrateRateLimiter();

        // 4. Bulkhead Example
        demonstrateBulkhead();

        // 5. TimeLimiter Example
        demonstrateTimeLimiter();

        // 6. Cache Example
        demonstrateCache();
    }

    // 1. CircuitBreaker: Protects against repeated failures
    private static void demonstrateCircuitBreaker() {
        System.out.println("\n--- CircuitBreaker Example ---");

        // Configure CircuitBreaker to open after 2 failures in a window of 3 calls
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(66) // Open if 2/3 fail (66%)
                .slidingWindowSize(3)     // Track last 3 calls
                .waitDurationInOpenState(Duration.ofMillis(2100)) // Wait 2s before half-open
                .permittedNumberOfCallsInHalfOpenState(2) // Allow 2 calls in half-open
                .build();

        CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(config).circuitBreaker("myBreaker");

        // Simulate 10 calls with random success/failure
        for (int i = 1; i <= 10; i++) {
            System.out.println("Try #" + i);
            try {
                String result = circuitBreaker.executeSupplier(() -> {
                    System.out.println("Executing operation...");
                    if (Math.random() > 0.5) throw new RuntimeException("Failed!");
                    return "Success!";
                });
                System.out.println("Result: " + result);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            try {
                Thread.sleep(1000); // Wait 1 second between calls
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 2. Retry: Automatically retries failed operations
    private static void demonstrateRetry() {
        System.out.println("\n--- Retry Example ---");

        // Configure Retry to attempt up to 3 times with 500ms delay
        int maxAttempts = 3;
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(e -> e instanceof RuntimeException)
                .build();

        Retry retry = Retry.of("myRetry", config);

        // Simulate an operation that might fail
        String result = "";
        try {
            result = retry.executeSupplier(() -> {
            System.out.println("Executing operation...");
            if (Math.random() > 0.3) throw new RuntimeException("Temporary failure");
            return "Success!";
        });
        } catch (RuntimeException e) {
            System.out.println("It definitely failed: " + maxAttempts + " times");
        } finally {
            System.out.println("Final result: " + result);
        }

    }

    // 3. RateLimiter: Limits the rate of operations
    private static void demonstrateRateLimiter() {

        System.out.println("\n--- RateLimiter Example ---");

        // Configure RateLimiter to allow 2 calls per second
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();

        RateLimiter rateLimiter = RateLimiter.of("myRateLimiter", config);

        // Simulate 5 calls
        for (int i = 1; i <= 5; i++) {
            System.out.println("Try #" + i);
            if (rateLimiter.acquirePermission()) {
                System.out.println("Operation permitted");
            } else {
                System.out.println("Operation blocked: Rate limit exceeded");
            }
            try {
                Thread.sleep(250); // Simulate time between calls
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 4. Bulkhead: Limits concurrent operations
    private static void demonstrateBulkhead() {
        System.out.println("\n--- Bulkhead Example ---");

        // Configure Bulkhead to allow 2 concurrent operations
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(2)
                .build();

        Bulkhead bulkhead = Bulkhead.of("myBulkhead", config);

        // Simulate 3 concurrent operations
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    bulkhead.executeRunnable(() -> {
                        System.out.println("Task " + taskId + " started");
                        try {
                            Thread.sleep(2000); // Simulate long-running task
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        System.out.println("Task " + taskId + " completed");
                    });
                } catch (Exception e) {
                    System.out.println("Task " + taskId + " blocked: " + e.getMessage());
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 5. TimeLimiter: Fails operations that take too long
    private static void demonstrateTimeLimiter() throws Exception {
        System.out.println("\n--- TimeLimiter Example ---");

        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .build();

        TimeLimiter timeLimiter = TimeLimiter.of(config);
        try {

            // Use executeFutureSupplier with proper CompletableFuture
            for (int i = 1; i < 3; i++) {
                int finalI = i;
                String result = timeLimiter.executeFutureSupplier(() ->
                        CompletableFuture.supplyAsync(() -> {
                            System.out.println("Starting slow operation... " + finalI);
                            try {
                                Thread.sleep(finalI * 550); // Simulate a delay
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return "Success!";
                        })
                );
                System.out.println("Result: " + result);
            }
        } catch (TimeoutException e) {
            System.out.println("Error: Operation timed out - " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    // 6. Cache: Caches results to avoid redundant operations
    private static void demonstrateCache() {
        System.out.println("\n--- Cache Example ---");

        // Configure a simple Caffeine cache
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();

        // Simulate a database call
        String result1 = cache.get("key1", k -> {
            System.out.println("Executing database operation for " + k);
            return "Value1";
        });
        System.out.println("Result1: " + result1);

        // Retrieve again (should use cache)
        String result2 = cache.get("key1", k -> {
            System.out.println("Executing database operation for " + k);
            return "Value1";
        });
        System.out.println("Result2: " + result2); // Should not execute operation again
    }
}
