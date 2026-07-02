package com.loopers.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loopers.domain.like.event.LikeAddedEvent;
import com.loopers.domain.like.event.LikeRemovedEvent;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.outbox.model.OutboxEvent;
import com.loopers.domain.outbox.repository.OutboxEventRepository;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.support.config.KafkaTopicsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 도메인 이벤트를 outbox 테이블에 기록한다.
 * BEFORE_COMMIT: 도메인 트랜잭션과 같은 트랜잭션으로 묶여, 롤백 시 outbox도 함께 롤백된다.
 * fallbackExecution: 트랜잭션이 없는 흐름(상품 조회 등)에서는 자체 트랜잭션으로 즉시 기록한다.
 */
@RequiredArgsConstructor
@Component
public class OutboxEventRecorder {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void record(LikeAddedEvent event) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("productId", event.productId());
        save(KafkaTopicsConfig.CATALOG_EVENTS, String.valueOf(event.productId()), "LIKE_ADDED", payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void record(LikeRemovedEvent event) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("productId", event.productId());
        save(KafkaTopicsConfig.CATALOG_EVENTS, String.valueOf(event.productId()), "LIKE_REMOVED", payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void record(ProductViewedEvent event) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("productId", event.productId());
        save(KafkaTopicsConfig.CATALOG_EVENTS, String.valueOf(event.productId()), "PRODUCT_VIEWED", payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void record(OrderCreatedEvent event) {
        // 카드 정보는 서비스 경계 밖으로 내보내지 않는다
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("orderId", event.orderId());
        ArrayNode items = payload.putArray("items");
        event.items().forEach(item -> {
            ObjectNode node = items.addObject();
            node.put("productId", item.productId());
            node.put("quantity", item.quantity());
        });
        save(KafkaTopicsConfig.ORDER_EVENTS, String.valueOf(event.orderId()), "ORDER_CREATED", payload);
    }

    private void save(String topic, String partitionKey, String eventType, ObjectNode payload) {
        String eventId = UUID.randomUUID().toString();

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("aggregateId", partitionKey);
        envelope.put("occurredAt", ZonedDateTime.now().toString());
        envelope.set("payload", payload);

        outboxEventRepository.save(
            OutboxEvent.create(eventId, topic, partitionKey, eventType, envelope.toString())
        );
    }
}
