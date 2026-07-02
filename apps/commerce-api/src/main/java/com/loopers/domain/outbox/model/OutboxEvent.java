package com.loopers.domain.outbox.model;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.ZonedDateTime;

/**
 * Transactional Outbox. 도메인 트랜잭션과 같은 트랜잭션으로 기록되고,
 * 릴레이가 Kafka로 발행한 뒤 PUBLISHED로 마킹한다. (At Least Once)
 */
@Entity
@Table(
    name = "outbox_event",
    indexes = @Index(name = "idx_outbox_status_id", columnList = "status, id")
)
@Getter
public class OutboxEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxEventStatus status;

    @Column(name = "published_at")
    private ZonedDateTime publishedAt;

    protected OutboxEvent() {}

    private OutboxEvent(String eventId, String topic, String partitionKey, String eventType, String payload) {
        this.eventId = eventId;
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
    }

    public static OutboxEvent create(String eventId, String topic, String partitionKey, String eventType, String payload) {
        return new OutboxEvent(eventId, topic, partitionKey, eventType, payload);
    }

    public void markPublished() {
        if (this.status != OutboxEventStatus.PUBLISHED) {
            this.status = OutboxEventStatus.PUBLISHED;
            this.publishedAt = ZonedDateTime.now();
        }
    }
}
