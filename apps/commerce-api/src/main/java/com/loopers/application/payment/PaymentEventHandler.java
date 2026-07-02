package com.loopers.application.payment;

import com.loopers.domain.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 커밋 이후 결제 요청을 비동기로 수행한다.
 * PG 장애로 요청이 실패해도 주문은 이미 커밋되어 있으며,
 * PENDING 결제는 PaymentRecoveryScheduler가 복구한다.
 */
@RequiredArgsConstructor
@Component
public class PaymentEventHandler {

    private final PaymentApplicationService paymentApplicationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCreatedEvent event) {
        if (event.cardType() == null) {
            return; // 결제수단 미지정 주문은 별도 결제 API(수동) 경로를 사용한다
        }
        paymentApplicationService.requestPayment(
            event.loginId(), event.orderId(), event.cardType(), event.cardNo());
    }
}
