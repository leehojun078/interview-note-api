-- Phase 8A: AI 채팅 면접 종합 평가 점수 계산 개선
-- Author: Claude + Hojun
-- Date: 2026-05-04
-- Description: 경력 수준 및 가중 평균 점수 지원 추가
-- Note: H2 및 PostgreSQL 호환 버전

-- =============================================================================
-- 1. 경력 수준 컬럼 추가
-- =============================================================================
-- 경력 수준: ENTRY(신입, 1년 미만), JUNIOR(주니어, 1-3년),
--            SENIOR(시니어, 3-7년), SENIOR_PLUS(시니어+, 7년 이상)

ALTER TABLE mock_interviews
ADD COLUMN career_level VARCHAR(20);

-- =============================================================================
-- 2. 가중 평균 점수 컬럼 추가
-- =============================================================================
-- 가중 평균 점수 (첫 답변 제외, 저품질 답변 50% 이상 시 0.8 패널티 적용)

ALTER TABLE mock_interviews
ADD COLUMN weighted_average_score DOUBLE PRECISION;

-- =============================================================================
-- 3. 점수 계산 방법 컬럼 추가
-- =============================================================================
-- 점수 계산 방법: SIMPLE_AVERAGE(Phase 7 기존 방식),
--                WEIGHTED_AVERAGE(Phase 8 신규 방식)

ALTER TABLE mock_interviews
ADD COLUMN score_calculation_method VARCHAR(50) DEFAULT 'WEIGHTED_AVERAGE';

-- 기존 레코드는 SIMPLE_AVERAGE로 설정 (Phase 7 기준)
UPDATE mock_interviews
SET score_calculation_method = 'SIMPLE_AVERAGE'
WHERE score_calculation_method = 'WEIGHTED_AVERAGE';

-- =============================================================================
-- 4. 인덱스 추가 (H2/PostgreSQL 공통)
-- =============================================================================

-- 경력 수준별 통계 조회 최적화
CREATE INDEX idx_mock_interviews_career_level
ON mock_interviews(career_level);

-- 완료된 면접 중 가중 평균 점수로 정렬 조회 최적화
CREATE INDEX idx_mock_interviews_weighted_score
ON mock_interviews(weighted_average_score);

-- =============================================================================
-- 5. 제약 조건 추가 (H2/PostgreSQL 공통)
-- =============================================================================

-- 경력 수준 값 제약
ALTER TABLE mock_interviews
ADD CONSTRAINT chk_career_level
CHECK (career_level IS NULL OR career_level IN ('ENTRY', 'JUNIOR', 'SENIOR', 'SENIOR_PLUS'));

-- 가중 평균 점수 범위 제약 (1.0 ~ 5.0)
ALTER TABLE mock_interviews
ADD CONSTRAINT chk_weighted_average_score_range
CHECK (weighted_average_score IS NULL OR (weighted_average_score >= 1.0 AND weighted_average_score <= 5.0));

-- 점수 계산 방법 값 제약
ALTER TABLE mock_interviews
ADD CONSTRAINT chk_score_calculation_method
CHECK (score_calculation_method IN ('SIMPLE_AVERAGE', 'WEIGHTED_AVERAGE'));

-- =============================================================================
-- Migration Complete
-- =============================================================================
