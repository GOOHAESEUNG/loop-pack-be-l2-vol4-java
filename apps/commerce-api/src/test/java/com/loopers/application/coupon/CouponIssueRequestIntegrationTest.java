package com.loopers.application.coupon;

import com.loopers.domain.coupon.model.CouponIssueRequestStatus;
import com.loopers.domain.coupon.model.CouponTemplate;
import com.loopers.domain.coupon.model.CouponType;
import com.loopers.domain.coupon.repository.CouponTemplateRepository;
import com.loopers.domain.member.model.Member;
import com.loopers.domain.member.service.MemberService;
import com.loopers.domain.outbox.model.OutboxEvent;
import com.loopers.infrastructure.outbox.persistence.OutboxEventJpaRepository;
import com.loopers.support.config.KafkaTopicsConfig;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CouponIssueRequestIntegrationTest {

    @Autowired
    private CouponApplicationService couponApplicationService;

    @Autowired
    private CouponTemplateRepository couponTemplateRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private CouponTemplate template;
    private Member member;

    @BeforeEach
    void setUp() {
        template = couponTemplateRepository.save(CouponTemplate.create(
            "선착순 쿠폰", CouponType.FIXED, 1_000L, null, ZonedDateTime.now().plusDays(7), 100L
        ));
        member = memberService.register(
            "couponReqUser", "Password1!", "유저", "1990-01-01", "couponreq@test.com"
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("발급을 요청하면, PENDING 요청과 outbox 이벤트가 같은 트랜잭션으로 기록된다.")
    @Test
    void recordsRequestAndOutboxEvent_whenIssueIsRequested() {
        // act
        String requestId = couponApplicationService.requestCouponIssue("couponReqUser", template.getId());

        // assert: 요청 레코드
        CouponIssueRequestInfo info = couponApplicationService.getIssueRequest("couponReqUser", requestId);
        assertThat(info.status()).isEqualTo(CouponIssueRequestStatus.PENDING);

        // assert: outbox 기록 (eventId=requestId, key=couponTemplateId)
        List<OutboxEvent> events = outboxEventJpaRepository.findAll().stream()
            .filter(event -> event.getEventType().equals("COUPON_ISSUE_REQUESTED"))
            .toList();
        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getEventId()).isEqualTo(requestId);
        assertThat(event.getTopic()).isEqualTo(KafkaTopicsConfig.COUPON_ISSUE_REQUESTS);
        assertThat(event.getPartitionKey()).isEqualTo(String.valueOf(template.getId()));
        assertThat(event.getPayload()).contains("\"requestId\":\"" + requestId + "\"");
    }

    @DisplayName("다른 회원의 발급 요청은 조회할 수 없다.")
    @Test
    void throwsForbidden_whenRequestBelongsToAnotherMember() {
        // arrange
        memberService.register("otherUser", "Password1!", "다른유저", "1990-01-01", "other@test.com");
        String requestId = couponApplicationService.requestCouponIssue("couponReqUser", template.getId());

        // act & assert
        CoreException ex = assertThrows(CoreException.class, () ->
            couponApplicationService.getIssueRequest("otherUser", requestId)
        );
        assertThat(ex.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
    }

    @DisplayName("만료된 쿠폰은 발급 요청이 거절된다.")
    @Test
    void throwsBadRequest_whenTemplateIsExpired() {
        // arrange
        CouponTemplate expired = couponTemplateRepository.save(CouponTemplate.create(
            "만료 쿠폰", CouponType.FIXED, 1_000L, null, ZonedDateTime.now().minusDays(1), 100L
        ));

        // act & assert
        CoreException ex = assertThrows(CoreException.class, () ->
            couponApplicationService.requestCouponIssue("couponReqUser", expired.getId())
        );
        assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }
}
