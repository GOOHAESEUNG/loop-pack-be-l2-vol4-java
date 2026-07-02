package com.loopers.application.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.domain.event.model.EventHandled;
import com.loopers.domain.event.repository.EventHandledRepository;
import com.loopers.domain.metrics.repository.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트를 product_metrics에 집계한다.
 * event_handled 기록과 지표 반영을 한 트랜잭션으로 묶어,
 * 커밋 전 장애 시 재수신하면 처음부터 다시 처리하고(유실 없음),
 * 커밋 후 재수신하면 event_id 중복으로 건너뛴다(중복 없음). — Idempotent Consumer
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MetricsApplicationService {

    private final EventHandledRepository eventHandledRepository;
    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void handle(EventEnvelope envelope) {
        if (eventHandledRepository.existsByEventId(envelope.eventId())) {
            log.debug("이미 처리한 이벤트를 건너뜁니다. eventId={}", envelope.eventId());
            return;
        }
        eventHandledRepository.save(EventHandled.create(envelope.eventId()));

        switch (envelope.eventType()) {
            case "LIKE_ADDED" -> productMetricsRepository.applyDelta(productId(envelope), 1, 0, 0);
            case "LIKE_REMOVED" -> productMetricsRepository.applyDelta(productId(envelope), -1, 0, 0);
            case "PRODUCT_VIEWED" -> productMetricsRepository.applyDelta(productId(envelope), 0, 0, 1);
            case "ORDER_CREATED" -> envelope.payload().path("items").forEach(item ->
                productMetricsRepository.applyDelta(
                    item.path("productId").asLong(), 0, item.path("quantity").asLong(), 0));
            default -> log.debug("집계 대상이 아닌 이벤트 타입입니다. eventType={}", envelope.eventType());
        }
    }

    private Long productId(EventEnvelope envelope) {
        JsonNode node = envelope.payload().path("productId");
        return node.asLong();
    }
}
