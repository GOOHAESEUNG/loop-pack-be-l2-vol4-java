package com.loopers.infrastructure.outbox.persistence;

import com.loopers.domain.outbox.model.OutboxEvent;
import com.loopers.domain.outbox.model.OutboxEventStatus;
import com.loopers.domain.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return outboxEventJpaRepository.save(event);
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return outboxEventJpaRepository.findByStatusOrderByIdAsc(OutboxEventStatus.PENDING, PageRequest.of(0, limit));
    }
}
