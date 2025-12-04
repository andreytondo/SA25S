package com.example.sa25s.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class RateLimiterService {

    private static class Counter {
        int attempts;
        Instant windowStart;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Duration window = Duration.ofMinutes(5);
    private final int maxAttempts = 5;

    public boolean isAllowed(String key) {
        Counter counter = counters.computeIfAbsent(key, k -> {
            Counter c = new Counter();
            c.windowStart = Instant.now();
            return c;
        });

        synchronized (counter) {
            Instant now = Instant.now();
            if (Duration.between(counter.windowStart, now).compareTo(window) > 0) {
                counter.windowStart = now;
                counter.attempts = 0;
            }
            return counter.attempts < maxAttempts;
        }
    }

    public void recordFailure(String key) {
        Counter counter = counters.computeIfAbsent(key, k -> {
            Counter c = new Counter();
            c.windowStart = Instant.now();
            return c;
        });
        synchronized (counter) {
            counter.attempts++;
        }
    }

    public void reset(String key) {
        counters.remove(key);
    }
}
