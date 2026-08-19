-- V2: Add wishlist, cart, and product recommendation log tables

CREATE TABLE IF NOT EXISTS wishlist_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    product_source VARCHAR(20) NOT NULL,
    added_at DATETIME NOT NULL,
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_wishlist_user_product UNIQUE (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    product_source VARCHAR(20) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    added_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_recommendation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    product_source VARCHAR(20) NOT NULL,
    recommended_date DATE NOT NULL,
    CONSTRAINT fk_rec_log_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_rec_log_user_product_date (user_id, product_id, recommended_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
