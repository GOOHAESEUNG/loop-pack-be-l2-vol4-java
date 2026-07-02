package com.loopers.infrastructure.event.persistence;

import com.loopers.domain.event.model.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, Long> {
    boolean existsByEventId(String eventId);
}
