package com.hojun.interviewnote.interviewnoteapi.exception

abstract class NotFoundException(message: String) : RuntimeException(message)

class QuestionNotFoundException(id: Long) :
    NotFoundException("질문을 찾을 수 없습니다: $id")

class AnswerNotFoundException(id: Long) :
    NotFoundException("답변을 찾을 수 없습니다: $id")

class FeedbackNotFoundException(answerId: Long) :
    NotFoundException("평가 결과를 찾을 수 없습니다 (답변 ID: $answerId)")

class UserNotFoundException(message: String) :
    NotFoundException(message)
