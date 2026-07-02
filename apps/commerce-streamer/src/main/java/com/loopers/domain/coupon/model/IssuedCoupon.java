package com.loopers.domain.coupon.model;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

/**
 * commerce-api가 소유한 issued_coupons 테이블의 Consumer 관점 매핑.
 * Consumer는 AVAILABLE 상태의 발급만 수행한다.
 */
@Entity
@Table(
    name = "issued_coupons",
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "coupon_template_id"})
)
@Getter
public class IssuedCoupon extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "coupon_template_id", nullable = false)
    private Long couponTemplateId;

    @Column(name = "status", nullable = false)
    private String status;

    @Version
    @Column(name = "version")
    private Long version;

    protected IssuedCoupon() {}

    private IssuedCoupon(Long memberId, Long couponTemplateId) {
        this.memberId = memberId;
        this.couponTemplateId = couponTemplateId;
        this.status = "AVAILABLE";
    }

    public static IssuedCoupon create(Long memberId, Long couponTemplateId) {
        return new IssuedCoupon(memberId, couponTemplateId);
    }
}
