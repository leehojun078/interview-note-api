package com.hojun.interviewnote.interviewnoteapi.exception

/**
 * 모의 면접 관련 예외 (Phase 7)
 *
 * sealed class로 구현하여 타입 안전성 확보
 */
sealed class MockInterviewException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 면접 세션을 찾을 수 없음
 */
class MockInterviewNotFoundException(
    val interviewId: Long
) : MockInterviewException("면접 세션을 찾을 수 없습니다: $interviewId")

/**
 * 면접 세션 접근 권한 없음
 * 타 사용자의 면접 세션 접근 시도
 */
class MockInterviewAccessDeniedException(
    val interviewId: Long,
    val userId: Long
) : MockInterviewException(
    "면접 세션에 접근 권한이 없습니다: interviewId=$interviewId, userId=$userId"
)

/**
 * 최대 대화 턴 수 초과
 * 30턴 제한
 */
class MaxTurnExceededException(
    val interviewId: Long,
    val maxTurns: Int = 30
) : MockInterviewException(
    "최대 대화 횟수를 초과했습니다: ${maxTurns}턴"
)

/**
 * 이미 종료된 면접 세션
 * COMPLETED 또는 ABORTED 상태에서 메시지 전송 시도
 */
class InterviewAlreadyEndedException(
    val interviewId: Long,
    val status: String
) : MockInterviewException(
    "이미 종료된 면접 세션입니다: interviewId=$interviewId, status=$status"
)

/**
 * 면접 세션 생성 실패
 * 일반적인 생성 오류
 */
class MockInterviewCreationException(
    message: String,
    cause: Throwable? = null
) : MockInterviewException(message, cause)
