package com.hojun.interviewnote.interviewnoteapi.service.ai

/**
 * AI 클라이언트 인터페이스
 *
 * OpenAI API 호출을 추상화하여 Mock 테스트와 향후 다른 AI 모델로의 교체를 용이하게 합니다.
 * (예: Claude API, Gemini API 등으로 교체 가능)
 */
interface AiClient {
    /**
     * AI 피드백 요청
     *
     * @param systemPrompt 시스템 프롬프트 (역할 및 평가 기준)
     * @param userPrompt 사용자 프롬프트 (질문 및 답변)
     * @return AI 응답 (JSON 문자열)
     */
    fun requestFeedback(systemPrompt: String, userPrompt: String): String
}
