package com.loopers.domain.coupon.model;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;

import java.time.ZonedDateTime;

@Entity
@Table(name = "coupon_templates")
@SQLRestriction("deleted_at IS NULL")
@Getter
public class CouponTemplate extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CouponType type;

    @Column(name = "value", nullable = false)
    private Long value;

    @Column(name = "min_order_amount")
    private Long minOrderAmount;

    @Column(name = "expired_at", nullable = false)
    private ZonedDateTime expiredAt;

    /**
     * 선착순 발급 총 수량. null이면 무제한.
     * issuedQuantity 증가는 발급 Consumer(commerce-streamer)가 원자적 UPDATE로 수행한다.
     */
    @Column(name = "total_quantity")
    private Long totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private long issuedQuantity;

    protected CouponTemplate() {}

    private CouponTemplate(String name, CouponType type, Long value, Long minOrderAmount, ZonedDateTime expiredAt, Long totalQuantity) {
        validate(name, type, value, expiredAt);
        if (totalQuantity != null && totalQuantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 수량은 0보다 커야 합니다.");
        }
        this.name = name;
        this.type = type;
        this.value = value;
        this.minOrderAmount = minOrderAmount;
        this.expiredAt = expiredAt;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0L;
    }

    public static CouponTemplate create(String name, CouponType type, Long value, Long minOrderAmount, ZonedDateTime expiredAt) {
        return new CouponTemplate(name, type, value, minOrderAmount, expiredAt, null);
    }

    public static CouponTemplate create(String name, CouponType type, Long value, Long minOrderAmount, ZonedDateTime expiredAt, Long totalQuantity) {
        return new CouponTemplate(name, type, value, minOrderAmount, expiredAt, totalQuantity);
    }

    public void update(String name, CouponType type, Long value, Long minOrderAmount, ZonedDateTime expiredAt) {
        validate(name, type, value, expiredAt);
        this.name = name;
        this.type = type;
        this.value = value;
        this.minOrderAmount = minOrderAmount;
        this.expiredAt = expiredAt;
    }

    public long calculateDiscount(long orderAmount) {
        if (minOrderAmount != null && orderAmount < minOrderAmount) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "최소 주문 금액(" + minOrderAmount + "원)을 충족하지 않아 쿠폰을 사용할 수 없습니다.");
        }
        if (type == CouponType.FIXED) {
            return Math.min(value, orderAmount);
        }
        return orderAmount * value / 100;
    }

    private static void validate(String name, CouponType type, Long value, ZonedDateTime expiredAt) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 필수입니다.");
        }
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 타입은 필수입니다.");
        }
        if (value == null || value <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 값은 0보다 커야 합니다.");
        }
        if (type == CouponType.RATE && value > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정률 쿠폰의 할인율은 100%를 초과할 수 없습니다.");
        }
        if (expiredAt == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "만료일은 필수입니다.");
        }
    }
}
