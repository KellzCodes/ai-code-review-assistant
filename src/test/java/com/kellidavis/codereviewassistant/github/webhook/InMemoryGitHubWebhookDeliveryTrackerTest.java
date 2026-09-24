package com.kellidavis.codereviewassistant.github.webhook;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InMemoryGitHubWebhookDeliveryTrackerTest {
    private static final Instant INITIAL_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void tryReserve_withNewDeliveryId_returnsTrue() {
        InMemoryGitHubWebhookDeliveryTracker tracker = newTracker();

        assertThat(tracker.tryReserve("delivery-1")).isTrue();
    }

    @Test
    void tryReserve_withReservedDeliveryId_returnsFalse() {
        InMemoryGitHubWebhookDeliveryTracker tracker = newTracker();
        tracker.tryReserve("delivery-1");

        assertThat(tracker.tryReserve("delivery-1")).isFalse();
    }

    @Test
    void tryReserve_withCompletedDeliveryId_returnsFalse() {
        InMemoryGitHubWebhookDeliveryTracker tracker = newTracker();
        tracker.tryReserve("delivery-1");
        tracker.markCompleted("delivery-1");

        assertThat(tracker.tryReserve("delivery-1")).isFalse();
    }

    @Test
    void release_withReservedDeliveryId_allowsRetry() {
        InMemoryGitHubWebhookDeliveryTracker tracker = newTracker();
        tracker.tryReserve("delivery-1");

        tracker.release("delivery-1");

        assertThat(tracker.tryReserve("delivery-1")).isTrue();
    }

    @Test
    void tryReserve_withExpiredDeliveryId_allowsRetry() {
        MutableClock clock = new MutableClock(INITIAL_TIME);
        InMemoryGitHubWebhookDeliveryTracker tracker = new InMemoryGitHubWebhookDeliveryTracker(
                clock,
                Duration.ofMinutes(5),
                10);
        tracker.tryReserve("delivery-1");
        tracker.markCompleted("delivery-1");

        clock.advance(Duration.ofMinutes(5));

        assertThat(tracker.tryReserve("delivery-1")).isTrue();
    }

    @Test
    void tryReserve_whenTrackerIsAtCapacity_evictsCompletedDeliveryBeforeActiveReservation() {
        MutableClock clock = new MutableClock(INITIAL_TIME);
        InMemoryGitHubWebhookDeliveryTracker tracker = new InMemoryGitHubWebhookDeliveryTracker(
                clock,
                Duration.ofHours(1),
                2);
        tracker.tryReserve("delivery-1");
        clock.advance(Duration.ofSeconds(1));
        tracker.tryReserve("delivery-2");
        tracker.markCompleted("delivery-2");
        clock.advance(Duration.ofSeconds(1));

        assertThat(tracker.tryReserve("delivery-3")).isTrue();
        assertThat(tracker.tryReserve("delivery-1")).isFalse();
        assertThat(tracker.tryReserve("delivery-2")).isTrue();
    }

    @Test
    void tryReserve_withBlankDeliveryId_throwsException() {
        InMemoryGitHubWebhookDeliveryTracker tracker = newTracker();

        assertThatIllegalArgumentException().isThrownBy(() -> tracker.tryReserve("  "))
                .withMessage("GitHub webhook delivery ID must not be blank.");
    }

    private InMemoryGitHubWebhookDeliveryTracker newTracker() {
        return new InMemoryGitHubWebhookDeliveryTracker(
                new MutableClock(INITIAL_TIME),
                Duration.ofHours(1),
                10);
    }

    private static class MutableClock extends Clock {
        private Instant currentTime;

        private MutableClock(Instant currentTime) {
            this.currentTime = currentTime;
        }

        void advance(Duration duration) {
            currentTime = currentTime.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(currentTime, zone);
        }

        @Override
        public Instant instant() {
            return currentTime;
        }
    }
}
