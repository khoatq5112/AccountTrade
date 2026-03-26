package com.group3.accounttrade.service.notification;

import com.group3.accounttrade.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Publisher for notification events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Async
    public void publish(NotificationEvent event) {
        log.info("Publishing notification event: {}", event.getEventType());
        eventPublisher.publishEvent(event);
    }
}
