package com.loopers.application.log;

import com.loopers.application.like.LikeApplicationService;
import com.loopers.application.product.ProductFacade;
import com.loopers.domain.brand.model.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.member.model.Member;
import com.loopers.domain.member.service.MemberService;
import com.loopers.domain.product.model.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.stock.model.Stock;
import com.loopers.domain.stock.repository.StockRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class UserActionLogIntegrationTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private LikeApplicationService likeApplicationService;

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
            "actionLogUser", "Password1!", "유저", "1990-01-01", "actionlog@test.com"
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품을 조회하면, 유저 행동 로그가 비동기로 기록된다.")
    @Test
    void logsProductViewedAction_whenProductIsViewed(CapturedOutput output) {
        // act
        productFacade.getProduct(product.getId());

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(output.getOut()).contains("[USER_ACTION] PRODUCT_VIEWED productId=" + product.getId())
        );
    }

    @DisplayName("좋아요를 등록하면, 유저 행동 로그가 비동기로 기록된다.")
    @Test
    void logsLikeAddedAction_whenLikeIsAdded(CapturedOutput output) {
        // act
        likeApplicationService.addLike(member.getId(), product.getId());

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertThat(output.getOut()).contains("[USER_ACTION] LIKE_ADDED productId=" + product.getId())
        );
    }
}
