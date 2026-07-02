package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loopers.domain.coupon.model.CouponIssueRequest;
import com.loopers.domain.coupon.model.CouponTemplate;
import com.loopers.infrastructure.coupon.persistence.CouponIssueRequestJpaRepository;
import com.loopers.infrastructure.coupon.persistence.CouponTemplateJpaRepository;
import com.loopers.infrastructure.coupon.persistence.IssuedCouponJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class CouponIssueConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponTemplateJpaRepository couponTemplateJpaRepository;

    @Autowired
    private CouponIssueRequestJpaRepository couponIssueRequestJpaRepository;

    @Autowired
    private IssuedCouponJpaRepository issuedCouponJpaRepository;

    @DisplayName("발급 요청을 소비하면, 쿠폰이 발급되고 요청이 SUCCESS로 확정된다.")
    @Test
    void issuesCoupon_whenRequestIsConsumed() throws Exception {
        // arrange
        CouponTemplate template = couponTemplateJpaRepository.save(CouponTemplate.create(10L));
        String requestId = saveRequestAndPublish(1L, template.getId());

        // assert
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            CouponIssueRequest request = couponIssueRequestJpaRepository.findByRequestId(requestId).orElseThrow();
            assertThat(request.getStatus()).isEqualTo("SUCCESS");
        });
        assertThat(issuedCouponJpaRepository.existsByMemberIdAndCouponTemplateId(1L, template.getId())).isTrue();
        assertThat(couponTemplateJpaRepository.findById(template.getId()).orElseThrow().getIssuedQuantity()).isEqualTo(1);
    }

    @DisplayName("이미 발급받은 회원의 재요청은 FAILED로 확정되고, 추가 발급되지 않는다.")
    @Test
    void failsRequest_whenMemberAlreadyHasCoupon() throws Exception {
        // arrange
        CouponTemplate template = couponTemplateJpaRepository.save(CouponTemplate.create(10L));
        long memberId = 2L;

        // act: 같은 회원이 두 번 요청 (서로 다른 requestId)
        String firstRequestId = saveRequestAndPublish(memberId, template.getId());
        String secondRequestId = saveRequestAndPublish(memberId, template.getId());

        // assert
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(status(firstRequestId)).isEqualTo("SUCCESS");
            assertThat(status(secondRequestId)).isEqualTo("FAILED");
        });
        assertThat(couponTemplateJpaRepository.findById(template.getId()).orElseThrow().getIssuedQuantity()).isEqualTo(1);
    }

    @DisplayName("같은 requestId의 메시지를 두 번 소비해도, 발급은 한 번만 수행된다. (멱등)")
    @Test
    void issuesOnlyOnce_whenDuplicateRequestMessageIsConsumed() throws Exception {
        // arrange
        CouponTemplate template = couponTemplateJpaRepository.save(CouponTemplate.create(10L));
        long memberId = 3L;
        String requestId = UUID.randomUUID().toString();
        couponIssueRequestJpaRepository.save(CouponIssueRequest.create(requestId, memberId, template.getId()));

        // act: At Least Once 재전송 상황 재현
        publish(requestId, memberId, template.getId());
        publish(requestId, memberId, template.getId());

        // assert
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
            assertThat(status(requestId)).isEqualTo("SUCCESS")
        );
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(couponTemplateJpaRepository.findById(template.getId()).orElseThrow().getIssuedQuantity()).isEqualTo(1)
        );
    }

    @DisplayName("100장 한정 쿠폰에 300명이 요청해도, 정확히 100건만 발급된다.")
    @Test
    void neverIssuesMoreThanQuota_underMassRequests() throws Exception {
        // arrange
        long totalQuantity = 100L;
        int requestCount = 300;
        CouponTemplate template = couponTemplateJpaRepository.save(CouponTemplate.create(totalQuantity));

        List<String> requestIds = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            long memberId = 10_000L + i;
            String requestId = UUID.randomUUID().toString();
            couponIssueRequestJpaRepository.save(CouponIssueRequest.create(requestId, memberId, template.getId()));
            requestIds.add(requestId);
        }

        // act
        for (int i = 0; i < requestCount; i++) {
            publish(requestIds.get(i), 10_000L + i, template.getId());
        }

        // assert
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            List<CouponIssueRequest> requests = couponIssueRequestJpaRepository.findAll().stream()
                .filter(request -> request.getCouponTemplateId().equals(template.getId()))
                .toList();
            assertThat(requests).hasSize(requestCount);
            assertThat(requests).noneMatch(request -> "PENDING".equals(request.getStatus()));

            long successCount = requests.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
            long failedCount = requests.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
            assertThat(successCount).isEqualTo(totalQuantity);
            assertThat(failedCount).isEqualTo(requestCount - totalQuantity);
        });
        assertThat(couponTemplateJpaRepository.findById(template.getId()).orElseThrow().getIssuedQuantity())
            .isEqualTo(totalQuantity);
    }

    private String status(String requestId) {
        return couponIssueRequestJpaRepository.findByRequestId(requestId).orElseThrow().getStatus();
    }

    private String saveRequestAndPublish(long memberId, Long couponTemplateId) throws Exception {
        String requestId = UUID.randomUUID().toString();
        couponIssueRequestJpaRepository.save(CouponIssueRequest.create(requestId, memberId, couponTemplateId));
        publish(requestId, memberId, couponTemplateId);
        return requestId;
    }

    private void publish(String requestId, long memberId, Long couponTemplateId) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("requestId", requestId);
        payload.put("memberId", memberId);
        payload.put("couponTemplateId", couponTemplateId);

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("eventId", requestId); // requestId가 멱등 키
        envelope.put("eventType", "COUPON_ISSUE_REQUESTED");
        envelope.put("aggregateId", String.valueOf(couponTemplateId));
        envelope.set("payload", payload);

        kafkaTemplate.send(CouponIssueConsumer.COUPON_ISSUE_REQUESTS, String.valueOf(couponTemplateId), envelope)
            .get(10, TimeUnit.SECONDS);
    }
}
