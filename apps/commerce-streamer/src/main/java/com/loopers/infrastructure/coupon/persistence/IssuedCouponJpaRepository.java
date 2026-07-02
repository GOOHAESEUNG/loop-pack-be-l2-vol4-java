package com.loopers.infrastructure.coupon.persistence;

import com.loopers.domain.coupon.model.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {
    boolean existsByMemberIdAndCouponTemplateId(Long memberId, Long couponTemplateId);
}
