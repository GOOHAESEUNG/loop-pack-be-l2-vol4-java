package com.loopers.domain.order.event;

import com.loopers.domain.payment.model.CardType;

import java.util.List;

/**
 * 주문 생성 트랜잭션이 커밋된 뒤 발행된다.
 * cardType/cardNo가 null이면 결제수단 미지정 주문(수동 결제 경로)이다.
 */
public record OrderCreatedEvent(
    Long orderId,
    String loginId,
    CardType cardType,
    String cardNo,
    List<Item> items
) {
    public record Item(Long productId, int quantity) {}
}
