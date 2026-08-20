package com.innerderma.product.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.product.api.ProductResponse;
import com.innerderma.product.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final ProductTranslationRepository translationRepository;

    public ProductService(ProductRepository repository,
                          ProductTranslationRepository translationRepository) {
        this.repository = repository;
        this.translationRepository = translationRepository;
    }

    /**
     * 활성 제품 목록을 조회한다.
     * locale이 지정되면 해당 언어 번역을 함께 내려준다 (없으면 translation=null → 프론트가 한국어 폴백).
     */
    public List<ProductResponse> getActiveProducts(ProductCategory category, ProductConcern concern,
                                                   String source, String locale) {
        List<Product> products = repository.findAllByActiveTrueOrderByDisplayPriorityAscProductCodeAsc().stream()
                .filter(product -> category == null || product.getCategory() == category)
                .filter(product -> concern == null || product.getTargetConcern() == concern)
                .filter(product -> source == null || source.equalsIgnoreCase(product.getSource()))
                .toList();

        if (locale == null || locale.isBlank() || "ko".equalsIgnoreCase(locale)) {
            return products.stream().map(ProductResponse::from).toList();
        }

        // 번역을 한 번에 조회 (N+1 방지)
        List<String> productCodes = products.stream().map(Product::getProductCode).toList();
        Map<String, ProductTranslation> translationMap = translationRepository
                .findAllByProductCodeInAndLocale(productCodes, normalizeLocale(locale))
                .stream()
                .collect(Collectors.toMap(ProductTranslation::getProductCode, t -> t));

        return products.stream()
                .map(p -> ProductResponse.from(p, translationMap.get(p.getProductCode())))
                .toList();
    }

    /** locale 없는 하위 호환 오버로드 */
    public List<ProductResponse> getActiveProducts(ProductCategory category, ProductConcern concern, String source) {
        return getActiveProducts(category, concern, source, null);
    }

    /** 하위 호환 (category + concern만) */
    public List<ProductResponse> getActiveProducts(ProductCategory category, ProductConcern concern) {
        return getActiveProducts(category, concern, null, null);
    }

    /**
     * 단일 제품 상세 조회.
     * locale이 지정되면 해당 번역도 포함.
     */
    public ProductResponse getProduct(String productCode, String locale) {
        Product product = repository.findByProductCode(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (locale == null || locale.isBlank() || "ko".equalsIgnoreCase(locale)) {
            return ProductResponse.from(product);
        }

        ProductTranslation translation = translationRepository
                .findByProductCodeAndLocale(productCode, normalizeLocale(locale))
                .orElse(null);

        return ProductResponse.from(product, translation);
    }

    /** locale 없는 하위 호환 오버로드 */
    public ProductResponse getProduct(String productCode) {
        return getProduct(productCode, null);
    }

    /**
     * Accept-Language 헤더에서 주요 언어만 추출. "en-US" → "en", "ja" → "ja"
     */
    private String normalizeLocale(String locale) {
        if (locale == null) return null;
        String normalized = locale.trim().toLowerCase();
        int dashIdx = normalized.indexOf('-');
        if (dashIdx > 0) normalized = normalized.substring(0, dashIdx);
        int underscoreIdx = normalized.indexOf('_');
        if (underscoreIdx > 0) normalized = normalized.substring(0, underscoreIdx);
        return normalized;
    }
}
