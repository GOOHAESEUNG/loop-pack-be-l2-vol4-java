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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class LikeEventIntegrationTest {

    @Autowired
    private LikeApplicationService likeApplicationService;

    @Autowired
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
            "likeEventUser", "Password1!", "유저", "1990-01-01", "likeevent@test.com"
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요를 등록하면, 커밋 이후 like_count가 최종적으로 반영된다.")
    @Test
    void likeCountIsEventuallyIncremented_whenLikeIsAdded() {
        // act
        likeApplicationService.addLike(member.getId(), product.getId());

        // assert
        assertThat(likeRepository.existsByUserIdAndProductId(member.getId(), product.getId())).isTrue();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Product updated = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(1);
        });
    }

    @DisplayName("좋아요를 취소하면, 커밋 이후 like_count가 최종적으로 감소한다.")
    @Test
    void likeCountIsEventuallyDecremented_whenLikeIsRemoved() {
        // arrange
        likeApplicationService.addLike(member.getId(), product.getId());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Product updated = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isEqualTo(1);
        });

        // act
        likeApplicationService.removeLike(member.getId(), product.getId());

        // assert
        assertThat(likeRepository.existsByUserIdAndProductId(member.getId(), product.getId())).isFalse();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Product updated = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getLikeCount()).isZero();
        });
    }
}
