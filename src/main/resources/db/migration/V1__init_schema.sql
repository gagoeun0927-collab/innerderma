-- InnerDerma V1 Initial Schema
-- Generated from JPA entity definitions

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    preferred_locale VARCHAR(10) NOT NULL DEFAULT 'en'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL UNIQUE,
    brand VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(30) NOT NULL,
    target_concern VARCHAR(30) NOT NULL,
    safety_attention_compatible BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    demo_product BOOLEAN NOT NULL DEFAULT FALSE,
    official_url VARCHAR(500),
    display_priority INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    priority INT NOT NULL,
    conditions_json LONGTEXT NOT NULL,
    actions_json LONGTEXT NOT NULL,
    restrictions_json LONGTEXT NOT NULL,
    explanation_template VARCHAR(1000),
    version VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_ai_rule_id_version UNIQUE (rule_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skin_captures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    captured_date DATE NOT NULL,
    captured_at DATETIME NOT NULL,
    image_path VARCHAR(500) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    quality_status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_skin_capture_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skin_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skin_capture_id BIGINT NOT NULL UNIQUE,
    analyzed_at DATETIME NOT NULL,
    overall_score DOUBLE NOT NULL,
    skin_health_grade VARCHAR(50) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    raw_result LONGTEXT NOT NULL,
    CONSTRAINT fk_skin_analysis_capture FOREIGN KEY (skin_capture_id) REFERENCES skin_captures(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS whs_skin_diagnoses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    diagnosed_date DATE NOT NULL,
    result_summary VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_whs_diagnosis_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS whs_skin_diagnosis_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    diagnosis_id BIGINT NOT NULL,
    metric_type VARCHAR(40) NOT NULL,
    user_score DOUBLE,
    average_score DOUBLE,
    grade VARCHAR(30),
    CONSTRAINT fk_whs_metric_diagnosis FOREIGN KEY (diagnosis_id) REFERENCES whs_skin_diagnoses(id),
    CONSTRAINT uk_whs_diagnosis_metric UNIQUE (diagnosis_id, metric_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS self_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    checked_at DATETIME NOT NULL,
    pain VARCHAR(20) NOT NULL,
    heat_sensation VARCHAR(20) NOT NULL,
    tightness VARCHAR(20) NOT NULL,
    dryness VARCHAR(20) NOT NULL,
    itching VARCHAR(20) NOT NULL,
    swelling VARCHAR(20) NOT NULL,
    peeling VARCHAR(20) NOT NULL,
    breakout VARCHAR(20) NOT NULL,
    oozing VARCHAR(20) NOT NULL,
    bleeding VARCHAR(20) NOT NULL,
    barrier_damage VARCHAR(20) NOT NULL,
    note VARCHAR(500),
    CONSTRAINT fk_self_check_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skin_state_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    scoring_version VARCHAR(40) NOT NULL,
    symptom_scores_json LONGTEXT NOT NULL,
    analysis_scores_json LONGTEXT,
    dominant_symptom VARCHAR(40),
    source_self_check_id BIGINT NOT NULL,
    source_analysis_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_snapshot_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_snapshot_user_date UNIQUE (user_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS procedure_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    procedure_date DATE NOT NULL,
    procedure_name VARCHAR(100) NOT NULL,
    care_guide VARCHAR(1000) NOT NULL,
    treatment_code VARCHAR(100),
    treatment_type VARCHAR(100),
    treatment_area VARCHAR(100),
    expected_recovery_days_min INT,
    expected_recovery_days_max INT,
    treatment_source VARCHAR(100),
    treatment_rule_version VARCHAR(50),
    CONSTRAINT fk_procedure_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_procedure_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS procedure_normal_symptoms (
    procedure_record_id BIGINT NOT NULL,
    symptom VARCHAR(500),
    CONSTRAINT fk_normal_symptoms_procedure FOREIGN KEY (procedure_record_id) REFERENCES procedure_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS procedure_warning_symptoms (
    procedure_record_id BIGINT NOT NULL,
    symptom VARCHAR(500),
    CONSTRAINT fk_warning_symptoms_procedure FOREIGN KEY (procedure_record_id) REFERENCES procedure_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS procedure_aftercare_restrictions (
    procedure_record_id BIGINT NOT NULL,
    restriction_text VARCHAR(500),
    CONSTRAINT fk_aftercare_restrictions_procedure FOREIGN KEY (procedure_record_id) REFERENCES procedure_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS procedure_allowed_product_tags (
    procedure_record_id BIGINT NOT NULL,
    product_tag VARCHAR(100),
    CONSTRAINT fk_allowed_tags_procedure FOREIGN KEY (procedure_record_id) REFERENCES procedure_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS procedure_restricted_product_tags (
    procedure_record_id BIGINT NOT NULL,
    product_tag VARCHAR(100),
    CONSTRAINT fk_restricted_tags_procedure FOREIGN KEY (procedure_record_id) REFERENCES procedure_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS care_cycles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    skin_analysis_id BIGINT NOT NULL UNIQUE,
    self_check_id BIGINT,
    origin_capture_date DATE NOT NULL,
    evening_care_date DATE NOT NULL,
    morning_care_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_care_cycle_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_care_cycle_analysis FOREIGN KEY (skin_analysis_id) REFERENCES skin_analyses(id),
    CONSTRAINT fk_care_cycle_self_check FOREIGN KEY (self_check_id) REFERENCES self_checks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS care_solutions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    care_cycle_id BIGINT NOT NULL UNIQUE,
    whs_diagnosis_id BIGINT,
    procedure_record_id BIGINT,
    season VARCHAR(20) NOT NULL,
    safety_level VARCHAR(20) NOT NULL,
    headline VARCHAR(200) NOT NULL,
    evening_steps_json LONGTEXT NOT NULL,
    morning_steps_json LONGTEXT NOT NULL,
    safety_message VARCHAR(1000),
    primary_concern VARCHAR(30),
    generated_at DATETIME NOT NULL,
    CONSTRAINT fk_care_solution_cycle FOREIGN KEY (care_cycle_id) REFERENCES care_cycles(id),
    CONSTRAINT fk_care_solution_diagnosis FOREIGN KEY (whs_diagnosis_id) REFERENCES whs_skin_diagnoses(id),
    CONSTRAINT fk_care_solution_procedure FOREIGN KEY (procedure_record_id) REFERENCES procedure_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS care_completions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    care_solution_id BIGINT NOT NULL,
    served_date DATE NOT NULL,
    phase VARCHAR(20) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_care_completion_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_care_completion_solution FOREIGN KEY (care_solution_id) REFERENCES care_solutions(id),
    CONSTRAINT uk_care_completion_user_date_phase UNIQUE (user_id, served_date, phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
