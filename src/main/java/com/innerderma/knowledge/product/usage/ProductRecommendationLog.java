package com.innerderma.knowledge.product.usage;

import com.innerderma.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * 사용자에게 특정 제품이 추천된 이력.
 * ProductMatcher가 추천 빈도 제한을 적용할 때 이 테이블을 참조한다.
 */
@Entity
@Table(name = "product_recommendation_logs", indexes = {
        @Index(name = "idx_rec_log_user_product_date", columnList = "user_id, product_id, recommended_date")
})
public class ProductRecommendationLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "product_source", nullable = false, length = 20)
    private String productSource;

    @Column(name = "recommended_date", nullable = false)
    private LocalDate recommendedDate;

    protected ProductRecommendationLog() {}

    public ProductRecommendationLog(User user, String productId, String productSource, LocalDate recommendedDate) {
        this.user = user;
        this.productId = productId;
        this.productSource = productSource;
        this.recommendedDate = recommendedDate;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getProductId() { return productId; }
    public String getProductSource() { return productSource; }
    public LocalDate getRecommendedDate() { return recommendedDate; }
}
