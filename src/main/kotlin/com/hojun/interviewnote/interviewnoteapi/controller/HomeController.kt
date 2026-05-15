package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.dto.AnswerWithFeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.FeedbackDto
import com.hojun.interviewnote.interviewnoteapi.exception.UserNotFoundException
import com.hojun.interviewnote.interviewnoteapi.service.QuestionService
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import com.hojun.interviewnote.interviewnoteapi.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.time.LocalDateTime

@Controller
class HomeController(
    private val reviewService: ReviewService,
    private val userService: UserService,
    private val questionService: QuestionService
) {
    /**
     * 홈 페이지
     *
     * Phase 4A-2에서 수정: 로그인한 사용자의 최근 리뷰 표시
     * Phase 5에서 수정: 직무 기반 개인화 (추천 질문, 직무 미설정 배너)
     */
    @GetMapping("/", "/home")
    fun home(
        @AuthenticationPrincipal userDetails: UserDetails?,
        model: Model
    ): String {
        val isLoggedIn = userDetails != null
        model.addAttribute("isLoggedIn", isLoggedIn)

        // Add sample feedback for hero preview (always shown)
        val sampleFeedback = createSampleFeedback()
        model.addAttribute("sampleFeedback", sampleFeedback)

        if (userDetails != null) {
            val user = userService.findByEmail(userDetails.username)
                ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

            // 최근 리뷰 3개
            val recentReviews = reviewService.getUserReviews(user.id).take(3)
            model.addAttribute("recentReviews", recentReviews)

            // 직무 설정 여부
            val hasJobFieldSet = user.jobField != null
            model.addAttribute("hasJobFieldSet", hasJobFieldSet)

            // 추천 질문 (사용자 직무 기반, 없으면 IT 기본)
            val jobField = user.jobField?.name ?: "IT"
            val recommendedQuestions = questionService.findAll(jobField, null, null)
                .shuffled()
                .take(5)
            model.addAttribute("recommendedQuestions", recommendedQuestions)
            model.addAttribute("userJobField", user.jobField?.displayName ?: "IT개발")
        } else {
            model.addAttribute("recentReviews", emptyList<Any>())
        }

        return "home"
    }

    companion object {
        /**
         * 홈 페이지 우측 컬럼에 표시할 샘플 피드백 데이터
         * 서비스의 실제 결과물을 미리 보여주는 "Social Proof" 역할
         */
        private fun createSampleFeedback(): AnswerWithFeedbackDto {
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
                    logicScore = 5,
                    specificityScore = 4,
                    jobFitScore = 5,
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
    }
}
