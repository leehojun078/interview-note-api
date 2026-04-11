-- 초기 질문 데이터 20개 삽입
-- 카테고리: 기술역량, 문제해결, 협업경험
-- 난이도: EASY, MEDIUM, HARD

-- 기술역량 - EASY (5개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('IT', '백엔드 개발자', '기술역량', 'HTTP와 HTTPS의 차이점을 설명해주세요.', 'EASY'),
('IT', '백엔드 개발자', '기술역량', 'GET과 POST 메서드의 차이는 무엇인가요?', 'EASY'),
('IT', '백엔드 개발자', '기술역량', '변수(variable)와 상수(constant)의 차이를 설명해주세요.', 'EASY'),
('IT', '백엔드 개발자', '기술역량', 'SQL과 NoSQL 데이터베이스의 차이점은 무엇인가요?', 'EASY'),
('IT', '백엔드 개발자', '기술역량', 'API란 무엇이고, 왜 필요한가요?', 'EASY');

-- 기술역량 - MEDIUM (5개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('IT', '백엔드 개발자', '기술역량', 'Spring Bean의 생명주기를 설명해주세요.', 'MEDIUM'),
('IT', '백엔드 개발자', '기술역량', 'RESTful API 설계 원칙에 대해 설명해주세요.', 'MEDIUM'),
('IT', '백엔드 개발자', '기술역량', 'JPA와 MyBatis의 차이점과 각각의 장단점을 설명해주세요.', 'MEDIUM'),
('IT', '백엔드 개발자', '기술역량', '트랜잭션(Transaction)이란 무엇이고, ACID 속성을 설명해주세요.', 'MEDIUM'),
('IT', '백엔드 개발자', '기술역량', 'MVC 패턴에 대해 설명하고, Spring MVC의 구조를 설명해주세요.', 'MEDIUM');

-- 기술역량 - HARD (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('IT', '백엔드 개발자', '기술역량', 'MSA(Microservices Architecture)와 Monolithic Architecture의 차이점과 각각의 장단점을 설명해주세요.', 'HARD'),
('IT', '백엔드 개발자', '기술역량', 'DB 인덱스(Index)의 동작 원리와 B-Tree, Hash 인덱스의 차이를 설명해주세요.', 'HARD'),
('IT', '백엔드 개발자', '기술역량', 'JVM의 메모리 구조와 Garbage Collection 동작 원리를 설명해주세요.', 'HARD');

-- 문제해결 - MEDIUM (3개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('IT', '백엔드 개발자', '문제해결', '서버 응답 시간이 느려졌을 때 어떻게 문제를 진단하고 해결하시겠습니까?', 'MEDIUM'),
('IT', '백엔드 개발자', '문제해결', '프로덕션 환경에서 갑자기 메모리 사용량이 급증했을 때 어떻게 대응하시겠습니까?', 'MEDIUM'),
('IT', '백엔드 개발자', '문제해결', 'API 응답 속도를 개선한 경험이 있다면 구체적으로 설명해주세요.', 'MEDIUM');

-- 문제해결 - HARD (2개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('IT', '백엔드 개발자', '문제해결', '대용량 트래픽을 처리한 경험이 있나요? 어떤 전략을 사용했는지 설명해주세요.', 'HARD'),
('IT', '백엔드 개발자', '문제해결', '동시성 이슈(Race Condition)를 경험한 적이 있나요? 어떻게 해결했는지 설명해주세요.', 'HARD');

-- 협업경험 - EASY (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('IT', '백엔드 개발자', '협업경험', '버전 관리 시스템(Git)을 사용한 경험이 있나요? 주로 어떤 워크플로우를 사용했나요?', 'EASY');

-- 협업경험 - MEDIUM (1개)
INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
('IT', '백엔드 개발자', '협업경험', '코드 리뷰 시 중요하게 생각하는 포인트는 무엇인가요?', 'MEDIUM');
