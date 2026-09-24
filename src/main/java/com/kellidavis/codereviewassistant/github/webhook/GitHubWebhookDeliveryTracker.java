package com.kellidavis.codereviewassistant.github.webhook;

public interface GitHubWebhookDeliveryTracker {
    boolean tryReserve(String deliveryId);

    void markCompleted(String deliveryId);

    void release(String deliveryId);
}
