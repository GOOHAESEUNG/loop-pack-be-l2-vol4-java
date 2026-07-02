package com.loopers.application.like;

import com.loopers.domain.brand.model.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.like.repository.LikeRepository;
import com.loopers.domain.member.model.Member;
import com.loopers.domain.member.service.MemberService;
import com.loopers.domain.product.model.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class LikeEventFailureIntegrationTest {

    @Autowired
    private LikeApplicationService likeApplicationService;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private LikeRepository likeRepository;

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
        member = memberService.register(
            "likeFailUser", "Password1!", "유저", "1990-01-01", "likefail@test.com"
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("like_count 집계가 실패해도, 좋아요 등록은 성공한다.")
    @Test
    void addLikeSucceeds_evenWhenAggregationFails() {
        // arrange
        doThrow(new RuntimeException("집계 실패")).when(productRepository).incrementLikeCount(product.getId());

        // act & assert
        assertThatCode(() -> likeApplicationService.addLike(member.getId(), product.getId()))
            .doesNotThrowAnyException();

        assertThat(likeRepository.existsByUserIdAndProductId(member.getId(), product.getId())).isTrue();

        // 집계는 반영되지 못한 채 유지된다 (사후 정합성 처리는 별도 수단의 몫)
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            Product updated = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isZero();
        });
    }
}
