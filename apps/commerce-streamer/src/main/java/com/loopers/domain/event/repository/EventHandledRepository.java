package com.loopers.domain.event.repository;

import com.loopers.domain.event.model.EventHandled;

public interface EventHandledRepository {
    boolean existsByEventId(String eventId);
    EventHandled save(EventHandled eventHandled);
}
