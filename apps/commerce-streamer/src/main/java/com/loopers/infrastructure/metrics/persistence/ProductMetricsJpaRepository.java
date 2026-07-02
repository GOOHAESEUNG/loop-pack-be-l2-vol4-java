package com.loopers.infrastructure.metrics.persistence;

import com.loopers.domain.metrics.model.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

    Optional<ProductMetrics> findByProductId(Long productId);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, created_at, updated_at)
        VALUES (:productId, GREATEST(:likeDelta, 0), GREATEST(:salesDelta, 0), GREATEST(:viewDelta, 0), NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            like_count = GREATEST(0, like_count + :likeDelta),
            sales_count = GREATEST(0, sales_count + :salesDelta),
            view_count = GREATEST(0, view_count + :viewDelta),
            updated_at = NOW()
        """, nativeQuery = true)
    int applyDelta(
        @Param("productId") Long productId,
        @Param("likeDelta") long likeDelta,
        @Param("salesDelta") long salesDelta,
        @Param("viewDelta") long viewDelta
    );
}
