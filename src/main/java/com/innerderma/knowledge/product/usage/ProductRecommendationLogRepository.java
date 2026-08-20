package com.innerderma.knowledge.product.usage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProductRecommendationLogRepository extends JpaRepository<ProductRecommendationLog, Long> {
    List<ProductRecommendationLog> findByUser_UserCodeAndRecommendedDateAfter(String userCode, LocalDate after);

    /**
     * 같은 날 같은 제품이 이미 기록됐는지 확인한다.
     * 캐시가 인메모리라 재배포 후 재호출 시 중복 저장되는 것을 막는다.
     * (중복이 쌓이면 제품 추천 빈도 제한 계산이 왜곡된다)
     */
    boolean existsByUser_UserCodeAndProductIdAndRecommendedDate(
            String userCode, String productId, LocalDate recommendedDate);
}
