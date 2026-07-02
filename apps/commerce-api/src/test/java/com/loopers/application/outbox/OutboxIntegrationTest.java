package com.loopers.application.outbox;

import com.loopers.application.like.LikeApplicationService;
import com.loopers.application.order.OrderApplicationService;
import com.loopers.application.order.OrderItemRequest;
import com.loopers.domain.brand.model.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.like.event.LikeAddedEvent;
import com.loopers.domain.member.model.Member;
import com.loopers.domain.member.service.MemberService;
import com.loopers.domain.outbox.model.OutboxEvent;
import com.loopers.domain.outbox.model.OutboxEventStatus;
import com.loopers.domain.product.model.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.stock.model.Stock;
import com.loopers.domain.stock.repository.StockRepository;
import com.loopers.infrastructure.outbox.persistence.OutboxEventJpaRepository;
import com.loopers.support.config.KafkaTopicsConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class OutboxIntegrationTest {

    @Autowired
    private LikeApplicationService likeApplicationService;

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    private Product product;
    private Member member;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(Brand.create("나이키"));
        product = productRepository.save(Product.create(brand.getId(), "에어맥스", "운동화", 100_000L));
        stockRepository.save(Stock.create(product.getId(), 100));
        member = memberService.register(
            "outboxUser", "Password1!", "유저", "1990-01-01", "outbox@test.com"
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요를 등록하면, 같은 트랜잭션으로 outbox에 LIKE_ADDED 이벤트가 기록된다.")
    @Test
    void recordsOutboxEvent_whenLikeIsAdded() {
        // act
        likeApplicationService.addLike(member.getId(), product.getId());

        // assert: BEFORE_COMMIT 기록이므로 메서드 리턴 직후 존재해야 한다
        List<OutboxEvent> events = findByType("LIKE_ADDED");
        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getTopic()).isEqualTo(KafkaTopicsConfig.CATALOG_EVENTS);
        assertThat(event.getPartitionKey()).isEqualTo(String.valueOf(product.getId()));
        assertThat(event.getPayload()).contains("\"productId\":" + product.getId());
    }

    @DisplayName("도메인 트랜잭션이 롤백되면, outbox에도 기록되지 않는다.")
    @Test
    void doesNotRecordOutboxEvent_whenTransactionRollsBack() {
        // arrange
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // act
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new LikeAddedEvent(product.getId()));
            status.setRollbackOnly();
            return null;
        });

        // assert
        assertThat(findByType("LIKE_ADDED")).isEmpty();
    }

    @DisplayName("릴레이가 PENDING 이벤트를 Kafka로 발행하고 PUBLISHED로 마킹한다.")
    @Test
    void relayPublishesPendingEventsToKafka() {
        // act
        likeApplicationService.addLike(member.getId(), product.getId());
        String eventId = findByType("LIKE_ADDED").get(0).getEventId();

        // assert 1: 릴레이가 PUBLISHED로 마킹
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            OutboxEvent event = findByType("LIKE_ADDED").get(0);
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
            assertThat(event.getPublishedAt()).isNotNull();
        });

        // assert 2: Kafka에서 실제로 소비 가능
        List<String> consumed = consumeAll(KafkaTopicsConfig.CATALOG_EVENTS, Duration.ofSeconds(10), eventId);
        assertThat(consumed).anySatisfy(value -> assertThat(value).contains(eventId));
    }

    @DisplayName("주문을 생성하면, order-events용 ORDER_CREATED 이벤트가 카드정보 없이 기록된다.")
    @Test
    void recordsOrderCreatedEvent_withoutCardInfo() {
        // act
        Long orderId = orderApplicationService.createOrder(
            "outboxUser",
            List.of(new OrderItemRequest(product.getId(), 2)),
            null
        );

        // assert
        List<OutboxEvent> events = findByType("ORDER_CREATED");
        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getTopic()).isEqualTo(KafkaTopicsConfig.ORDER_EVENTS);
        assertThat(event.getPartitionKey()).isEqualTo(String.valueOf(orderId));
        assertThat(event.getPayload())
            .contains("\"orderId\":" + orderId)
            .contains("\"quantity\":2")
            .doesNotContain("cardNo")
            .doesNotContain("cardType");
    }

    private List<OutboxEvent> findByType(String eventType) {
        return outboxEventJpaRepository.findAll().stream()
            .filter(event -> event.getEventType().equals(eventType))
            .toList();
    }

    private List<String> consumeAll(String topic, Duration timeout, String untilContains) {
        Properties props = new Properties();
        props.putAll(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.kafka.bootstrap-servers"),
            ConsumerConfig.GROUP_ID_CONFIG, "outbox-test-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()
        ));

        List<String> values = new CopyOnWriteArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            await().atMost(timeout).until(() -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    values.add(record.value());
                }
                return values.stream().anyMatch(value -> value.contains(untilContains));
            });
        }
        return values;
    }
}
