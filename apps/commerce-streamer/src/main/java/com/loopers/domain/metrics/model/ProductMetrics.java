package com.loopers.domain.metrics.model;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/**
 * 상품별 집계 지표. 쓰기는 네이티브 upsert(applyDelta)로만 수행하고,
 * 이 엔티티는 스키마 정의와 조회 용도로 사용한다.
 */
@Entity
@Table(
    name = "product_metrics",
    uniqueConstraints = @UniqueConstraint(columnNames = "product_id")
)
@Getter
public class ProductMetrics extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    protected ProductMetrics() {}
}
