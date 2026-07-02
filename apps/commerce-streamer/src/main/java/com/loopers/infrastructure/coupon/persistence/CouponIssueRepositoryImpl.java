package com.loopers.infrastructure.coupon.persistence;

import com.loopers.domain.coupon.model.CouponIssueRequest;
import com.loopers.domain.coupon.model.IssuedCoupon;
import com.loopers.domain.coupon.repository.CouponIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CouponIssueRepositoryImpl implements CouponIssueRepository {

    private final CouponTemplateJpaRepository couponTemplateJpaRepository;
    private final IssuedCouponJpaRepository issuedCouponJpaRepository;
    private final CouponIssueRequestJpaRepository couponIssueRequestJpaRepository;

    @Override
    public int deductQuota(Long couponTemplateId) {
        return couponTemplateJpaRepository.deductQuota(couponTemplateId);
    }

    @Override
    public boolean existsIssuedCoupon(Long memberId, Long couponTemplateId) {
        return issuedCouponJpaRepository.existsByMemberIdAndCouponTemplateId(memberId, couponTemplateId);
    }

    @Override
    public IssuedCoupon saveIssuedCoupon(IssuedCoupon issuedCoupon) {
        return issuedCouponJpaRepository.save(issuedCoupon);
    }

    @Override
    public Optional<CouponIssueRequest> findRequestByRequestId(String requestId) {
        return couponIssueRequestJpaRepository.findByRequestId(requestId);
    }
}
