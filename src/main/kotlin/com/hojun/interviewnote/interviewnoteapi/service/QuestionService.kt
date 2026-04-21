package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.dto.QuestionDto
import com.hojun.interviewnote.interviewnoteapi.exception.QuestionNotFoundException
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QuestionService(
    private val questionRepository: QuestionRepository
) {
    fun findAll(jobField: String?, category: String?, difficulty: String?): List<QuestionDto> {
        // 빈 문자열을 null로 처리, jobField 기본값은 "IT"
        val effectiveJobField = jobField?.takeIf { it.isNotBlank() } ?: "IT"
        val validCategory = category?.takeIf { it.isNotBlank() }
        val validDifficulty = difficulty?.takeIf { it.isNotBlank() }

        val questions = when {
            validCategory != null && validDifficulty != null ->
                questionRepository.findByJobFieldAndCategoryAndDifficultyAndIsActiveTrue(
                    effectiveJobField, validCategory, validDifficulty
                )
            validCategory != null ->
                questionRepository.findByJobFieldAndCategoryAndIsActiveTrue(
                    effectiveJobField, validCategory
                )
            validDifficulty != null ->
                questionRepository.findByJobFieldAndDifficultyAndIsActiveTrue(
                    effectiveJobField, validDifficulty
                )
            else ->
                questionRepository.findByJobFieldAndIsActiveTrue(effectiveJobField)
        }

        return questions.map { QuestionDto.from(it) }
    }

    fun findById(id: Long): Question {
        return questionRepository.findById(id)
            .orElseThrow { QuestionNotFoundException(id) }
    }

    fun findDtoById(id: Long): QuestionDto {
        return QuestionDto.from(findById(id))
    }

    /**
     * 모든 직무의 카테고리 맵 반환
     *
     * Phase 5: 직무별 동적 카테고리 필터링
     * - Key: JobField.name() (예: "IT", "SALES")
     * - Value: List<String> (해당 직무의 카테고리 목록)
     *
     * 용도:
     * - 프론트엔드에서 직무 선택 시 해당 직무의 카테고리만 표시
     * - JavaScript로 클라이언트 사이드에서 동적 필터링
     *
     * @return Map<String, List<String>> 직무별 카테고리 맵
     */
    fun getCategoriesByAllJobFields(): Map<String, List<String>> {
        return JobField.values().associate { jobField ->
            jobField.name to questionRepository.findDistinctCategoriesByJobField(jobField.name)
        }
    }
}
