package com.innerderma.product.application;

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
}
