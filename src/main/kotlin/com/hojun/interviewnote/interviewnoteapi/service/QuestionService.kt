package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.dto.QuestionDto
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QuestionService(
    private val questionRepository: QuestionRepository
) {
    fun findAll(category: String?, difficulty: String?): List<QuestionDto> {
        val questions = when {
            category != null && difficulty != null ->
                questionRepository.findByCategoryAndDifficultyAndIsActiveTrue(category, difficulty)
            category != null ->
                questionRepository.findByCategoryAndIsActiveTrue(category)
            difficulty != null ->
                questionRepository.findByDifficultyAndIsActiveTrue(difficulty)
            else ->
                questionRepository.findByIsActiveTrue()
        }

        return questions.map { QuestionDto.from(it) }
    }

    fun findById(id: Long): Question {
        return questionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("질문을 찾을 수 없습니다: $id") }
    }

    fun findDtoById(id: Long): QuestionDto {
        return QuestionDto.from(findById(id))
    }
}
