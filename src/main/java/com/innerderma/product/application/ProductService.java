package com.innerderma.product.application;

import com.innerderma.common.error.BusinessException;
import com.innerderma.common.error.ErrorCode;
import com.innerderma.product.api.ProductResponse;
import com.innerderma.product.domain.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;
    public ProductService(ProductRepository repository) { this.repository = repository; }

    public List<ProductResponse> getActiveProducts() {
        return repository.findAllByActiveTrueOrderByDisplayPriorityAscProductCodeAsc()
                .stream().map(ProductResponse::from).toList();
    }

    public ProductResponse getProduct(String productCode) {
        return repository.findByProductCode(productCode)
                .map(ProductResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
