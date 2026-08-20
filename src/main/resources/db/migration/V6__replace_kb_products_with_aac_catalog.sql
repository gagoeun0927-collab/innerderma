-- AAC 신규 제품 카탈로그로 교체 (KB 제품 38개 → 16개)
--
-- 배경: 제품 리스트가 PSS_001~022 / WIM_001~018 / WHS_P023~030 / SWL_013~014 에서
-- PSS_001~007 / WIM_001~009 (총 16개)로 교체됐다. 제품명·설명·가격도 모두 변경됐다.
--
-- 시딩은 existsByProductCode로 멱등성을 판단하므로, 기존 행이 남아 있으면
-- 갱신되지 않고 옛 데이터가 그대로 유지된다. 따라서 여기서 정리한다.
--
-- 데모 제품(DEMO-*, source IS NULL)은 유지한다.

-- 1) 기존 KB 제품 번역 삭제 (product_translations는 products와 FK가 없어 별도 삭제)
DELETE FROM product_translations;

-- 2) 기존 KB 제품 삭제 (데모 제품은 source IS NULL 이라 보존됨)
--    장바구니/위시리스트/추천이력은 product_id를 문자열로 보관하고 FK가 없으므로
--    삭제된 제품을 참조하는 행이 남을 수 있다. 사라진 제품 참조를 함께 정리한다.
DELETE FROM cart_items
WHERE product_id NOT IN (
    'PSS_001','PSS_002','PSS_003','PSS_004','PSS_005','PSS_006','PSS_007',
    'WIM_001','WIM_002','WIM_003','WIM_004','WIM_005','WIM_006','WIM_007','WIM_008','WIM_009'
);

DELETE FROM wishlist_items
WHERE product_id NOT IN (
    'PSS_001','PSS_002','PSS_003','PSS_004','PSS_005','PSS_006','PSS_007',
    'WIM_001','WIM_002','WIM_003','WIM_004','WIM_005','WIM_006','WIM_007','WIM_008','WIM_009'
);

DELETE FROM product_recommendation_logs
WHERE product_id NOT IN (
    'PSS_001','PSS_002','PSS_003','PSS_004','PSS_005','PSS_006','PSS_007',
    'WIM_001','WIM_002','WIM_003','WIM_004','WIM_005','WIM_006','WIM_007','WIM_008','WIM_009'
);

DELETE FROM products WHERE source IS NOT NULL;
