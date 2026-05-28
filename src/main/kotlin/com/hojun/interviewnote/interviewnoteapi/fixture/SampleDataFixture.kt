package com.hojun.interviewnote.interviewnoteapi.fixture

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerWithFeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.FeedbackDto
import java.time.LocalDateTime

/**
 * 샘플 데이터 생성을 위한 중앙화된 Fixture 객체
 *
 * 이 객체는 다음 용도로 사용됩니다:
 * 1. 프로덕션: 홈 페이지 등에서 서비스 데모용 샘플 데이터 제공
 * 2. 테스트: 테스트 픽스처로 재사용 가능
 *
 * 모든 샘플 데이터는 이 객체를 통해 생성하여 일관성과 재사용성을 확보합니다.
 */
object SampleDataFixture {

    /**
     * 홈 페이지 우측 컬럼에 표시할 샘플 피드백 데이터
     * 서비스의 실제 결과물을 미리 보여주는 "Social Proof" 역할
     *
     * @return 백엔드 개발자 기술 면접 샘플 답변 및 AI 피드백
     */
    fun createSampleFeedback(): AnswerWithFeedbackDto {
        return AnswerWithFeedbackDto(
            answerId = 0,
            questionId = 0,
            questionContent = "프로젝트에서 발생한 기술적 문제를 어떻게 해결했나요?",
            answerText = "프로젝트에서 대용량 데이터 처리 시 성능 저하 문제가 발생했습니다. " +
                    "기존 동기 처리 방식에서 비동기 처리로 전환하고, Redis 캐싱을 도입하여 " +
                    "응답 속도를 5초에서 0.5초로 개선했습니다. 또한 DB 인덱스 최적화를 통해 " +
                    "쿼리 성능을 30% 향상시켰습니다.",
            answeredAt = LocalDateTime.now(),
            feedback = FeedbackDto(
                logicScore = 4,
                specificityScore = 3,
                jobFitScore = 4,
                deliveryScore = 4,
                strengths = listOf(
                    "문제 상황과 해결 과정이 명확하게 제시됨",
                    "구체적인 성과 지표 제시 (5초 → 0.5초)",
                    "다층적 해결 방안 제시 (비동기, 캐싱, 인덱싱)"
                ),
                improvements = listOf(
                    "팀 협업 과정을 추가하면 더 좋음",
                    "기술 선택 이유를 설명하면 더 설득력 있음"
                ),
                modelAnswer = "프로젝트 중 대용량 데이터 처리 시 응답 속도가 5초 이상 걸리는 병목 현상이 발생했습니다. " +
                        "먼저 프로파일링을 통해 동기 API 호출이 주 원인임을 파악했습니다. " +
                        "해결을 위해 CompletableFuture를 활용한 비동기 처리로 전환했고, " +
                        "자주 조회되는 데이터는 Redis 캐싱(TTL 10분)을 적용했습니다. " +
                        "또한 슬로우 쿼리 분석 후 인덱스를 추가해 쿼리 성능을 30% 개선했습니다. " +
                        "결과적으로 평균 응답 시간을 0.5초로 단축하여 사용자 만족도를 크게 향상시켰습니다.",
                overallComment = "기술적 문제 해결 능력이 우수합니다. 문제 분석, 해결 전략, 성과 측정이 체계적으로 제시되었습니다. " +
                        "팀 협업 경험을 추가하면 더욱 완성도 높은 답변이 될 것입니다."
            ),
            generatedQuestionId = null,
            isGenerated = false
        )
    }

    /**
     * 샘플 질문 생성
     *
     * @param jobField 직무 분야 (기본값: "IT")
     * @param targetJob 세부 직무 (기본값: "백엔드 개발자")
     * @param category 질문 카테고리 (기본값: "기술역량")
     * @param content 질문 내용 (기본값: "샘플 질문입니다")
     * @param difficulty 난이도 (기본값: "MEDIUM")
     * @param isActive 활성화 여부 (기본값: true)
     * @return 생성된 Question 엔티티 (ID는 0으로 설정됨)
     */
    fun createSampleQuestion(
        jobField: String = "IT",
        targetJob: String = "백엔드 개발자",
        category: String = "기술역량",
        content: String = "샘플 질문입니다",
        difficulty: String = "MEDIUM",
        isActive: Boolean = true
    ): Question {
        return Question(
            jobField = jobField,
            targetJob = targetJob,
            category = category,
            content = content,
            difficulty = difficulty,
            isActive = isActive,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 샘플 답변 생성
     *
     * @param questionId 질문 ID (기본값: 0)
     * @param userId 사용자 ID (기본값: 0)
     * @param answerText 답변 내용 (기본값: "샘플 답변입니다")
     * @return 생성된 InterviewAnswer 엔티티 (ID는 0으로 설정됨)
     */
    fun createSampleAnswer(
        questionId: Long = 0,
        userId: Long = 0,
        answerText: String = "샘플 답변입니다"
    ): InterviewAnswer {
        return InterviewAnswer(
            questionId = questionId,
            userId = userId,
            answerText = answerText,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
