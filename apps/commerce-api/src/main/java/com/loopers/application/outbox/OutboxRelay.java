package com.loopers.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.model.OutboxEvent;
import com.loopers.domain.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * outbox의 PENDING 이벤트를 Kafka로 발행한다.
 * 발행 실패 시 PENDING으로 남아 다음 주기에 재시도된다. (At Least Once — Consumer 멱등 처리 전제)
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:1000}")
    @Transactional
    public void relayPending() {
        List<OutboxEvent> pendings = outboxEventRepository.findPending(BATCH_SIZE);
        for (OutboxEvent event : pendings) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), objectMapper.readTree(event.getPayload()))
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception e) {
                // 같은 파티션 키의 순서 보장을 위해 이번 주기는 중단하고 다음 주기에 처음부터 재시도한다
                log.warn("outbox 발행 실패. eventId={}, topic={}, cause={}",
                    event.getEventId(), event.getTopic(), e.toString());
                break;
            }
        }
    }
}
