package com.hojun.interviewnote.interviewnoteapi.controller

/**
 * Controller에서 사용하는 상수 정의
 *
 * Magic String을 제거하고 타입 안전성을 보장하기 위한 상수 모음
 * - Query Parameter 이름
 * - Error/Warning 타입
 * - Redirect URL 패턴
 */
object ControllerConstants {

    // ===== Query Parameters =====
    const val PARAM_ERROR = "error"
    const val PARAM_WARNING = "warning"
    const val PARAM_DUPLICATE = "duplicate"

    // ===== Error Types =====
    const val ERROR_RATELIMIT = "ratelimit"
    const val ERROR_VALIDATION = "validation"
    const val ERROR_INVALID_ANSWER = "invalid_answer"
    const val ERROR_SUBMIT_FAILED = "submit_failed"

    // ===== Warning Types =====
    const val WARNING_LOW_QUALITY = "low_quality"

    // ===== Boolean Values =====
    const val VALUE_TRUE = "true"
    const val VALUE_FALSE = "false"

    // ===== Redirect URL Builder Helpers =====
    /**
     * 에러 파라미터를 포함한 redirect URL 생성
     * @param baseUrl 기본 URL (예: "/questions/123/answer")
     * @param errorType 에러 타입 (예: ERROR_VALIDATION)
     * @return redirect URL (예: "redirect:/questions/123/answer?error=validation")
     */
    fun buildRedirectWithError(baseUrl: String, errorType: String): String {
        return "redirect:$baseUrl?$PARAM_ERROR=$errorType"
    }

    /**
     * 경고 파라미터를 포함한 redirect URL 생성
     * @param baseUrl 기본 URL (예: "/answers/456/feedback")
     * @param warningType 경고 타입 (예: WARNING_LOW_QUALITY)
     * @return redirect URL (예: "redirect:/answers/456/feedback?warning=low_quality")
     */
    fun buildRedirectWithWarning(baseUrl: String, warningType: String): String {
        return "redirect:$baseUrl?$PARAM_WARNING=$warningType"
    }

    /**
     * 중복 파라미터를 포함한 redirect URL 생성
     * @param baseUrl 기본 URL (예: "/answers/456/feedback")
     * @return redirect URL (예: "redirect:/answers/456/feedback?duplicate=true")
     */
    fun buildRedirectWithDuplicate(baseUrl: String): String {
        return "redirect:$baseUrl?$PARAM_DUPLICATE=$VALUE_TRUE"
    }
}
