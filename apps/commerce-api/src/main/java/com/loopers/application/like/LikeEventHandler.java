package com.loopers.application.like;

import com.loopers.domain.like.event.LikeAddedEvent;
import com.loopers.domain.like.event.LikeRemovedEvent;
import com.loopers.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좋아요 커밋 이후 like_count 집계를 비동기로 반영한다.
 * 집계 실패는 좋아요 자체에 영향을 주지 않는다. (사후 정합성은 검증 배치의 몫)
 */
@RequiredArgsConstructor
@Component
public class LikeEventHandler {

    private final ProductRepository productRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(LikeAddedEvent event) {
        productRepository.incrementLikeCount(event.productId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(LikeRemovedEvent event) {
        productRepository.decrementLikeCount(event.productId());
    }
}
