# 📚 Interview Note API - 문서 디렉토리

이 디렉토리는 프로젝트의 모든 문서를 체계적으로 관리합니다.

---

## 📁 디렉토리 구조

```
docs/
├── guides/            # 개발 가이드 및 설정 문서
├── archive/           # 과거 Phase 개발 문서 (아카이브)
├── current/           # 현재 활성화된 개선 계획 및 최신 보고서
└── README.md          # 이 파일
```

---

## 🔧 guides/ - 개발 가이드

프로젝트 설정, 개발 방법, 코드 품질 기준 등 **개발자가 참고할 가이드**

| 파일 | 설명 |
|------|------|
| `SETUP_GUIDE.md` | 프로젝트 초기 설정 가이드 |
| `IMPLEMENTATION_GUIDE.md` | 기능 구현 가이드 |
| `CODE_QUALITY_GUIDE.md` | 코드 품질 기준 및 Best Practices |
| `REFACTORING_GUIDE.md` | 리팩토링 가이드 (Week 1-3) |
| `NGINX_SSE_SETUP.md` | Nginx SSE 설정 가이드 |
| `PHASE5_MIGRATION_GUIDE.md` | Phase 5 다중 직무 마이그레이션 가이드 |

**용도**: 신규 개발자 온보딩, 기능 추가 시 참고

---

## 📦 archive/ - Phase 개발 아카이브

Phase 1부터 Phase 8까지의 **모든 개발 문서 보관소**

### Phase별 문서 종류
- **COMPLETION_REPORT.md**: 완료 보고서
- **implementation_plan.md**: 구현 계획서
- **BUG_FIX_REPORT.md**: 버그 수정 보고서
- **기능 명세서**: 특정 기능에 대한 상세 명세

### 주요 Phase
| Phase | 주요 내용 |
|-------|----------|
| Phase 1-3 | MVP 기반 구축, AI 연동, 프로덕션 준비 |
| Phase 4 | 사용자 관리 (인증/권한) |
| Phase 5 | 다중 직무 지원 (17개 직무) |
| Phase 6 | 채용 공고 기반 질문 생성 |
| Phase 7 | AI 채팅 면접 |
| Phase 8 | AI 면접 개선 |

**용도**: 개발 히스토리 추적, 의사결정 배경 확인

---

## 💡 current/ - 현재 활성 문서

**현재 진행 중이거나 다음 구현 대상인 개선 계획 및 최신 보고서**

| 파일 | 상태 | 설명 |
|------|------|------|
| `REFACTORING_COMPLETION_REPORT.md` | ✅ 완료 | Week 2-3 리팩토링 완료 보고서 |
| `ANSWER_VALIDATION_IMPROVEMENTS.md` | 📋 계획 | 답변 검증 개선 계획 |
| `UI_UX_COMPARISON_ANALYSIS.md` | 📋 계획 | UI/UX 비교 분석 |
| `UI_UX_MODERNIZATION_PLAN.md` | 📋 계획 | UI/UX 현대화 계획 |
| `UNIT_TEST_PLAN.md` | 📋 계획 | 단위 테스트 확대 계획 |

**용도**: 다음 작업 계획, 현재 진행 상황 확인

---

## 🔍 문서 찾기

### 설정이 필요할 때
→ `docs/guides/SETUP_GUIDE.md`

### 코드 품질 기준을 확인하고 싶을 때
→ `docs/guides/CODE_QUALITY_GUIDE.md`

### 과거 Phase에서 어떻게 구현했는지 확인하고 싶을 때
→ `docs/archive/PHASE{N}_*.md`

### 다음에 무엇을 할지 확인하고 싶을 때
→ `docs/current/` 디렉토리 확인

### 최신 리팩토링 내용을 확인하고 싶을 때
→ `docs/current/REFACTORING_COMPLETION_REPORT.md`

---

## 📝 문서 작성 규칙

1. **새로운 개발 가이드**: `docs/guides/`에 추가
2. **Phase 완료 보고서**: `docs/archive/`로 이동
3. **진행 중인 계획**: `docs/current/`에 유지
4. **구현 완료된 계획**: `docs/archive/`로 이동

---

**문서 관리 담당**: Claude Sonnet 4.5
**최종 업데이트**: 2026-06-13
