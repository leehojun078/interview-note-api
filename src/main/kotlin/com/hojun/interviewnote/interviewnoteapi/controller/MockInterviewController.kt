package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.exception.MockInterviewNotFoundException
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.MockInterviewService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * 모의 면접 컨트롤러
 *
 * Phase 7C: SSE 실시간 통신
 */
@Controller
@RequestMapping("/mock-interviews")
class MockInterviewController(
    private val mockInterviewService: MockInterviewService,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * POST /mock-interviews/start
     *
     * 면접 시작
     *
     * 요청:
     * - jobPostingId: Long? (선택 사항, 공고 기반 면접 시)
     * - selectedJobField: String (필수)
     *
     * 응답: redirect to /mock-interviews/{id}/chat
     */
    @PostMapping("/start")
    fun startInterview(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(required = false) jobPostingId: Long?,
        @RequestParam selectedJobField: String,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        return try {
            val jobField = JobField.valueOf(selectedJobField)

            val interview = mockInterviewService.startInterview(
                userId = user.id,
                jobPostingId = jobPostingId,
                selectedJobField = jobField
            )

            logger.info("면접 시작 - interviewId: ${interview.id}, userId: ${user.id}, jobField: $jobField")

            "redirect:/mock-interviews/${interview.id}/chat"
        } catch (e: IllegalArgumentException) {
            logger.error("잘못된 직무 필드: $selectedJobField", e)
            redirectAttributes.addFlashAttribute("error", "잘못된 직무가 선택되었습니다")
            "redirect:/"
        } catch (e: Exception) {
            logger.error("면접 시작 실패", e)
            redirectAttributes.addFlashAttribute("error", "면접 시작 중 오류가 발생했습니다: ${e.message}")
            "redirect:/"
        }
    }

    /**
     * GET /mock-interviews/{id}/chat
     *
     * 채팅 UI 페이지
     */
    @GetMapping("/{id}/chat")
    fun chatPage(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        model: Model
    ): String {
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        val interview = mockInterviewService.getInterview(id, user.id)
        val messages = mockInterviewService.getMessages(id)

        model.addAttribute("interview", interview)
        model.addAttribute("messages", messages)
        model.addAttribute("currentUser", user)

        return "mock-interviews/chat"
    }

    /**
     * POST /mock-interviews/{id}/messages
     *
     * 사용자 메시지 전송
     *
     * 요청:
     * - content: String (사용자 답변, 200자 이내)
     *
     * 응답: JSON {"success": true}
     *
     * SSE를 통해 메시지 브로드캐스트 수행 (비동기)
     */
    @PostMapping("/{id}/messages")
    @ResponseBody
    fun sendMessage(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam content: String
    ): Map<String, Any> {
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        return try {
            // 200자 제한 검증
            if (content.length > 200) {
                return mapOf(
                    "success" to false,
                    "error" to "답변은 200자 이내로 작성해주세요"
                )
            }

            mockInterviewService.sendUserMessage(id, user.id, content)

            mapOf("success" to true)
        } catch (e: Exception) {
            logger.error("메시지 전송 실패 - interviewId: $id", e)
            mapOf(
                "success" to false,
                "error" to (e.message ?: "메시지 전송 중 오류가 발생했습니다")
            )
        }
    }

    /**
     * GET /mock-interviews/{id}/stream (SSE)
     *
     * 실시간 메시지 수신 (Server-Sent Events)
     *
     * 응답: text/event-stream
     * - event: message
     * - data: JSON (id, sender, content, timestamp, scores...)
     *
     * 타임아웃: 30분
     */
    @GetMapping("/{id}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamMessages(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SseEmitter {
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        // 소유권 검증 (면접이 존재하고 사용자의 것인지 확인)
        mockInterviewService.getInterview(id, user.id)

        // SseEmitter 생성 (30분 타임아웃)
        val emitter = SseEmitter(30 * 60 * 1000L)

        // Emitter 등록
        mockInterviewService.registerEmitter(id, emitter)

        // 이벤트 핸들러 등록
        emitter.onCompletion {
            logger.info("SSE 연결 완료 - interviewId: $id")
            mockInterviewService.removeEmitter(id)
        }

        emitter.onTimeout {
            logger.warn("SSE 연결 타임아웃 - interviewId: $id")
            mockInterviewService.removeEmitter(id)
        }

        emitter.onError { error ->
            logger.error("SSE 연결 에러 - interviewId: $id", error)
            mockInterviewService.removeEmitter(id)
        }

        logger.info("SSE 스트림 연결 - interviewId: $id, userId: ${user.id}")

        return emitter
    }

    /**
     * POST /mock-interviews/{id}/end
     *
     * 면접 종료 및 종합 평가 생성
     *
     * 응답: redirect to /mock-interviews/{id}/result
     */
    @PostMapping("/{id}/end")
    fun endInterview(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        return try {
            mockInterviewService.endInterview(id, user.id)

            logger.info("면접 종료 - interviewId: $id, userId: ${user.id}")

            "redirect:/mock-interviews/$id/result"
        } catch (e: Exception) {
            logger.error("면접 종료 실패 - interviewId: $id", e)
            redirectAttributes.addFlashAttribute("error", "면접 종료 중 오류가 발생했습니다: ${e.message}")
            "redirect:/mock-interviews/$id/chat"
        }
    }

    /**
     * GET /mock-interviews/{id}/result
     *
     * 종합 평가 결과 페이지
     *
     * UI 요구사항:
     * - 점수 요약 카드 (평균 점수, 프로그레스 바)
     * - 종합 피드백 카드
     * - 강점 카드 (초록색)
     * - 개선점 카드 (노란색)
     * - 대화 내역 (접기 가능)
     * - 액션 버튼 (다시 연습하기, 리뷰 목록)
     */
    @GetMapping("/{id}/result")
    fun resultPage(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        model: Model
    ): String {
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        val interview = mockInterviewService.getInterview(id, user.id)

        // 면접이 종료되지 않았으면 에러
        if (interview.status.name != "COMPLETED") {
            throw IllegalStateException("면접이 아직 종료되지 않았습니다: interviewId=$id, status=${interview.status.name}")
        }

        val messages = mockInterviewService.getMessages(id)

        model.addAttribute("interview", interview)
        model.addAttribute("messages", messages)
        model.addAttribute("currentUser", user)

        return "mock-interviews/result"
    }
}
