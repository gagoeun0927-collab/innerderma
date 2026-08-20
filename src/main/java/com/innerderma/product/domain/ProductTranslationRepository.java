package com.innerderma.product.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductTranslationRepository extends JpaRepository<ProductTranslation, Long> {

    Optional<ProductTranslation> findByProductCodeAndLocale(String productCode, String locale);

    List<ProductTranslation> findAllByLocale(String locale);

    List<ProductTranslation> findAllByProductCodeInAndLocale(List<String> productCodes, String locale);

    boolean existsByProductCodeAndLocale(String productCode, String locale);
}
