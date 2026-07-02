package com.loopers.domain.coupon.event;

public record CouponIssueRequestedEvent(
    String requestId,
    Long memberId,
    Long couponTemplateId
) {
}
