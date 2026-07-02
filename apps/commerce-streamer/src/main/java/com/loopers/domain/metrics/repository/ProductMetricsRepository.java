package com.loopers.domain.metrics.repository;

import com.loopers.domain.metrics.model.ProductMetrics;

import java.util.Optional;

public interface ProductMetricsRepository {
    /**
     * 상품 지표를 원자적으로 증감한다. 행이 없으면 생성한다. (upsert)
     */
    int applyDelta(Long productId, long likeDelta, long salesDelta, long viewDelta);

    Optional<ProductMetrics> findByProductId(Long productId);
}
