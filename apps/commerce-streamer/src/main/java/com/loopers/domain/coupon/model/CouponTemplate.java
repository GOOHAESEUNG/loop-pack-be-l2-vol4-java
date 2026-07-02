package com.loopers.domain.coupon.model;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * commerce-api가 소유한 coupon_templates 테이블의 Consumer 관점 매핑.
 * 발급 수량 차감(원자적 UPDATE)에 필요한 컬럼만 매핑한다. (apps 간 코드 의존 금지)
 */
@Entity
@Table(name = "coupon_templates")
@Getter
public class CouponTemplate extends BaseEntity {

    @Column(name = "total_quantity")
    private Long totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private long issuedQuantity;

    protected CouponTemplate() {}

    private CouponTemplate(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0L;
    }

    /**
     * 테스트 픽스처용. 운영에서 템플릿 생성은 commerce-api의 책임이다.
     */
    public static CouponTemplate create(Long totalQuantity) {
        return new CouponTemplate(totalQuantity);
    }
}
