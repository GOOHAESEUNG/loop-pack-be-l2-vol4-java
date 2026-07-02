package com.loopers.application.log;

import com.loopers.domain.like.event.LikeAddedEvent;
import com.loopers.domain.like.event.LikeRemovedEvent;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.product.event.ProductViewedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 유저 행동(조회, 좋아요, 주문)에 대한 서버 레벨 로깅.
 * 행동 "시도" 자체를 기록하므로 트랜잭션 커밋 여부와 무관하게 동작한다. (@EventListener)
 */
@Slf4j
@Component
public class UserActionLogHandler {

    @Async
    @EventListener
    public void handle(ProductViewedEvent event) {
        log.info("[USER_ACTION] PRODUCT_VIEWED productId={}", event.productId());
    }

    @Async
    @EventListener
    public void handle(LikeAddedEvent event) {
        log.info("[USER_ACTION] LIKE_ADDED productId={}", event.productId());
    }

    @Async
    @EventListener
    public void handle(LikeRemovedEvent event) {
        log.info("[USER_ACTION] LIKE_REMOVED productId={}", event.productId());
    }

    @Async
    @EventListener
    public void handle(OrderCreatedEvent event) {
        // 카드 정보는 민감 정보이므로 로깅하지 않는다
        log.info("[USER_ACTION] ORDER_CREATED orderId={} loginId={}", event.orderId(), event.loginId());
    }
}
