-- Phase 5: Insert multi-job field questions
-- 17개 직무별 20개 질문 (총 340개)
-- 각 직무: EASY 5개, MEDIUM 10개, HARD 5개

-- ==============================================
-- 1. 영업·판매·무역 (SALES) - 20개
-- ==============================================

-- 고객관리 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SALES', '영업관리자', '고객관리', '고객과의 첫 미팅에서 가장 중요하게 생각하는 것은 무엇인가요?', 'EASY'),
('SALES', '영업관리자', '고객관리', '고객 불만을 처리한 경험이 있나요? 어떻게 해결했나요?', 'EASY'),
('SALES', '영업관리자', '고객관리', 'CRM 시스템을 사용한 경험이 있나요?', 'EASY');

-- 실적달성 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SALES', '영업관리자', '실적달성', '목표 달성률이 가장 높았던 달은 언제였나요? 어떤 전략을 사용했나요?', 'EASY'),
('SALES', '영업관리자', '실적달성', '영업 실적을 추적하고 관리하는 본인만의 방법이 있나요?', 'EASY');

-- 고객관리 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SALES', '영업관리자', '고객관리', '장기 고객과의 관계를 유지하기 위해 어떤 노력을 하나요?', 'MEDIUM'),
('SALES', '영업관리자', '고객관리', '거절하는 고객을 설득한 성공 사례를 구체적으로 설명해주세요.', 'MEDIUM'),
('SALES', '영업관리자', '고객관리', 'B2B와 B2C 영업의 차이점과 각각의 전략을 설명해주세요.', 'MEDIUM'),
('SALES', '영업관리자', '고객관리', '고객 이탈을 방지하기 위해 시행한 조치가 있나요?', 'MEDIUM');

-- 실적달성 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SALES', '영업관리자', '실적달성', '목표 미달성 시 어떻게 대응하나요? 구체적인 사례를 들어주세요.', 'MEDIUM'),
('SALES', '영업관리자', '실적달성', '신규 시장이나 신제품 런칭 시 영업 전략을 어떻게 세우나요?', 'MEDIUM'),
('SALES', '영업관리자', '실적달성', '경쟁사 대비 우위를 확보하기 위해 어떤 전략을 사용하나요?', 'MEDIUM'),
('SALES', '영업관리자', '실적달성', '분기별 목표를 초과 달성한 경험이 있나요? 어떤 방법을 사용했나요?', 'MEDIUM');

-- 협상스킬 - MEDIUM (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SALES', '영업관리자', '협상스킬', '가격 협상에서 win-win을 만든 사례를 설명해주세요.', 'MEDIUM'),
('SALES', '영업관리자', '협상스킬', '계약 체결 직전 고객이 마음을 바꿨을 때 어떻게 대응하나요?', 'MEDIUM');

-- 고객관리 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SALES', '영업관리자', '고객관리', '대형 고객사(Enterprise)를 유치한 경험이 있나요? 프로세스를 설명해주세요.', 'HARD'),
('SALES', '영업관리자', '고객관리', '고객 생애 가치(LTV)를 극대화하기 위한 전략은 무엇인가요?', 'HARD');

-- 실적달성 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SALES', '영업관리자', '실적달성', '매출 목표 200% 달성을 위한 구체적인 실행 계획을 수립해주세요.', 'HARD'),
('SALES', '영업관리자', '실적달성', '시장 점유율을 3배 증가시킨 사례가 있나요? 전략과 실행 과정을 설명해주세요.', 'HARD');

-- 협상스킬 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SALES', '영업관리자', '협상스킬', '다수의 이해관계자가 있는 복잡한 계약을 성사시킨 경험을 설명해주세요.', 'HARD');


-- ==============================================
-- 2. 회계·세무·재무 (ACCOUNTING) - 20개
-- ==============================================

-- 재무분석 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ACCOUNTING', '회계담당자', '재무분석', '재무제표의 3대 요소는 무엇인가요?', 'EASY'),
('ACCOUNTING', '회계담당자', '재무분석', '유동비율과 당좌비율의 차이를 설명해주세요.', 'EASY'),
('ACCOUNTING', '회계담당자', '재무분석', 'Excel을 사용한 재무 데이터 분석 경험이 있나요?', 'EASY');

-- 세무지식 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ACCOUNTING', '회계담당자', '세무지식', '부가가치세 신고 주기는 어떻게 되나요?', 'EASY'),
('ACCOUNTING', '회계담당자', '세무지식', '법인세와 소득세의 차이를 간단히 설명해주세요.', 'EASY');

-- 재무분석 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ACCOUNTING', '회계담당자', '재무분석', '손익분기점(BEP) 분석을 수행한 경험이 있나요?', 'MEDIUM'),
('ACCOUNTING', '회계담당자', '재무분석', '현금흐름표(Cash Flow Statement)를 작성하고 분석한 사례를 설명해주세요.', 'MEDIUM'),
('ACCOUNTING', '회계담당자', '재무분석', 'ROI(투자수익률)와 ROE(자기자본이익률)의 차이와 활용법을 설명해주세요.', 'MEDIUM'),
('ACCOUNTING', '회계담당자', '재무분석', '재무 건전성을 평가하기 위해 어떤 지표를 주로 사용하나요?', 'MEDIUM');

-- 세무지식 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ACCOUNTING', '회계담당자', '세무지식', '세무 조사를 대응한 경험이 있나요? 어떻게 준비했나요?', 'MEDIUM'),
('ACCOUNTING', '회계담당자', '세무지식', '연말정산 업무를 처리한 경험을 구체적으로 설명해주세요.', 'MEDIUM'),
('ACCOUNTING', '회계담당자', '세무지식', '절세 전략을 제안하고 실행한 사례가 있나요?', 'MEDIUM');

-- 리스크관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ACCOUNTING', '회계담당자', '리스크관리', '재무 리스크를 사전에 감지하고 대응한 경험이 있나요?', 'MEDIUM'),
('ACCOUNTING', '회계담당자', '리스크관리', '내부 통제 시스템 구축에 참여한 경험을 설명해주세요.', 'MEDIUM'),
('ACCOUNTING', '회계담당자', '리스크관리', '회계 오류를 발견하고 수정한 사례를 말씀해주세요.', 'MEDIUM');

-- 재무분석 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ACCOUNTING', '회계담당자', '재무분석', 'M&A 실사(Due Diligence) 과정에 참여한 경험이 있나요?', 'HARD'),
('ACCOUNTING', '회계담당자', '재무분석', '재무 모델링을 통해 사업 타당성을 분석한 사례를 설명해주세요.', 'HARD');

-- 세무지식 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ACCOUNTING', '회계담당자', '세무지식', '국제 세무(이전가격, 외환 거래 등)에 대한 경험이 있나요?', 'HARD'),
('ACCOUNTING', '회계담당자', '세무지식', '복잡한 세무 이슈를 해결하고 세금을 절감한 사례를 설명해주세요.', 'HARD');

-- 리스크관리 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ACCOUNTING', '회계담당자', '리스크관리', '재무 위기 상황에서 회사의 재무 구조를 개선한 경험을 설명해주세요.', 'HARD');


-- ==============================================
-- 3. 마케팅·홍보·조사 (MARKETING) - 20개
-- ==============================================

-- 캠페인기획 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MARKETING', '마케팅매니저', '캠페인기획', '마케팅 캠페인 기획 시 가장 중요하게 고려하는 요소는 무엇인가요?', 'EASY'),
('MARKETING', '마케팅매니저', '캠페인기획', 'SNS(소셜미디어) 마케팅 경험이 있나요?', 'EASY'),
('MARKETING', '마케팅매니저', '캠페인기획', '타겟 고객(Target Audience)을 어떻게 정의하나요?', 'EASY');

-- 데이터분석 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MARKETING', '마케팅매니저', '데이터분석', 'Google Analytics나 유사한 분석 도구를 사용한 경험이 있나요?', 'EASY'),
('MARKETING', '마케팅매니저', '데이터분석', 'CTR(클릭률)과 CVR(전환율)의 차이를 설명해주세요.', 'EASY');

-- 캠페인기획 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MARKETING', '마케팅매니저', '캠페인기획', '성공적인 마케팅 캠페인 사례를 하나 들어주세요. 목표와 성과는?', 'MEDIUM'),
('MARKETING', '마케팅매니저', '캠페인기획', '제한된 예산으로 효과적인 마케팅을 한 경험을 설명해주세요.', 'MEDIUM'),
('MARKETING', '마케팅매니저', '캠페인기획', '브랜드 인지도를 높이기 위해 어떤 전략을 사용하나요?', 'MEDIUM'),
('MARKETING', '마케팅매니저', '캠페인기획', '온라인과 오프라인 마케팅을 통합한 O2O 전략을 수립한 경험이 있나요?', 'MEDIUM');

-- 데이터분석 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MARKETING', '마케팅매니저', '데이터분석', 'A/B 테스트를 통해 마케팅 성과를 개선한 사례를 설명해주세요.', 'MEDIUM'),
('MARKETING', '마케팅매니저', '데이터분석', '고객 세분화(Segmentation)를 어떻게 수행하나요?', 'MEDIUM'),
('MARKETING', '마케팅매니저', '데이터분석', 'ROI(투자수익률)를 측정하고 개선한 경험이 있나요?', 'MEDIUM');

-- 콘텐츠전략 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MARKETING', '마케팅매니저', '콘텐츠전략', '바이럴 콘텐츠를 제작하고 확산시킨 경험을 설명해주세요.', 'MEDIUM'),
('MARKETING', '마케팅매니저', '콘텐츠전략', '인플루언서 마케팅을 진행한 경험이 있나요? 성과는?', 'MEDIUM'),
('MARKETING', '마케팅매니저', '콘텐츠전략', 'SEO(검색엔진 최적화)를 통해 트래픽을 증가시킨 사례를 말씀해주세요.', 'MEDIUM');

-- 캠페인기획 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MARKETING', '마케팅매니저', '캠페인기획', '신제품 출시를 위한 통합 마케팅 전략(IMC)을 수립한 경험을 설명해주세요.', 'HARD'),
('MARKETING', '마케팅매니저', '캠페인기획', '글로벌 시장 진출을 위한 마케팅 전략을 기획한 사례가 있나요?', 'HARD');

-- 데이터분석 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MARKETING', '마케팅매니저', '데이터분석', '빅데이터 분석을 활용한 마케팅 의사결정 사례를 설명해주세요.', 'HARD'),
('MARKETING', '마케팅매니저', '데이터분석', '고객 여정(Customer Journey) 분석을 통해 전환율을 3배 높인 사례를 설명해주세요.', 'HARD');

-- 콘텐츠전략 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MARKETING', '마케팅매니저', '콘텐츠전략', '브랜드 리포지셔닝을 통해 시장 점유율을 회복한 경험을 설명해주세요.', 'HARD');


-- ==============================================
-- 4. 기획·전략 (PLANNING) - 20개
-- ==============================================

-- 전략수립 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PLANNING', '경영기획자', '전략수립', '회사의 비전과 미션을 어떻게 이해하고 있나요?', 'EASY'),
('PLANNING', '경영기획자', '전략수립', 'SWOT 분석을 수행한 경험이 있나요?', 'EASY'),
('PLANNING', '경영기획자', '전략수립', '사업 계획서를 작성한 경험을 간단히 설명해주세요.', 'EASY');

-- 시장분석 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PLANNING', '경영기획자', '시장분석', '시장 조사 방법론에는 어떤 것들이 있나요?', 'EASY'),
('PLANNING', '경영기획자', '시장분석', '경쟁사 분석을 위해 주로 어떤 정보를 수집하나요?', 'EASY');

-- 전략수립 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PLANNING', '경영기획자', '전략수립', '중장기 전략을 수립한 경험을 구체적으로 설명해주세요.', 'MEDIUM'),
('PLANNING', '경영기획자', '전략수립', '신규 사업 진출을 위한 타당성 분석을 수행한 사례가 있나요?', 'MEDIUM'),
('PLANNING', '경영기획자', '전략수립', 'KPI를 설정하고 성과를 관리한 경험을 설명해주세요.', 'MEDIUM'),
('PLANNING', '경영기획자', '전략수립', '조직 개편이나 구조 조정 프로젝트에 참여한 경험이 있나요?', 'MEDIUM');

-- 시장분석 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PLANNING', '경영기획자', '시장분석', '시장 트렌드 분석을 통해 새로운 기회를 발굴한 사례를 말씀해주세요.', 'MEDIUM'),
('PLANNING', '경영기획자', '시장분석', '고객 인사이트를 도출하기 위해 어떤 방법을 사용하나요?', 'MEDIUM'),
('PLANNING', '경영기획자', '시장분석', '데이터 기반 의사결정을 위해 어떤 도구나 방법론을 활용하나요?', 'MEDIUM');

-- 프로젝트관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PLANNING', '경영기획자', '프로젝트관리', '전사 프로젝트를 기획하고 실행한 경험을 설명해주세요.', 'MEDIUM'),
('PLANNING', '경영기획자', '프로젝트관리', '이해관계자 관리(Stakeholder Management)에서 어려웠던 점은?', 'MEDIUM'),
('PLANNING', '경영기획자', '프로젝트관리', '예산 대비 성과를 극대화한 프로젝트 사례가 있나요?', 'MEDIUM');

-- 전략수립 - HARD (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PLANNING', '경영기획자', '전략수립', '회사의 디지털 전환(Digital Transformation) 전략을 수립한 경험이 있나요?', 'HARD'),
('PLANNING', '경영기획자', '전략수립', 'M&A 후 통합(PMI) 프로젝트에 참여한 경험을 설명해주세요.', 'HARD'),
('PLANNING', '경영기획자', '전략수립', '글로벌 시장 진출 전략을 수립하고 실행한 사례를 설명해주세요.', 'HARD');

-- 시장분석 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PLANNING', '경영기획자', '시장분석', '산업 패러다임 변화를 예측하고 선제적으로 대응한 사례가 있나요?', 'HARD'),
('PLANNING', '경영기획자', '시장분석', '블루오션 전략을 통해 새로운 시장을 개척한 경험을 설명해주세요.', 'HARD');


-- ==============================================
-- 5. 인사·노무·HRD (HR) - 20개
-- ==============================================

-- 채용관리 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('HR', '인사담당자', '채용관리', '채용 공고를 작성할 때 가장 중요하게 생각하는 요소는 무엇인가요?', 'EASY'),
('HR', '인사담당자', '채용관리', '면접관으로 참여한 경험이 있나요? 어떤 질문을 주로 하나요?', 'EASY'),
('HR', '인사담당자', '채용관리', 'ATS(채용관리시스템)를 사용한 경험이 있나요?', 'EASY');

-- 인사평가 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('HR', '인사담당자', '인사평가', '인사 평가 제도의 종류에는 어떤 것들이 있나요?', 'EASY'),
('HR', '인사담당자', '인사평가', '360도 피드백의 장단점을 설명해주세요.', 'EASY');

-- 채용관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('HR', '인사담당자', '채용관리', '우수 인재를 유치하기 위해 어떤 전략을 사용하나요?', 'MEDIUM'),
('HR', '인사담당자', '채용관리', '채용 프로세스를 개선하여 채용 기간을 단축한 사례가 있나요?', 'MEDIUM'),
('HR', '인사담당자', '채용관리', '헤드헌팅이나 레퍼럴 채용을 진행한 경험을 설명해주세요.', 'MEDIUM');

-- 인사평가 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('HR', '인사담당자', '인사평가', '성과 관리 시스템(OKR, MBO 등)을 운영한 경험이 있나요?', 'MEDIUM'),
('HR', '인사담당자', '인사평가', '평가 결과에 불만을 제기하는 직원을 어떻게 대응하나요?', 'MEDIUM'),
('HR', '인사담당자', '인사평가', '역량 평가 체계를 구축하거나 개선한 경험을 설명해주세요.', 'MEDIUM'),
('HR', '인사담당자', '인사평가', '저성과자 관리 프로세스를 어떻게 운영하나요?', 'MEDIUM');

-- 교육훈련 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('HR', '인사담당자', '교육훈련', '신입사원 온보딩 프로그램을 설계한 경험이 있나요?', 'MEDIUM'),
('HR', '인사담당자', '교육훈련', '리더십 교육 프로그램을 기획하고 운영한 사례를 말씀해주세요.', 'MEDIUM'),
('HR', '인사담당자', '교육훈련', '교육 효과성을 측정하고 개선한 경험을 설명해주세요.', 'MEDIUM');

-- 채용관리 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('HR', '인사담당자', '채용관리', '대규모 조직 확장 시 채용 전략을 수립하고 실행한 경험이 있나요?', 'HARD'),
('HR', '인사담당자', '채용관리', '고급 인력(임원급) 채용 프로젝트를 이끈 경험을 설명해주세요.', 'HARD');

-- 인사평가 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('HR', '인사담당자', '인사평가', '조직 문화 개선을 위한 인사 제도 혁신을 주도한 경험이 있나요?', 'HARD'),
('HR', '인사담당자', '인사평가', '보상 체계를 재설계하여 조직 성과를 향상시킨 사례를 설명해주세요.', 'HARD');

-- 교육훈련 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('HR', '인사담당자', '교육훈련', 'HRD 전략을 수립하고 인재 육성 체계를 구축한 경험을 설명해주세요.', 'HARD');


-- ==============================================
-- 6. 총무·법무·사무 (ADMIN) - 20개
-- ==============================================

-- 사무관리 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ADMIN', '총무담당자', '사무관리', '사무실 관리에서 가장 중요하게 생각하는 부분은 무엇인가요?', 'EASY'),
('ADMIN', '총무담당자', '사무관리', '문서 관리 시스템을 운영한 경험이 있나요?', 'EASY'),
('ADMIN', '총무담당자', '사무관리', 'Excel이나 Office 프로그램 활용 능력은 어느 정도인가요?', 'EASY');

-- 계약관리 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ADMIN', '총무담당자', '계약관리', '계약서 검토 시 주로 어떤 항목을 확인하나요?', 'EASY'),
('ADMIN', '총무담당자', '계약관리', '협력업체 관리 경험이 있나요?', 'EASY');

-- 사무관리 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ADMIN', '총무담당자', '사무관리', '사무실 이전 프로젝트를 진행한 경험을 설명해주세요.', 'MEDIUM'),
('ADMIN', '총무담당자', '사무관리', '비용 절감을 위해 어떤 노력을 하셨나요? 구체적인 사례는?', 'MEDIUM'),
('ADMIN', '총무담당자', '사무관리', '복리후생 제도를 개선하거나 신규 도입한 경험이 있나요?', 'MEDIUM'),
('ADMIN', '총무담당자', '사무관리', '행사나 회의 운영을 담당한 경험을 설명해주세요.', 'MEDIUM');

-- 계약관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ADMIN', '총무담당자', '계약관리', '계약 분쟁을 해결한 경험이 있나요? 어떻게 대응했나요?', 'MEDIUM'),
('ADMIN', '총무담당자', '계약관리', '협상을 통해 유리한 조건을 이끌어낸 사례를 말씀해주세요.', 'MEDIUM'),
('ADMIN', '총무담당자', '계약관리', '계약 관리 프로세스를 개선한 경험을 설명해주세요.', 'MEDIUM');

-- 법률검토 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ADMIN', '총무담당자', '법률검토', '법률 리스크를 사전에 파악하고 대응한 경험이 있나요?', 'MEDIUM'),
('ADMIN', '총무담당자', '법률검토', '개인정보 보호법이나 근로기준법 준수를 위해 어떤 조치를 취하나요?', 'MEDIUM'),
('ADMIN', '총무담당자', '법률검토', '외부 법률 자문을 활용한 경험을 설명해주세요.', 'MEDIUM');

-- 사무관리 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ADMIN', '총무담당자', '사무관리', '전사 자원 관리(ERP, 자산관리) 시스템 도입을 주도한 경험이 있나요?', 'HARD'),
('ADMIN', '총무담당자', '사무관리', '위기 상황(재해, 사고 등) 대응 매뉴얼을 수립하고 실행한 사례가 있나요?', 'HARD');

-- 계약관리 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ADMIN', '총무담당자', '계약관리', '대규모 계약(M&A, 부동산 등)을 성사시킨 경험을 설명해주세요.', 'HARD'),
('ADMIN', '총무담당자', '계약관리', '글로벌 계약이나 해외 법인 관리 경험이 있나요?', 'HARD');

-- 법률검토 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('ADMIN', '총무담당자', '법률검토', '법률 소송이나 분쟁을 해결하고 회사 손실을 최소화한 사례를 설명해주세요.', 'HARD');


-- ==============================================
-- 7. 디자인 (DESIGN) - 20개
-- ==============================================

-- 디자인기획 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('DESIGN', '디자이너', '디자인기획', '디자인 작업 시 가장 중요하게 생각하는 원칙은 무엇인가요?', 'EASY'),
('DESIGN', '디자이너', '디자인기획', 'UI와 UX의 차이를 설명해주세요.', 'EASY'),
('DESIGN', '디자이너', '디자인기획', '디자인 트렌드를 파악하기 위해 어떤 노력을 하나요?', 'EASY');

-- 툴활용 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('DESIGN', '디자이너', '툴활용', '주로 사용하는 디자인 툴은 무엇인가요? (Figma, Sketch, Adobe 등)', 'EASY'),
('DESIGN', '디자이너', '툴활용', '프로토타이핑 도구를 사용한 경험이 있나요?', 'EASY');

-- 디자인기획 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('DESIGN', '디자이너', '디자인기획', '브랜드 아이덴티티를 디자인한 경험을 설명해주세요.', 'MEDIUM'),
('DESIGN', '디자이너', '디자인기획', '사용자 리서치를 통해 디자인 방향을 개선한 사례가 있나요?', 'MEDIUM'),
('DESIGN', '디자이너', '디자인기획', '반응형 웹 디자인(Responsive Design)을 구현한 경험을 말씀해주세요.', 'MEDIUM'),
('DESIGN', '디자이너', '디자인기획', '디자인 시스템을 구축하거나 개선한 경험이 있나요?', 'MEDIUM');

-- 협업소통 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('DESIGN', '디자이너', '협업소통', '개발자나 기획자와 협업할 때 어려웠던 점과 해결 방법은?', 'MEDIUM'),
('DESIGN', '디자이너', '협업소통', '클라이언트나 이해관계자의 피드백을 반영한 경험을 설명해주세요.', 'MEDIUM'),
('DESIGN', '디자이너', '협업소통', '디자인 의도를 효과적으로 설명하고 설득한 사례가 있나요?', 'MEDIUM');

-- 포트폴리오 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('DESIGN', '디자이너', '포트폴리오', '가장 자신 있는 디자인 작업물을 소개해주세요.', 'MEDIUM'),
('DESIGN', '디자이너', '포트폴리오', '디자인 실패 사례와 거기서 배운 점은 무엇인가요?', 'MEDIUM'),
('DESIGN', '디자이너', '포트폴리오', '짧은 시간 안에 퀄리티 있는 결과물을 낸 경험을 설명해주세요.', 'MEDIUM');

-- 디자인기획 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('DESIGN', '디자이너', '디자인기획', '제품/서비스의 UX를 개선하여 전환율을 크게 높인 사례를 설명해주세요.', 'HARD'),
('DESIGN', '디자이너', '디자인기획', '브랜드 리뉴얼 프로젝트를 리드한 경험이 있나요?', 'HARD');

-- 협업소통 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('DESIGN', '디자이너', '협업소통', '다수의 이해관계자가 있는 프로젝트에서 디자인 방향을 조율한 경험은?', 'HARD'),
('DESIGN', '디자이너', '협업소통', '디자인 조직을 구축하거나 운영한 경험이 있나요?', 'HARD');

-- 포트폴리오 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('DESIGN', '디자이너', '포트폴리오', '혁신적인 디자인으로 비즈니스 성과를 만든 사례를 설명해주세요.', 'HARD');


-- ==============================================
-- 8. 상품기획·MD (MD) - 20개
-- ==============================================

-- 상품기획 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MD', '상품기획자', '상품기획', '상품 기획 시 가장 중요하게 고려하는 요소는 무엇인가요?', 'EASY'),
('MD', '상품기획자', '상품기획', '상품 라이프사이클에 대해 설명해주세요.', 'EASY'),
('MD', '상품기획자', '상품기획', '상품 기획서를 작성한 경험이 있나요?', 'EASY');

-- 트렌드분석 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MD', '상품기획자', '트렌드분석', '최근 관심 있는 상품 트렌드는 무엇인가요?', 'EASY'),
('MD', '상품기획자', '트렌드분석', '시장 조사를 위해 주로 어떤 방법을 사용하나요?', 'EASY');

-- 상품기획 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MD', '상품기획자', '상품기획', '신상품을 기획하고 출시한 경험을 구체적으로 설명해주세요.', 'MEDIUM'),
('MD', '상품기획자', '상품기획', '상품 포트폴리오를 최적화한 경험이 있나요?', 'MEDIUM'),
('MD', '상품기획자', '상품기획', '상품 가격 전략을 수립한 사례를 말씀해주세요.', 'MEDIUM'),
('MD', '상품기획자', '상품기획', '시즌별 상품 구성을 어떻게 계획하나요?', 'MEDIUM');

-- 트렌드분석 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MD', '상품기획자', '트렌드분석', '고객 데이터 분석을 통해 상품 기획에 반영한 사례가 있나요?', 'MEDIUM'),
('MD', '상품기획자', '트렌드분석', '경쟁 상품 분석을 어떻게 수행하나요?', 'MEDIUM'),
('MD', '상품기획자', '트렌드분석', '소비자 니즈를 파악하기 위해 어떤 방법을 사용하나요?', 'MEDIUM');

-- 재고관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MD', '상품기획자', '재고관리', '재고 회전율을 개선한 경험을 설명해주세요.', 'MEDIUM'),
('MD', '상품기획자', '재고관리', '시즌 종료 시 재고 소진 전략은 무엇인가요?', 'MEDIUM'),
('MD', '상품기획자', '재고관리', '품절이나 과재고 상황을 어떻게 대응하나요?', 'MEDIUM');

-- 상품기획 - HARD (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MD', '상품기획자', '상품기획', '히트 상품을 개발하여 매출을 크게 증가시킨 경험이 있나요?', 'HARD'),
('MD', '상품기획자', '상품기획', '신규 카테고리를 론칭하고 성공시킨 사례를 설명해주세요.', 'HARD'),
('MD', '상품기획자', '상품기획', '글로벌 시장을 타겟으로 한 상품 기획 경험이 있나요?', 'HARD');

-- 트렌드분석 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MD', '상품기획자', '트렌드분석', '빅데이터 분석을 활용한 상품 기획 사례를 설명해주세요.', 'HARD'),
('MD', '상품기획자', '트렌드분석', '트렌드를 선도하는 혁신적인 상품을 기획한 경험이 있나요?', 'HARD');


-- ==============================================
-- 9. 서비스 (SERVICE) - 20개
-- ==============================================

-- 고객응대 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SERVICE', '고객서비스담당자', '고객응대', '고객 응대 시 가장 중요하게 생각하는 것은 무엇인가요?', 'EASY'),
('SERVICE', '고객서비스담당자', '고객응대', '어려운 고객을 응대한 경험이 있나요?', 'EASY'),
('SERVICE', '고객서비스담당자', '고객응대', '고객 만족도를 높이기 위해 어떤 노력을 하나요?', 'EASY');

-- 문제해결 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SERVICE', '고객서비스담당자', '문제해결', '고객 불만을 처리한 경험을 설명해주세요.', 'EASY'),
('SERVICE', '고객서비스담당자', '문제해결', 'CS 시스템이나 도구를 사용한 경험이 있나요?', 'EASY');

-- 고객응대 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SERVICE', '고객서비스담당자', '고객응대', '화난 고객을 진정시키고 문제를 해결한 사례를 말씀해주세요.', 'MEDIUM'),
('SERVICE', '고객서비스담당자', '고객응대', '멀티채널(전화, 이메일, 채팅 등) 고객 응대 경험이 있나요?', 'MEDIUM'),
('SERVICE', '고객서비스담당자', '고객응대', '고객의 요구사항이 불합리할 때 어떻게 대응하나요?', 'MEDIUM'),
('SERVICE', '고객서비스담당자', '고객응대', 'VIP 고객을 응대한 경험을 설명해주세요.', 'MEDIUM');

-- 문제해결 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SERVICE', '고객서비스담당자', '문제해결', '복잡한 문제를 신속하게 해결한 사례가 있나요?', 'MEDIUM'),
('SERVICE', '고객서비스담당자', '문제해결', '시스템 오류나 장애 상황에서 고객을 응대한 경험은?', 'MEDIUM'),
('SERVICE', '고객서비스담당자', '문제해결', '고객 불만을 서비스 개선으로 연결한 사례를 말씀해주세요.', 'MEDIUM');

-- 서비스품질 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SERVICE', '고객서비스담당자', '서비스품질', '고객 만족도(CSAT)를 향상시킨 경험을 설명해주세요.', 'MEDIUM'),
('SERVICE', '고객서비스담당자', '서비스품질', '서비스 프로세스를 개선한 사례가 있나요?', 'MEDIUM'),
('SERVICE', '고객서비스담당자', '서비스품질', '응대 시간을 단축하면서도 품질을 유지한 방법은?', 'MEDIUM');

-- 고객응대 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SERVICE', '고객서비스담당자', '고객응대', '대규모 고객 불만 사태를 수습한 경험이 있나요?', 'HARD'),
('SERVICE', '고객서비스담당자', '고객응대', '고객 서비스 조직을 구축하거나 개편한 경험을 설명해주세요.', 'HARD');

-- 문제해결 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SERVICE', '고객서비스담당자', '문제해결', '위기 상황에서 고객 신뢰를 회복한 사례를 말씀해주세요.', 'HARD'),
('SERVICE', '고객서비스담당자', '문제해결', 'AI 챗봇이나 자동화 시스템 도입 프로젝트에 참여한 경험이 있나요?', 'HARD');

-- 서비스품질 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('SERVICE', '고객서비스담당자', '서비스품질', '고객 충성도를 높여 재구매율을 2배 증가시킨 사례를 설명해주세요.', 'HARD');


-- ==============================================
-- 10. 생산 (PRODUCTION) - 20개
-- ==============================================

-- 공정관리 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PRODUCTION', '생산관리자', '공정관리', '생산 공정 관리에서 가장 중요한 것은 무엇인가요?', 'EASY'),
('PRODUCTION', '생산관리자', '공정관리', '생산 계획을 수립한 경험이 있나요?', 'EASY'),
('PRODUCTION', '생산관리자', '공정관리', '작업 지시서를 작성하거나 관리한 경험을 설명해주세요.', 'EASY');

-- 품질관리 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PRODUCTION', '생산관리자', '품질관리', '품질 검사 절차에 대해 설명해주세요.', 'EASY'),
('PRODUCTION', '생산관리자', '품질관리', '불량품을 발견했을 때 어떻게 처리하나요?', 'EASY');

-- 공정관리 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PRODUCTION', '생산관리자', '공정관리', '생산 효율을 개선한 경험을 구체적으로 설명해주세요.', 'MEDIUM'),
('PRODUCTION', '생산관리자', '공정관리', '납기를 단축하기 위해 어떤 조치를 취한 적이 있나요?', 'MEDIUM'),
('PRODUCTION', '생산관리자', '공정관리', '원자재 조달이나 재고 관리 경험을 말씀해주세요.', 'MEDIUM'),
('PRODUCTION', '생산관리자', '공정관리', '설비 가동률을 높이기 위한 전략은 무엇인가요?', 'MEDIUM');

-- 품질관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PRODUCTION', '생산관리자', '품질관리', '불량률을 낮춘 사례를 구체적으로 설명해주세요.', 'MEDIUM'),
('PRODUCTION', '생산관리자', '품질관리', 'ISO나 품질 인증 취득 프로젝트에 참여한 경험이 있나요?', 'MEDIUM'),
('PRODUCTION', '생산관리자', '품질관리', '품질 이슈로 인한 고객 클레임을 해결한 경험은?', 'MEDIUM');

-- 안전관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PRODUCTION', '생산관리자', '안전관리', '작업장 안전 사고를 예방하기 위해 어떤 노력을 하나요?', 'MEDIUM'),
('PRODUCTION', '생산관리자', '안전관리', '안전 교육이나 캠페인을 진행한 경험이 있나요?', 'MEDIUM'),
('PRODUCTION', '생산관리자', '안전관리', '위험 상황을 발견하고 개선한 사례를 말씀해주세요.', 'MEDIUM');

-- 공정관리 - HARD (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PRODUCTION', '생산관리자', '공정관리', '스마트 팩토리나 자동화 시스템 도입을 주도한 경험이 있나요?', 'HARD'),
('PRODUCTION', '생산관리자', '공정관리', '생산 공정을 재설계하여 생산성을 2배 향상시킨 사례를 설명해주세요.', 'HARD'),
('PRODUCTION', '생산관리자', '공정관리', '린(Lean) 생산 방식이나 6시그마를 도입한 경험이 있나요?', 'HARD');

-- 품질관리 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PRODUCTION', '생산관리자', '품질관리', '품질 혁신을 통해 고객 만족도를 크게 높인 사례가 있나요?', 'HARD'),
('PRODUCTION', '생산관리자', '품질관리', 'TQM(전사적 품질관리) 체계를 구축한 경험을 설명해주세요.', 'HARD');


-- ==============================================
-- 11. 건설·건축 (CONSTRUCTION) - 20개
-- ==============================================

-- 시공관리 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('CONSTRUCTION', '건설현장관리자', '시공관리', '건설 현장 관리에서 가장 중요한 것은 무엇인가요?', 'EASY'),
('CONSTRUCTION', '건설현장관리자', '시공관리', '공정표를 작성하고 관리한 경험이 있나요?', 'EASY'),
('CONSTRUCTION', '건설현장관리자', '시공관리', 'CAD나 BIM 소프트웨어를 사용한 경험이 있나요?', 'EASY');

-- 안전관리 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('CONSTRUCTION', '건설현장관리자', '안전관리', '건설 현장 안전 수칙에는 어떤 것들이 있나요?', 'EASY'),
('CONSTRUCTION', '건설현장관리자', '안전관리', '안전 점검을 수행한 경험을 설명해주세요.', 'EASY');

-- 시공관리 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('CONSTRUCTION', '건설현장관리자', '시공관리', '공기 지연을 만회하기 위해 어떤 조치를 취한 적이 있나요?', 'MEDIUM'),
('CONSTRUCTION', '건설현장관리자', '시공관리', '협력업체나 하청업체 관리 경험을 설명해주세요.', 'MEDIUM'),
('CONSTRUCTION', '건설현장관리자', '시공관리', '자재 조달이나 원가 관리 경험이 있나요?', 'MEDIUM'),
('CONSTRUCTION', '건설현장관리자', '시공관리', '날씨나 외부 요인으로 인한 공사 지연을 어떻게 대응하나요?', 'MEDIUM');

-- 설계검토 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('CONSTRUCTION', '건설현장관리자', '설계검토', '설계 변경이나 VE(가치공학) 제안을 한 경험이 있나요?', 'MEDIUM'),
('CONSTRUCTION', '건설현장관리자', '설계검토', '시공 중 설계 오류를 발견하고 해결한 사례를 말씀해주세요.', 'MEDIUM'),
('CONSTRUCTION', '건설현장관리자', '설계검토', '도면 검토 시 주로 어떤 부분을 확인하나요?', 'MEDIUM');

-- 안전관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('CONSTRUCTION', '건설현장관리자', '안전관리', '중대 재해를 예방한 사례를 구체적으로 설명해주세요.', 'MEDIUM'),
('CONSTRUCTION', '건설현장관리자', '안전관리', '안전 교육 프로그램을 운영한 경험이 있나요?', 'MEDIUM'),
('CONSTRUCTION', '건설현장관리자', '안전관리', '위험 작업에 대한 안전 대책을 수립한 사례를 말씀해주세요.', 'MEDIUM');

-- 시공관리 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('CONSTRUCTION', '건설현장관리자', '시공관리', '대규모 프로젝트(100억 이상)의 현장 관리를 담당한 경험이 있나요?', 'HARD'),
('CONSTRUCTION', '건설현장관리자', '시공관리', '신공법이나 첨단 건설 기술을 적용한 프로젝트 사례를 설명해주세요.', 'HARD');

-- 설계검토 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('CONSTRUCTION', '건설현장관리자', '설계검토', '설계 최적화를 통해 공사비를 절감한 사례가 있나요?', 'HARD'),
('CONSTRUCTION', '건설현장관리자', '설계검토', '친환경 건축이나 그린빌딩 프로젝트 경험을 설명해주세요.', 'HARD');

-- 안전관리 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('CONSTRUCTION', '건설현장관리자', '안전관리', '무재해 현장을 달성하기 위한 안전관리 체계를 구축한 경험이 있나요?', 'HARD');


-- ==============================================
-- 12. 의료 (MEDICAL) - 20개
-- ==============================================

-- 환자케어 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDICAL', '간호사', '환자케어', '환자 케어에서 가장 중요하게 생각하는 것은 무엇인가요?', 'EASY'),
('MEDICAL', '간호사', '환자케어', '바이탈 사인(활력징후) 측정 경험을 설명해주세요.', 'EASY'),
('MEDICAL', '간호사', '환자케어', '환자나 보호자와 소통할 때 주의하는 점은 무엇인가요?', 'EASY');

-- 의료지식 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDICAL', '간호사', '의료지식', '감염 관리 절차에 대해 설명해주세요.', 'EASY'),
('MEDICAL', '간호사', '의료지식', '투약 시 확인해야 할 사항은 무엇인가요?', 'EASY');

-- 환자케어 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDICAL', '간호사', '환자케어', '어려운 환자를 케어한 경험을 구체적으로 설명해주세요.', 'MEDIUM'),
('MEDICAL', '간호사', '환자케어', '통증 관리나 증상 완화를 위해 어떤 조치를 취하나요?', 'MEDIUM'),
('MEDICAL', '간호사', '환자케어', '말기 환자나 임종 케어 경험이 있나요?', 'MEDIUM'),
('MEDICAL', '간호사', '환자케어', '환자 교육이나 건강 상담을 수행한 사례를 말씀해주세요.', 'MEDIUM');

-- 의료지식 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDICAL', '간호사', '의료지식', '의료 장비나 기기를 다룬 경험을 설명해주세요.', 'MEDIUM'),
('MEDICAL', '간호사', '의료지식', '약물 부작용을 발견하고 대응한 사례가 있나요?', 'MEDIUM'),
('MEDICAL', '간호사', '의료지식', 'EMR(전자의무기록) 시스템을 사용한 경험이 있나요?', 'MEDIUM');

-- 응급대응 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDICAL', '간호사', '응급대응', '응급 상황에서 신속하게 대응한 경험을 설명해주세요.', 'MEDIUM'),
('MEDICAL', '간호사', '응급대응', 'CPR(심폐소생술)을 수행한 경험이 있나요?', 'MEDIUM'),
('MEDICAL', '간호사', '응급대응', '환자 상태 악화를 조기 발견하고 조치한 사례를 말씀해주세요.', 'MEDIUM');

-- 환자케어 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDICAL', '간호사', '환자케어', '중환자실(ICU)이나 응급실 근무 경험이 있나요?', 'HARD'),
('MEDICAL', '간호사', '환자케어', '의료진 간 협업을 통해 환자 치료 성과를 높인 사례를 설명해주세요.', 'HARD');

-- 의료지식 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDICAL', '간호사', '의료지식', '전문 간호 분야(마취, 정신, 종양 등) 경험이 있나요?', 'HARD'),
('MEDICAL', '간호사', '의료지식', '의료 질 향상(QI) 프로젝트에 참여한 경험을 설명해주세요.', 'HARD');

-- 응급대응 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDICAL', '간호사', '응급대응', '대량 재해나 응급 상황 대응 훈련에 참여한 경험이 있나요?', 'HARD');


-- ==============================================
-- 13. 교육 (EDUCATION) - 20개
-- ==============================================

-- 교수법 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('EDUCATION', '교사', '교수법', '효과적인 수업을 위해 가장 중요한 것은 무엇이라고 생각하나요?', 'EASY'),
('EDUCATION', '교사', '교수법', '수업 계획서를 작성한 경험이 있나요?', 'EASY'),
('EDUCATION', '교사', '교수법', '교육 자료나 콘텐츠를 제작한 경험을 설명해주세요.', 'EASY');

-- 학생관리 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('EDUCATION', '교사', '학생관리', '학생들과 라포(신뢰관계)를 형성하기 위해 어떤 노력을 하나요?', 'EASY'),
('EDUCATION', '교사', '학생관리', '학생 상담 경험이 있나요?', 'EASY');

-- 교수법 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('EDUCATION', '교사', '교수법', '학생 참여를 높이는 수업 방법을 구체적으로 설명해주세요.', 'MEDIUM'),
('EDUCATION', '교사', '교수법', '온라인 수업이나 블렌디드 러닝 경험이 있나요?', 'MEDIUM'),
('EDUCATION', '교사', '교수법', '학습 부진 학생을 지도한 사례를 말씀해주세요.', 'MEDIUM'),
('EDUCATION', '교사', '교수법', '교육 기술(EdTech)을 활용한 수업 경험을 설명해주세요.', 'MEDIUM');

-- 학생관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('EDUCATION', '교사', '학생관리', '문제 행동 학생을 지도한 경험이 있나요?', 'MEDIUM'),
('EDUCATION', '교사', '학생관리', '학부모 상담이나 소통 경험을 설명해주세요.', 'MEDIUM'),
('EDUCATION', '교사', '학생관리', '학급 운영이나 생활 지도 방식은 무엇인가요?', 'MEDIUM');

-- 교육과정 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('EDUCATION', '교사', '교육과정', '교육과정을 개발하거나 개선한 경험이 있나요?', 'MEDIUM'),
('EDUCATION', '교사', '교육과정', '평가 방법을 다양화하거나 개선한 사례를 말씀해주세요.', 'MEDIUM'),
('EDUCATION', '교사', '교육과정', '교과 외 활동(동아리, 행사 등)을 운영한 경험을 설명해주세요.', 'MEDIUM');

-- 교수법 - HARD (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('EDUCATION', '교사', '교수법', '혁신적인 교수법으로 학업 성취도를 크게 향상시킨 사례가 있나요?', 'HARD'),
('EDUCATION', '교사', '교수법', '프로젝트 기반 학습(PBL)이나 플립러닝을 도입한 경험이 있나요?', 'HARD'),
('EDUCATION', '교사', '교수법', '특수교육이나 영재교육 경험을 설명해주세요.', 'HARD');

-- 학생관리 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('EDUCATION', '교사', '학생관리', '학교 폭력이나 심각한 문제 상황을 해결한 경험이 있나요?', 'HARD'),
('EDUCATION', '교사', '학생관리', '진로 지도나 진학 상담에서 성공적인 결과를 만든 사례를 설명해주세요.', 'HARD');


-- ==============================================
-- 14. 미디어·문화·스포츠 (MEDIA) - 20개
-- ==============================================

-- 콘텐츠제작 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '콘텐츠 제작 시 가장 중요하게 생각하는 것은 무엇인가요?', 'EASY'),
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '영상 편집 도구(프리미어, 파이널컷 등)를 다룰 수 있나요?', 'EASY'),
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '포트폴리오나 대표 작품이 있나요?', 'EASY');

-- 기획운영 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDIA', '콘텐츠제작자', '기획운영', '콘텐츠 기획서를 작성한 경험이 있나요?', 'EASY'),
('MEDIA', '콘텐츠제작자', '기획운영', '촬영 스케줄이나 제작 일정을 관리한 경험을 설명해주세요.', 'EASY');

-- 콘텐츠제작 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '높은 조회수나 반응을 얻은 콘텐츠 사례를 말씀해주세요.', 'MEDIUM'),
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '제한된 예산과 일정 안에서 퀄리티를 유지한 경험이 있나요?', 'MEDIUM'),
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '다양한 플랫폼(유튜브, 인스타그램, TV 등)별 콘텐츠 제작 경험은?', 'MEDIUM'),
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '촬영, 편집, 후반작업 전반을 담당한 프로젝트를 설명해주세요.', 'MEDIUM');

-- 기획운영 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDIA', '콘텐츠제작자', '기획운영', '시즌제 콘텐츠나 시리즈물을 기획하고 운영한 경험이 있나요?', 'MEDIUM'),
('MEDIA', '콘텐츠제작자', '기획운영', '협찬이나 광고 콘텐츠를 제작한 사례를 말씀해주세요.', 'MEDIUM'),
('MEDIA', '콘텐츠제작자', '기획운영', '다양한 파트너(출연자, 스태프, 업체)와 협업한 경험을 설명해주세요.', 'MEDIUM');

-- 트렌드분석 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDIA', '콘텐츠제작자', '트렌드분석', '콘텐츠 트렌드를 어떻게 파악하나요?', 'MEDIUM'),
('MEDIA', '콘텐츠제작자', '트렌드분석', '데이터 분석(조회수, 체류시간 등)을 통해 콘텐츠를 개선한 사례는?', 'MEDIUM'),
('MEDIA', '콘텐츠제작자', '트렌드분석', '타겟 오디언스 분석을 어떻게 수행하나요?', 'MEDIUM');

-- 콘텐츠제작 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '바이럴 콘텐츠를 만들어 수백만 조회수를 달성한 사례가 있나요?', 'HARD'),
('MEDIA', '콘텐츠제작자', '콘텐츠제작', '수상 경력이 있거나 화제가 된 작품을 제작한 경험을 설명해주세요.', 'HARD');

-- 기획운영 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDIA', '콘텐츠제작자', '기획운영', '대규모 프로젝트(영화, 드라마, 대형 이벤트 등)를 기획하고 총괄한 경험이 있나요?', 'HARD'),
('MEDIA', '콘텐츠제작자', '기획운영', '콘텐츠 IP를 개발하여 사업화에 성공한 사례를 설명해주세요.', 'HARD');

-- 트렌드분석 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('MEDIA', '콘텐츠제작자', '트렌드분석', '새로운 포맷이나 장르를 개척하여 트렌드를 선도한 경험이 있나요?', 'HARD');


-- ==============================================
-- 15. 금융·보험 (FINANCE) - 20개
-- ==============================================

-- 금융상품 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('FINANCE', '금융전문가', '금융상품', '주식과 채권의 차이를 설명해주세요.', 'EASY'),
('FINANCE', '금융전문가', '금융상품', '펀드 상품을 고객에게 설명한 경험이 있나요?', 'EASY'),
('FINANCE', '금융전문가', '금융상품', '금융 상품 판매 시 가장 중요하게 생각하는 것은 무엇인가요?', 'EASY');

-- 고객상담 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('FINANCE', '금융전문가', '고객상담', '고객 재무 상담 경험을 간단히 설명해주세요.', 'EASY'),
('FINANCE', '금융전문가', '고객상담', '고객 신뢰를 쌓기 위해 어떤 노력을 하나요?', 'EASY');

-- 금융상품 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('FINANCE', '금융전문가', '금융상품', '고객 니즈에 맞는 금융 상품을 추천한 사례를 말씀해주세요.', 'MEDIUM'),
('FINANCE', '금융전문가', '금융상품', '보험 상품 설계나 판매 경험을 구체적으로 설명해주세요.', 'MEDIUM'),
('FINANCE', '금융전문가', '금융상품', '대출 상담이나 심사 업무를 담당한 경험이 있나요?', 'MEDIUM'),
('FINANCE', '금융전문가', '금융상품', '상품 판매 목표를 달성하거나 초과한 경험을 설명해주세요.', 'MEDIUM');

-- 고객상담 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('FINANCE', '금융전문가', '고객상담', 'VIP 고객이나 법인 고객을 관리한 경험이 있나요?', 'MEDIUM'),
('FINANCE', '금융전문가', '고객상담', '고객 불만이나 민원을 처리한 사례를 말씀해주세요.', 'MEDIUM'),
('FINANCE', '금융전문가', '고객상담', '재무 설계나 자산 관리 상담을 수행한 경험을 설명해주세요.', 'MEDIUM');

-- 리스크관리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('FINANCE', '금융전문가', '리스크관리', '금융 리스크를 평가하고 관리한 경험이 있나요?', 'MEDIUM'),
('FINANCE', '금융전문가', '리스크관리', '부실 채권이나 연체 관리 경험을 설명해주세요.', 'MEDIUM'),
('FINANCE', '금융전문가', '리스크관리', '금융 사고를 예방하거나 대응한 사례가 있나요?', 'MEDIUM');

-- 금융상품 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('FINANCE', '금융전문가', '금융상품', '대규모 자산가의 포트폴리오를 관리한 경험이 있나요?', 'HARD'),
('FINANCE', '금융전문가', '금융상품', '신규 금융 상품을 개발하거나 출시에 참여한 경험을 설명해주세요.', 'HARD');

-- 고객상담 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('FINANCE', '금융전문가', '고객상담', '고객 자산을 10억 이상 증대시킨 성공 사례가 있나요?', 'HARD'),
('FINANCE', '금융전문가', '고객상담', '프라이빗 뱅킹(PB)이나 IB 업무 경험을 설명해주세요.', 'HARD');

-- 리스크관리 - HARD (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('FINANCE', '금융전문가', '리스크관리', '금융 위기 상황에서 고객 자산 손실을 최소화한 경험이 있나요?', 'HARD');


-- ==============================================
-- 16. 공공·복지 (PUBLIC) - 20개
-- ==============================================

-- 정책실행 - EASY (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PUBLIC', '공무원', '정책실행', '공공 정책을 실행할 때 가장 중요한 것은 무엇인가요?', 'EASY'),
('PUBLIC', '공무원', '정책실행', '행정 업무나 공문서 작성 경험이 있나요?', 'EASY'),
('PUBLIC', '공무원', '정책실행', '공공 기관에서 일하고 싶은 이유는 무엇인가요?', 'EASY');

-- 민원처리 - EASY (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PUBLIC', '공무원', '민원처리', '민원인 응대 시 주의할 점은 무엇인가요?', 'EASY'),
('PUBLIC', '공무원', '민원처리', '민원 처리 경험을 간단히 설명해주세요.', 'EASY');

-- 정책실행 - MEDIUM (4개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PUBLIC', '공무원', '정책실행', '공공 사업이나 프로젝트를 진행한 경험을 구체적으로 설명해주세요.', 'MEDIUM'),
('PUBLIC', '공무원', '정책실행', '예산을 편성하거나 집행한 경험이 있나요?', 'MEDIUM'),
('PUBLIC', '공무원', '정책실행', '다른 부서나 기관과 협력한 사례를 말씀해주세요.', 'MEDIUM'),
('PUBLIC', '공무원', '정책실행', '정책 효과를 평가하고 개선한 경험을 설명해주세요.', 'MEDIUM');

-- 민원처리 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PUBLIC', '공무원', '민원처리', '복잡하거나 어려운 민원을 해결한 사례가 있나요?', 'MEDIUM'),
('PUBLIC', '공무원', '민원처리', '민원 처리 프로세스를 개선한 경험을 설명해주세요.', 'MEDIUM'),
('PUBLIC', '공무원', '민원처리', '다수의 민원을 효율적으로 처리한 방법은 무엇인가요?', 'MEDIUM');

-- 사회복지 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PUBLIC', '공무원', '사회복지', '사회복지 서비스를 제공한 경험이 있나요?', 'MEDIUM'),
('PUBLIC', '공무원', '사회복지', '취약 계층을 지원하거나 도운 사례를 말씀해주세요.', 'MEDIUM'),
('PUBLIC', '공무원', '사회복지', '복지 제도를 안내하거나 상담한 경험을 설명해주세요.', 'MEDIUM');

-- 정책실행 - HARD (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PUBLIC', '공무원', '정책실행', '신규 정책을 기획하고 시행한 경험이 있나요?', 'HARD'),
('PUBLIC', '공무원', '정책실행', '지역사회 문제를 해결하기 위한 프로젝트를 주도한 사례를 설명해주세요.', 'HARD'),
('PUBLIC', '공무원', '정책실행', '법령이나 조례 제정에 참여한 경험이 있나요?', 'HARD');

-- 민원처리 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('PUBLIC', '공무원', '민원처리', '대규모 민원이나 집단 민원 사태를 수습한 경험이 있나요?', 'HARD'),
('PUBLIC', '공무원', '민원처리', '시민 참여를 높이는 소통 채널을 구축한 사례를 설명해주세요.', 'HARD');


-- ==============================================
-- 총 340개 질문 완성
-- IT (20개, V2에 존재) + 신규 16개 직무 (320개) = 340개
-- ==============================================
