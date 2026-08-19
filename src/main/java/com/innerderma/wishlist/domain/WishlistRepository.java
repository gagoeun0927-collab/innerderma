package com.innerderma.wishlist.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUser_UserCodeOrderByAddedAtDesc(String userCode);
    Optional<WishlistItem> findByUser_UserCodeAndProductId(String userCode, String productId);
    boolean existsByUser_UserCodeAndProductId(String userCode, String productId);
    void deleteByUser_UserCodeAndProductId(String userCode, String productId);
}
