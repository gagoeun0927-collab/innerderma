package com.innerderma.cart.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser_UserCodeOrderByAddedAtDesc(String userCode);
    Optional<CartItem> findByUser_UserCodeAndProductId(String userCode, String productId);
    void deleteByUser_UserCodeAndProductId(String userCode, String productId);
    void deleteByUser_UserCode(String userCode);
}
