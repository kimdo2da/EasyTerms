
-- ============================================
-- EasyTerms 데이터베이스 생성
-- ============================================

CREATE DATABASE IF NOT EXISTS easyterms
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE easyterms;


-- ============================================
-- 1. users : 회원 정보
-- ============================================

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- ============================================
-- 2. documents : 계약 문서
-- ============================================

CREATE TABLE documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    title VARCHAR(255) NOT NULL,

    document_type VARCHAR(30) NOT NULL,
    input_type VARCHAR(20) NOT NULL,

    original_filename VARCHAR(255),
    file_path VARCHAR(500),

    content LONGTEXT NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_documents_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_document_type
        CHECK (document_type IN (
            'INSURANCE',
            'LOAN',
            'RENTAL',
            'SUBSCRIPTION',
            'OTHER'
        )),

    CONSTRAINT chk_input_type
        CHECK (input_type IN ('TEXT', 'FILE')),

    INDEX idx_documents_user_created
        (user_id, created_at)
) ENGINE=InnoDB;


-- ============================================
-- 3. analysis_results : AI 분석 결과
-- ============================================

CREATE TABLE analysis_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    document_id BIGINT NOT NULL UNIQUE,

    easy_explanation LONGTEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_analysis_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_analysis_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'COMPLETED',
            'FAILED'
        ))
) ENGINE=InnoDB;


-- ============================================
-- 4. key_conditions : 핵심 계약 조건
-- ============================================

CREATE TABLE key_conditions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    analysis_id BIGINT NOT NULL,

    item_name VARCHAR(100) NOT NULL,
    item_value TEXT NOT NULL,

    source_excerpt TEXT,

    CONSTRAINT fk_conditions_analysis
        FOREIGN KEY (analysis_id)
        REFERENCES analysis_results(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 5. check_items : 확인이 필요한 내용
-- ============================================

CREATE TABLE check_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    analysis_id BIGINT NOT NULL,

    title VARCHAR(255) NOT NULL,
    explanation TEXT NOT NULL,

    source_excerpt TEXT,

    CONSTRAINT fk_check_items_analysis
        FOREIGN KEY (analysis_id)
        REFERENCES analysis_results(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 6. terms : 어려운 용어 설명
-- ============================================

CREATE TABLE terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    analysis_id BIGINT NOT NULL,

    term VARCHAR(100) NOT NULL,
    explanation TEXT NOT NULL,

    source_excerpt TEXT,

    CONSTRAINT fk_terms_analysis
        FOREIGN KEY (analysis_id)
        REFERENCES analysis_results(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 7. chat_messages : 문서 기반 후속 질문
-- ============================================

CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    document_id BIGINT NOT NULL,

    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chat_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_chat_role
        CHECK (role IN ('USER', 'ASSISTANT')),

    INDEX idx_chat_document_created
        (document_id, created_at, id)
) ENGINE=InnoDB;
