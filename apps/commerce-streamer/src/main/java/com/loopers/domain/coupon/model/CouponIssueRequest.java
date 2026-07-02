package com.loopers.domain.coupon.model;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * commerce-api가 소유한 coupon_issue_requests 테이블의 Consumer 관점 매핑.
 * commerce-api가 PENDING으로 접수하고, Consumer가 결과(SUCCESS/FAILED)를 확정한다.
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

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reason")
    private String reason;

    protected CouponIssueRequest() {}

    private CouponIssueRequest(String requestId, Long memberId, Long couponTemplateId) {
        this.requestId = requestId;
        this.memberId = memberId;
        this.couponTemplateId = couponTemplateId;
        this.status = "PENDING";
    }

    /**
     * 테스트 픽스처용. 운영에서 요청 접수는 commerce-api의 책임이다.
     */
    public static CouponIssueRequest create(String requestId, Long memberId, Long couponTemplateId) {
        return new CouponIssueRequest(requestId, memberId, couponTemplateId);
    }

    public boolean isCompleted() {
        return !"PENDING".equals(status);
    }

    public void complete() {
        this.status = "SUCCESS";
        this.reason = null;
    }

    public void fail(String reason) {
        this.status = "FAILED";
        this.reason = reason;
    }
}
