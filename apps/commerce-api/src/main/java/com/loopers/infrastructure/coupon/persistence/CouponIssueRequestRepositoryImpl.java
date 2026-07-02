package com.loopers.infrastructure.coupon.persistence;

import com.loopers.domain.coupon.model.CouponIssueRequest;
import com.loopers.domain.coupon.repository.CouponIssueRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CouponIssueRequestRepositoryImpl implements CouponIssueRequestRepository {

    private final CouponIssueRequestJpaRepository couponIssueRequestJpaRepository;

    @Override
    public CouponIssueRequest save(CouponIssueRequest request) {
        return couponIssueRequestJpaRepository.save(request);
    }

    @Override
    public Optional<CouponIssueRequest> findByRequestId(String requestId) {
        return couponIssueRequestJpaRepository.findByRequestId(requestId);
    }
}
