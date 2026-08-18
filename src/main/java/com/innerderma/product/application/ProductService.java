package com.innerderma.product.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.product.api.ProductResponse;
import com.innerderma.product.domain.ProductCategory;
import com.innerderma.product.domain.ProductConcern;
import com.innerderma.product.domain.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;
    public ProductService(ProductRepository repository) { this.repository = repository; }

    /** category와 concern은 모두 선택 필터이며, null이면 해당 조건을 적용하지 않는다. */
    public List<ProductResponse> getActiveProducts(ProductCategory category, ProductConcern concern) {
        return repository.findAllByActiveTrueOrderByDisplayPriorityAscProductCodeAsc().stream()
                .filter(product -> category == null || product.getCategory() == category)
                .filter(product -> concern == null || product.getTargetConcern() == concern)
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProduct(String productCode) {
        return repository.findByProductCode(productCode)
                .map(ProductResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
