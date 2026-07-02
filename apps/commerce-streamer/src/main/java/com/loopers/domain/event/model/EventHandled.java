package com.loopers.domain.event.model;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 처리 완료된 이벤트 ID 기록. At Least Once로 재수신되는 중복 메시지를 걸러낸다.
 */
@Entity
@Table(name = "event_handled")
@Getter
public class EventHandled extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    protected EventHandled() {}

    private EventHandled(String eventId) {
        this.eventId = eventId;
    }

    public static EventHandled create(String eventId) {
        return new EventHandled(eventId);
    }
}
