package com.loopers.domain.coupon.model;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 선착순 쿠폰 발급 요청.
 * commerce-api가 PENDING으로 접수하고, commerce-streamer의 Consumer가 결과를 확정한다.
 * 유저는 requestId로 결과를 polling 조회한다.
 */
@Entity
@Table(name = "coupon_issue_requests")
@Getter
public class CouponIssueRequest extends BaseEntity {

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponIssueRequestStatus status;

    @Column(name = "reason")
    private String reason;

    protected CouponIssueRequest() {}

    private CouponIssueRequest(String requestId, Long memberId, Long couponTemplateId) {
        if (requestId == null || requestId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "요청 ID는 필수입니다.");
        }
        this.requestId = requestId;
        this.memberId = memberId;
        this.couponTemplateId = couponTemplateId;
        this.status = CouponIssueRequestStatus.PENDING;
    }

    public static CouponIssueRequest create(String requestId, Long memberId, Long couponTemplateId) {
        return new CouponIssueRequest(requestId, memberId, couponTemplateId);
    }
}
