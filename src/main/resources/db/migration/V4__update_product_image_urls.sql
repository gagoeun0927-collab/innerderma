-- 상품 이미지 URL을 정적 리소스 경로로 일괄 갱신
UPDATE products
SET image_url = CONCAT('/product-images/', product_code, '.jpg')
WHERE source IS NOT NULL AND product_code IS NOT NULL;
