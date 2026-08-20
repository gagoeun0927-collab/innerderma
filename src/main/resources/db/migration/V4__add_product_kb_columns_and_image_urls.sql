-- products 테이블에 KB(지식베이스) 통합 컬럼 추가 + category enum 확장 + image_url 갱신
-- 엔티티(Product.java)에는 이미 반영되어 있으나 마이그레이션이 누락되어 있었음.

-- 1) KB 컬럼 추가
ALTER TABLE products
    ADD COLUMN source VARCHAR(20) NULL,
    ADD COLUMN price INT NULL,
    ADD COLUMN image_url VARCHAR(500) NULL,
    ADD COLUMN usage_instruction VARCHAR(500) NULL,
    ADD COLUMN application_method VARCHAR(500) NULL,
    ADD COLUMN verified_claims_json LONGTEXT NULL,
    ADD COLUMN ingredients_highlight_json LONGTEXT NULL,
    ADD COLUMN skin_state_tags_json LONGTEXT NULL;

-- 2) category enum 확장 (스킨케어 + 섭취류)
ALTER TABLE products
    MODIFY COLUMN category ENUM(
        'CLEANSER','TONER','SERUM','MOISTURIZER','SUNSCREEN','OIL','MASK','TARGETED_CARE',
        'POWDER','JELLY','SUPPLEMENT','DRINK','FOOD'
    ) NOT NULL;

-- 3) product_translations.features_json 타입을 엔티티(@Lob)와 일치시킴
ALTER TABLE product_translations
    MODIFY COLUMN features_json LONGTEXT NULL;

-- 4) 기존 KB 상품의 image_url을 정적 리소스 경로로 갱신
UPDATE products
SET image_url = CONCAT('/product-images/', product_code, '.jpg')
WHERE source IS NOT NULL AND product_code IS NOT NULL;
