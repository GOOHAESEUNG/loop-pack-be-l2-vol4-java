package com.loopers.application.metrics;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * commerce-api가 outbox로 발행하는 이벤트 봉투.
 * { eventId, eventType, aggregateId, occurredAt, payload }
 */
public record EventEnvelope(
    String eventId,
    String eventType,
    JsonNode payload
) {
}
