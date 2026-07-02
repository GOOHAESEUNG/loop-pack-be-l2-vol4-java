package com.loopers.application.coupon;

import com.loopers.domain.coupon.model.CouponIssueRequest;
import com.loopers.domain.coupon.model.CouponIssueRequestStatus;

public record CouponIssueRequestInfo(
    String requestId,
    CouponIssueRequestStatus status,
    String reason
) {
    public static CouponIssueRequestInfo from(CouponIssueRequest request) {
        return new CouponIssueRequestInfo(request.getRequestId(), request.getStatus(), request.getReason());
    }
}
