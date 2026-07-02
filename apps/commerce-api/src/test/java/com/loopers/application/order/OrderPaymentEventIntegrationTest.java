package com.loopers.application.order;

import com.loopers.domain.brand.model.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.member.model.Member;
import com.loopers.domain.member.service.MemberService;
import com.loopers.domain.order.repository.OrderRepository;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentGatewayResult;
import com.loopers.domain.payment.model.CardType;
import com.loopers.domain.payment.model.PaymentStatus;
import com.loopers.domain.payment.repository.PaymentRepository;
import com.loopers.domain.product.model.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.stock.model.Stock;
import com.loopers.domain.stock.repository.StockRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderPaymentEventIntegrationTest {

    @Autowired
    private OrderApplicationService orderApplicationService;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(Brand.create("나이키"));
        product = productRepository.save(Product.create(brand.getId(), "에어맥스", "운동화", 100_000L));
        stockRepository.save(Stock.create(product.getId(), 100));
        member = memberService.register(
            "orderPayUser", "Password1!", "유저", "1990-01-01", "orderpay@test.com"
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("결제수단을 포함해 주문하면, 커밋 이후 결제 요청이 자동으로 수행된다.")
    @Test
    void paymentIsRequestedAutomatically_whenOrderIncludesPaymentMethod() {
        // arrange
        when(paymentGateway.requestPayment(any()))
            .thenReturn(new PaymentGatewayResult("tx-100", PaymentStatus.PENDING, null));

        // act
        Long orderId = orderApplicationService.createOrder(
            "orderPayUser",
            List.of(new OrderItemRequest(product.getId(), 1)),
            null,
            new PaymentMethod(CardType.SAMSUNG, "1234-5678-9814-1451")
        );

        // assert
        assertThat(orderRepository.findById(orderId)).isPresent();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(paymentRepository.existsActiveByOrderId(orderId)).isTrue()
        );
    }

    @DisplayName("PG 호출이 실패해도, 주문은 저장되고 결제는 PENDING으로 남아 복구 대상이 된다.")
    @Test
    void orderIsCommitted_evenWhenPgRequestFails() {
        // arrange
        when(paymentGateway.requestPayment(any())).thenThrow(new RuntimeException("PG 장애"));

        // act & assert: 주문 생성은 PG 장애의 영향을 받지 않는다
        assertThatCode(() -> {
            Long orderId = orderApplicationService.createOrder(
                "orderPayUser",
                List.of(new OrderItemRequest(product.getId(), 1)),
                null,
                new PaymentMethod(CardType.SAMSUNG, "1234-5678-9814-1451")
            );
            assertThat(orderRepository.findById(orderId)).isPresent();

            // 결제는 PG 호출 전에 PENDING으로 생성되어, 폴링 복구의 대상이 된다
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(paymentRepository.existsActiveByOrderId(orderId)).isTrue()
            );
        }).doesNotThrowAnyException();
    }

    @DisplayName("결제수단 없이 주문하면, 결제 요청이 수행되지 않는다.")
    @Test
    void paymentIsNotRequested_whenOrderHasNoPaymentMethod() {
        // act
        Long orderId = orderApplicationService.createOrder(
            "orderPayUser",
            List.of(new OrderItemRequest(product.getId(), 1)),
            null
        );

        // assert
        assertThat(orderRepository.findById(orderId)).isPresent();
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3)).untilAsserted(() ->
            assertThat(paymentRepository.existsActiveByOrderId(orderId)).isFalse()
        );
    }
}
