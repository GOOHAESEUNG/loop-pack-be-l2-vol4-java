package com.loopers.application.coupon;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.application.metrics.EventEnvelope;
import com.loopers.domain.coupon.model.CouponIssueRequest;
import com.loopers.domain.coupon.model.IssuedCoupon;
import com.loopers.domain.coupon.repository.CouponIssueRepository;
import com.loopers.domain.event.model.EventHandled;
import com.loopers.domain.event.repository.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선착순 쿠폰 발급 처리.
 * 같은 couponTemplateId는 같은 파티션에서 순차 처리되므로 경합이 최소화되고,
 * 수량 선점은 원자적 UPDATE(deductQuota)가 최종 방어선이 된다.
 * requestId가 envelope의 eventId이므로 event_handled가 요청 단위 멱등을 보장한다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CouponIssueApplicationService {

    private final EventHandledRepository eventHandledRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    public void handle(EventEnvelope envelope) {
        if (eventHandledRepository.existsByEventId(envelope.eventId())) {
            log.debug("이미 처리한 발급 요청을 건너뜁니다. eventId={}", envelope.eventId());
            return;
        }
        eventHandledRepository.save(EventHandled.create(envelope.eventId()));

        JsonNode payload = envelope.payload();
        String requestId = payload.path("requestId").asText();
        long memberId = payload.path("memberId").asLong();
        long couponTemplateId = payload.path("couponTemplateId").asLong();

        CouponIssueRequest request = couponIssueRepository.findRequestByRequestId(requestId).orElse(null);
        if (request == null) {
            log.warn("발급 요청 레코드가 없어 건너뜁니다. requestId={}", requestId);
            return;
        }
        if (request.isCompleted()) {
            return;
        }

        if (couponIssueRepository.existsIssuedCoupon(memberId, couponTemplateId)) {
            request.fail("이미 발급받은 쿠폰입니다.");
            return;
        }

        if (couponIssueRepository.deductQuota(couponTemplateId) == 0) {
            request.fail("쿠폰이 모두 소진되었습니다.");
            return;
        }

        couponIssueRepository.saveIssuedCoupon(IssuedCoupon.create(memberId, couponTemplateId));
        request.complete();
    }
}
