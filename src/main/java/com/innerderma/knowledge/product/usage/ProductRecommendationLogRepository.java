package com.innerderma.knowledge.product.usage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProductRecommendationLogRepository extends JpaRepository<ProductRecommendationLog, Long> {
    List<ProductRecommendationLog> findByUser_UserCodeAndRecommendedDateAfter(String userCode, LocalDate after);
}
