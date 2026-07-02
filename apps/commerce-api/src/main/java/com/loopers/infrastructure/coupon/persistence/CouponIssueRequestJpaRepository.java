package com.loopers.infrastructure.coupon.persistence;

import com.loopers.domain.coupon.model.CouponIssueRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponIssueRequestJpaRepository extends JpaRepository<CouponIssueRequest, Long> {
    Optional<CouponIssueRequest> findByRequestId(String requestId);
}
