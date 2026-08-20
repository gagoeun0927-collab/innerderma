-- 상품 다국어 번역 테이블
CREATE TABLE IF NOT EXISTS product_translations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(300) NOT NULL,
    usage_text VARCHAR(1000),
    features_json TEXT,
    caution VARCHAR(2000),
    UNIQUE KEY uk_product_translations_code_locale (product_code, locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
