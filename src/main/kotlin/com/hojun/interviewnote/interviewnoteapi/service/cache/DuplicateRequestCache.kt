package com.hojun.interviewnote.interviewnoteapi.service.cache

import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * 중복 요청 방지 캐시
 *
 * 동일한 질문에 동일한 답변을 24시간 이내에 재평가하지 않도록 방지합니다.
 * (questionId, answerText) 조합의 SHA-256 해시를 사용하여 중복을 감지합니다.
 */
@Service
class DuplicateRequestCache(
    private val aiFeedbackRepository: AiFeedbackRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_DURATION_HOURS = 24L
    }

    /**
     * 캐시된 피드백 조회 (24시간 이내)
     *
     * @param questionId 질문 ID
     * @param answerText 답변 텍스트
     * @return 캐시된 피드백 (없으면 null)
     */
    fun findCached(questionId: Long, answerText: String): AiFeedback? {
        val hash = generateHash(questionId, answerText)
        val cutoffTime = LocalDateTime.now().minusHours(CACHE_DURATION_HOURS)

        val cached = aiFeedbackRepository.findByAnswerTextHashAndCreatedAtAfter(hash, cutoffTime)

        if (cached != null) {
            logger.info("캐시된 피드백 사용 - 해시: $hash, 피드백 ID: ${cached.id}")
        }

        return cached
    }

    /**
     * 질문 ID와 답변 텍스트로부터 SHA-256 해시 생성
     *
     * @param questionId 질문 ID
     * @param answerText 답변 텍스트
     * @return SHA-256 해시 (64자 16진수 문자열)
     */
    fun generateHash(questionId: Long, answerText: String): String {
        val input = "$questionId:$answerText"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
