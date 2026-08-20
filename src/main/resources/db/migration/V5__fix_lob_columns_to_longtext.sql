-- @Lob String 컬럼을 LONGTEXT로 통일한다.
--
-- 배경: 프로덕션 스키마는 Flyway 도입 전 Hibernate ddl-auto=update가 생성했고
-- Flyway는 baseline으로 건너뛰었다. 당시 Hibernate는 @Lob String의 길이
-- 기본값(255) 때문에 tinytext로 만들었다. tinytext는 255바이트 제한이라
-- JSON 저장에 부적합하며, 엔티티(length = Integer.MAX_VALUE)와도 불일치해
-- ddl-auto=validate가 실패한다.

ALTER TABLE ai_rules
    MODIFY COLUMN conditions_json LONGTEXT NOT NULL,
    MODIFY COLUMN actions_json LONGTEXT NOT NULL,
    MODIFY COLUMN restrictions_json LONGTEXT NOT NULL;

ALTER TABLE skin_analyses
    MODIFY COLUMN raw_result LONGTEXT NOT NULL;

ALTER TABLE skin_state_snapshots
    MODIFY COLUMN symptom_scores_json LONGTEXT NOT NULL,
    MODIFY COLUMN analysis_scores_json LONGTEXT NULL;

ALTER TABLE care_solutions
    MODIFY COLUMN evening_steps_json LONGTEXT NOT NULL,
    MODIFY COLUMN morning_steps_json LONGTEXT NOT NULL;

ALTER TABLE products
    MODIFY COLUMN verified_claims_json LONGTEXT NULL,
    MODIFY COLUMN ingredients_highlight_json LONGTEXT NULL,
    MODIFY COLUMN skin_state_tags_json LONGTEXT NULL;

ALTER TABLE product_translations
    MODIFY COLUMN features_json LONGTEXT NULL;
