package com.innerderma.wishlist.domain;

import com.innerderma.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_wishlist_user_product", columnNames = {"user_id", "product_id"}))
public class WishlistItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "product_source", nullable = false, length = 20)
    private String productSource;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    protected WishlistItem() {}

    public WishlistItem(User user, String productId, String productSource) {
        this.user = user;
        this.productId = productId;
        this.productSource = productSource;
        this.addedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getProductId() { return productId; }
    public String getProductSource() { return productSource; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
