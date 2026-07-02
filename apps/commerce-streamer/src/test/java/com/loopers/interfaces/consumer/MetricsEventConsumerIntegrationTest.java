package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loopers.infrastructure.metrics.persistence.ProductMetricsJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class MetricsEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductMetricsJpaRepository productMetricsJpaRepository;

    @DisplayName("LIKE_ADDED 이벤트를 소비하면, product_metrics의 like_count가 증가한다.")
    @Test
    void increasesLikeCount_whenLikeAddedEventIsConsumed() throws Exception {
        // arrange
        long productId = 101L;

        // act
        publish(MetricsEventConsumer.CATALOG_EVENTS, UUID.randomUUID().toString(), "LIKE_ADDED", productPayload(productId));

        // assert
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(productMetricsJpaRepository.findByProductId(productId))
                .hasValueSatisfying(metrics -> assertThat(metrics.getLikeCount()).isEqualTo(1))
        );
    }

    @DisplayName("같은 eventId의 이벤트를 두 번 소비해도, 집계는 한 번만 반영된다. (멱등)")
    @Test
    void appliesMetricOnlyOnce_whenDuplicateEventIsConsumed() throws Exception {
        // arrange
        long productId = 202L;
        String eventId = UUID.randomUUID().toString();

        // act: 동일 eventId 중복 발행 (At Least Once 재전송 상황 재현)
        publish(MetricsEventConsumer.CATALOG_EVENTS, eventId, "LIKE_ADDED", productPayload(productId));
        publish(MetricsEventConsumer.CATALOG_EVENTS, eventId, "LIKE_ADDED", productPayload(productId));

        // assert
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(productMetricsJpaRepository.findByProductId(productId))
                .hasValueSatisfying(metrics -> assertThat(metrics.getLikeCount()).isEqualTo(1))
        );
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(productMetricsJpaRepository.findByProductId(productId))
                .hasValueSatisfying(metrics -> assertThat(metrics.getLikeCount()).isEqualTo(1))
        );
    }

    @DisplayName("ORDER_CREATED 이벤트를 소비하면, 주문 항목 수량만큼 sales_count가 증가한다.")
    @Test
    void increasesSalesCount_whenOrderCreatedEventIsConsumed() throws Exception {
        // arrange
        long productId = 303L;
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("orderId", 900L);
        ObjectNode item = payload.putArray("items").addObject();
        item.put("productId", productId);
        item.put("quantity", 3);

        // act
        publish(MetricsEventConsumer.ORDER_EVENTS, UUID.randomUUID().toString(), "ORDER_CREATED", payload);

        // assert
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(productMetricsJpaRepository.findByProductId(productId))
                .hasValueSatisfying(metrics -> assertThat(metrics.getSalesCount()).isEqualTo(3))
        );
    }

    @DisplayName("PRODUCT_VIEWED 이벤트를 소비하면, view_count가 증가한다.")
    @Test
    void increasesViewCount_whenProductViewedEventIsConsumed() throws Exception {
        // arrange
        long productId = 404L;

        // act
        publish(MetricsEventConsumer.CATALOG_EVENTS, UUID.randomUUID().toString(), "PRODUCT_VIEWED", productPayload(productId));

        // assert
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(productMetricsJpaRepository.findByProductId(productId))
                .hasValueSatisfying(metrics -> assertThat(metrics.getViewCount()).isEqualTo(1))
        );
    }

    private ObjectNode productPayload(long productId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("productId", productId);
        return payload;
    }

    private void publish(String topic, String eventId, String eventType, ObjectNode payload) throws Exception {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("aggregateId", payload.path("productId").asText());
        envelope.set("payload", payload);
        kafkaTemplate.send(topic, envelope.path("aggregateId").asText(), envelope).get(10, TimeUnit.SECONDS);
    }
}
