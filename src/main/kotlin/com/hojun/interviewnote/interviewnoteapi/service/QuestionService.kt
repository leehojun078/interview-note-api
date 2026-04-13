package com.hojun.interviewnote.interviewnoteapi.service

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
    fun findAll(category: String?, difficulty: String?): List<QuestionDto> {
        // 빈 문자열을 null로 처리
        val validCategory = category?.takeIf { it.isNotBlank() }
        val validDifficulty = difficulty?.takeIf { it.isNotBlank() }

        val questions = when {
            validCategory != null && validDifficulty != null ->
                questionRepository.findByCategoryAndDifficultyAndIsActiveTrue(validCategory, validDifficulty)
            validCategory != null ->
                questionRepository.findByCategoryAndIsActiveTrue(validCategory)
            validDifficulty != null ->
                questionRepository.findByDifficultyAndIsActiveTrue(validDifficulty)
            else ->
                questionRepository.findByIsActiveTrue()
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
}
