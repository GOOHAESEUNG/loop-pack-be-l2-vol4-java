package com.loopers.domain.coupon.repository;

import com.loopers.domain.coupon.model.CouponIssueRequest;
import com.loopers.domain.coupon.model.IssuedCoupon;

import java.util.Optional;

public interface CouponIssueRepository {

    /**
     * 발급 수량을 원자적으로 1 차감(선점)한다.
     * 수량이 소진되었거나 템플릿이 없으면 0을 반환한다. (total_quantity가 null이면 무제한)
     */
    int deductQuota(Long couponTemplateId);

    boolean existsIssuedCoupon(Long memberId, Long couponTemplateId);

    IssuedCoupon saveIssuedCoupon(IssuedCoupon issuedCoupon);

    Optional<CouponIssueRequest> findRequestByRequestId(String requestId);
}
