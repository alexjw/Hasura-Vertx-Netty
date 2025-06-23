package org.example.hasuravertxnetty.config;

import dev.failsafe.CircuitBreaker;
import dev.failsafe.RetryPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

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

    /**
     * CircuitBreaker for network operations (e.g., client messages).
     * Opens after 3 failures, waits 5s, and requires 1 success to close.
     */
    @Bean
    public CircuitBreaker<Object> networkCircuitBreaker() {
        return CircuitBreaker.builder()
                .withFailureThreshold(3, 5) // Open after 3 failures in a window of 5 calls
                .withSuccessThreshold(1)    // Close after 1 success in half-open state
                .withDelay(Duration.ofSeconds(5)) // Delay before half-open state
                .onOpen(e -> System.out.println("CircuitBreaker opened for network"))
                .onClose(e -> System.out.println("CircuitBreaker closed for network"))
                .build();
    }

    // Examples:
    /**
     * Retry policy for database operations with exponential backoff.
     * Suitable for transient database issues like deadlocks or timeouts.
     */
    @Bean
    public RetryPolicy<Object> databaseRetryPolicy() {
        return RetryPolicy.builder()
                // Handle specific database-related exceptions
                .handle(SQLException.class)
                .handle(TimeoutException.class)
                // Limit to 5 retries
                .withMaxRetries(5)
                // Exponential backoff: starts at 100ms, maxes at 2s
                .withBackoff(Duration.ofMillis(100), Duration.ofSeconds(2))
                // Log each retry attempt (using getLastException() instead of getLastFailure())
                .onRetry(e -> System.out.println("Database retry #" + e.getAttemptCount() + " due to: " + e.getLastException()))
                // Abort retries if a specific SQLException message indicates a permanent failure
                .abortWhen((Predicate<Throwable>) e -> e instanceof SQLException &&
                        e.getMessage().contains("connection refused"))
                .build();
    }

    /**
     * Retry policy for flaky network operations with fixed delay and result-based retry.
     * Ideal for socket communications or HTTP requests.
     */
    @Bean
    public RetryPolicy<Object> networkRetryPolicy() {
        return RetryPolicy.builder()
                // Handle network-specific exceptions
                .handle(IOException.class)
                // Retry up to 3 times
                .withMaxRetries(3)
                // Fixed delay of 500ms between retries
                .withDelay(Duration.ofMillis(500))
                // Log failures for monitoring (using getLastException())
                .onFailedAttempt(e -> System.err.println("Network attempt #" + e.getAttemptCount() + " failed: " + e.getLastException()))
                // Stop retrying if total duration exceeds 2 seconds
                .withMaxDuration(Duration.ofSeconds(2))
                .build();
    }

    /**
     * Strict retry policy for critical operations with linear backoff and event listeners.
     * Designed for operations like server startup where failure is costly.
     */
    @Bean
    public RetryPolicy<Object> criticalOperationRetryPolicy() {
        return RetryPolicy.builder()
                // Retry on any exception
                .handle(Exception.class)
                // Retry up to 10 times
                .withMaxRetries(10)
                // Linear backoff: starts at 1s, maxes at 10s (no third step in 3.3.2)
                .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(10))
                // Add listeners for detailed control and logging (using getLastException())
                .onSuccess(e -> System.out.println("Critical operation succeeded after " + e.getAttemptCount() + " attempts"))
                .onFailure(e -> System.err.println("Critical operation failed after " + e.getAttemptCount() + " attempts: " + e.getException()))
                .onAbort(e -> System.out.println("Critical operation aborted due to: " + e.getException()))
                .build();
    }

    /**
     * Advanced retry policy with conditional retries based on exceptions.
     * Useful for operations where specific exceptions dictate retry behavior.
     * Note: retryIf/result-based retries are not supported in 3.3.2; using exception conditions instead.
     */
    @Bean
    public RetryPolicy<Object> conditionalRetryPolicy() {
        return RetryPolicy.builder()
                // Handle multiple exception types with custom conditions
                .handleIf(e -> e instanceof SQLException || (e instanceof IOException && e.getMessage().contains("timeout")))
                // Retry up to 4 times (result-based retryIf not supported, using exception-based logic)
                .withMaxRetries(4)
                // Fixed delay with jitter for variability (jitter as a percentage 0-1)
                .withDelay(Duration.ofMillis(200))
                .withJitter(0.25) // 25% jitter
                .build();
    }

    /**
     * Retry policy with time-based constraints and detailed event handling.
     * Suitable for operations where total execution time is critical.
     */
    @Bean
    public RetryPolicy<Object> timeBoundRetryPolicy() {
        return RetryPolicy.builder()
                // Handle specific exceptions
                .handle(TimeoutException.class)
                // Maximum 3 retries
                .withMaxRetries(3)
                // Exponential backoff: starts at 50ms, maxes at 1s
                .withBackoff(Duration.ofMillis(50), Duration.ofSeconds(1))
                // Limit total duration to 5 seconds
                .withMaxDuration(Duration.ofSeconds(5))
                // Log each attempt with detailed context (using getLastException())
                .onRetry(e -> System.out.printf("Retry #%d: Failed with %s after %s%n",
                        e.getAttemptCount(), e.getLastException(), e.getElapsedTime()))
                .build();
    }

}
