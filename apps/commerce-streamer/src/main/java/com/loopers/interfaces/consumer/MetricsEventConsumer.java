package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.EventEnvelope;
import com.loopers.application.metrics.MetricsApplicationService;
import com.loopers.confg.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class MetricsEventConsumer {

    public static final String CATALOG_EVENTS = "catalog-events";
    public static final String ORDER_EVENTS = "order-events";

    private final MetricsApplicationService metricsApplicationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = {CATALOG_EVENTS, ORDER_EVENTS},
        groupId = "streamer-metrics",
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consume(List<ConsumerRecord<Object, Object>> messages, Acknowledgment acknowledgment) {
        log.debug("metrics 이벤트 배치 수신. size={}", messages.size());
        for (ConsumerRecord<Object, Object> message : messages) {
            EventEnvelope envelope = parse(message);
            if (envelope == null) {
                continue; // 해석 불가한 메시지는 재시도해도 실패하므로 건너뛴다
            }
            metricsApplicationService.handle(envelope);
        }
        // 배치 전체 처리 후 수동 커밋. 중간 실패 시 커밋하지 않고 재수신하며,
        // 이미 처리된 레코드는 event_handled가 걸러낸다.
        acknowledgment.acknowledge();
    }

    private EventEnvelope parse(ConsumerRecord<Object, Object> message) {
        try {
            JsonNode root = objectMapper.readTree((byte[]) message.value());
            String eventId = root.path("eventId").asText();
            if (eventId.isBlank()) {
                log.warn("eventId가 없는 메시지를 건너뜁니다. topic={}, offset={}", message.topic(), message.offset());
                return null;
            }
            return new EventEnvelope(eventId, root.path("eventType").asText(), root.path("payload"));
        } catch (Exception e) {
            log.error("메시지 해석 실패로 건너뜁니다. topic={}, offset={}, cause={}",
                message.topic(), message.offset(), e.toString());
            return null;
        }
    }
}
