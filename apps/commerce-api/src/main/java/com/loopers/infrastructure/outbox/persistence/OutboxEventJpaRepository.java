package com.loopers.infrastructure.outbox.persistence;

import com.loopers.domain.outbox.model.OutboxEvent;
import com.loopers.domain.outbox.model.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusOrderByIdAsc(OutboxEventStatus status, Pageable pageable);
}
