package com.loopers.infrastructure.metrics.persistence;

import com.loopers.domain.metrics.model.ProductMetrics;
import com.loopers.domain.metrics.repository.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository productMetricsJpaRepository;

    @Override
    public int applyDelta(Long productId, long likeDelta, long salesDelta, long viewDelta) {
        return productMetricsJpaRepository.applyDelta(productId, likeDelta, salesDelta, viewDelta);
    }

    @Override
    public Optional<ProductMetrics> findByProductId(Long productId) {
        return productMetricsJpaRepository.findByProductId(productId);
    }
}
