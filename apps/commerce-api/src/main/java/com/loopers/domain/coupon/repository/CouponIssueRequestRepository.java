package com.loopers.domain.coupon.repository;

import com.loopers.domain.coupon.model.CouponIssueRequest;

import java.util.Optional;

public interface CouponIssueRequestRepository {
    CouponIssueRequest save(CouponIssueRequest request);
    Optional<CouponIssueRequest> findByRequestId(String requestId);
}
