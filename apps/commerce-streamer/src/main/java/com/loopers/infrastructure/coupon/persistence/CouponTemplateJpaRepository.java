package com.loopers.infrastructure.coupon.persistence;

import com.loopers.domain.coupon.model.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponTemplateJpaRepository extends JpaRepository<CouponTemplate, Long> {

    @Modifying
    @Query("""
        UPDATE CouponTemplate t
        SET t.issuedQuantity = t.issuedQuantity + 1
        WHERE t.id = :couponTemplateId
          AND (t.totalQuantity IS NULL OR t.issuedQuantity < t.totalQuantity)
        """)
    int deductQuota(@Param("couponTemplateId") Long couponTemplateId);
}
