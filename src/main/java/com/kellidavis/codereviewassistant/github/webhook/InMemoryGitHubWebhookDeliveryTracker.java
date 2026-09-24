package com.kellidavis.codereviewassistant.github.webhook;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class InMemoryGitHubWebhookDeliveryTracker implements GitHubWebhookDeliveryTracker {
    private static final Duration DEFAULT_RETENTION = Duration.ofHours(24);
    private static final int DEFAULT_MAXIMUM_ENTRIES = 10_000;

    private final Clock clock;
    private final Duration retention;
    private final int maximumEntries;
    private final Map<String, TrackedDelivery> trackedDeliveries = new LinkedHashMap<>();

    public InMemoryGitHubWebhookDeliveryTracker() {
        this(Clock.systemUTC(), DEFAULT_RETENTION, DEFAULT_MAXIMUM_ENTRIES);
    }

    InMemoryGitHubWebhookDeliveryTracker(Clock clock, Duration retention, int maximumEntries) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null.");
        this.retention = Objects.requireNonNull(retention, "Retention must not be null.");

        if (retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("Retention must be positive.");
        }

        if (maximumEntries <= 0) {
            throw new IllegalArgumentException("Maximum entries must be positive.");
        }

        this.maximumEntries = maximumEntries;
    }

    @Override
    public synchronized boolean tryReserve(String deliveryId) {
        requireDeliveryId(deliveryId);

        Instant now = clock.instant();
        removeExpiredDeliveries(now);

        if (trackedDeliveries.containsKey(deliveryId)) {
            return false;
        }

        removeOldestDeliveryWhenAtCapacity();
        trackedDeliveries.put(deliveryId, new TrackedDelivery(now.plus(retention), false));
        return true;
    }

    @Override
    public synchronized void markCompleted(String deliveryId) {
        requireDeliveryId(deliveryId);

        removeExpiredDeliveries(clock.instant());

        TrackedDelivery trackedDelivery = trackedDeliveries.get(deliveryId);

        if (trackedDelivery != null) {
            trackedDeliveries.put(deliveryId, trackedDelivery.markCompleted());
        }
    }

    @Override
    public synchronized void release(String deliveryId) {
        requireDeliveryId(deliveryId);
        trackedDeliveries.remove(deliveryId);
    }

    private void removeExpiredDeliveries(Instant now) {
        trackedDeliveries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private void removeOldestDeliveryWhenAtCapacity() {
        if (trackedDeliveries.size() < maximumEntries) {
            return;
        }

        Iterator<Map.Entry<String, TrackedDelivery>> trackedDeliveryEntries =
                trackedDeliveries.entrySet().iterator();

        while (trackedDeliveryEntries.hasNext()) {
            Map.Entry<String, TrackedDelivery> trackedDeliveryEntry = trackedDeliveryEntries.next();

            if (trackedDeliveryEntry.getValue().completed()) {
                trackedDeliveryEntries.remove();
                return;
            }
        }

        Iterator<String> deliveryIds = trackedDeliveries.keySet().iterator();
        deliveryIds.next();
        deliveryIds.remove();
    }

    private void requireDeliveryId(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) {
            throw new IllegalArgumentException("GitHub webhook delivery ID must not be blank.");
        }
    }

    private record TrackedDelivery(Instant expiresAt, boolean completed) {
        private TrackedDelivery markCompleted() {
            return new TrackedDelivery(expiresAt, true);
        }
    }
}
