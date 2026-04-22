package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 전용 API 컨트롤러
 *
 * Phase 5 Step 16에서 추가됨
 * - 통계 조회 (직무별 사용자 분포 등)
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 직무별 사용자 통계
     *
     * Phase 5 Step 16: Admin 통계 엔드포인트
     *
     * @return 직무별 사용자 수 (Key: 직무명 또는 "NONE", Value: 사용자 수)
     *
     * 예시 응답:
     * ```json
     * {
     *   "IT": 15,
     *   "SALES": 8,
     *   "MARKETING": 5,
     *   "NONE": 3
     * }
     * ```
     */
    @GetMapping("/stats/job-fields")
    fun getJobFieldStats(): Map<String, Long> {
        logger.info("관리자 통계 조회: 직무별 사용자 분포")

        val stats = userRepository.findAll()
            .groupingBy { it.jobField?.name ?: "NONE" }
            .eachCount()
            .mapValues { it.value.toLong() }

        logger.debug("직무별 사용자 통계: {}", stats)

        return stats
    }

    /**
     * 경력별 사용자 통계
     *
     * Phase 5 Step 16: 추가 통계 엔드포인트
     *
     * @return 경력별 사용자 수 (Key: 경력 수준 또는 "NONE", Value: 사용자 수)
     */
    @GetMapping("/stats/career-levels")
    fun getCareerLevelStats(): Map<String, Long> {
        logger.info("관리자 통계 조회: 경력별 사용자 분포")

        val stats = userRepository.findAll()
            .groupingBy { it.careerLevel?.name ?: "NONE" }
            .eachCount()
            .mapValues { it.value.toLong() }

        logger.debug("경력별 사용자 통계: {}", stats)

        return stats
    }

    /**
     * 전체 사용자 통계 요약
     *
     * Phase 5 Step 16: 통합 통계 엔드포인트
     *
     * @return 전체 통계 정보
     */
    @GetMapping("/stats/summary")
    fun getStatsSummary(): Map<String, Any> {
        logger.info("관리자 통계 조회: 전체 요약")

        val allUsers = userRepository.findAll()

        val summary = mapOf(
            "totalUsers" to allUsers.size,
            "activeUsers" to allUsers.count { it.isActive },
            "usersWithJobField" to allUsers.count { it.jobField != null },
            "usersWithCareerLevel" to allUsers.count { it.careerLevel != null },
            "jobFieldDistribution" to allUsers
                .groupingBy { it.jobField?.displayName ?: "미설정" }
                .eachCount(),
            "careerLevelDistribution" to allUsers
                .groupingBy { it.careerLevel?.displayName ?: "미설정" }
                .eachCount()
        )

        logger.debug("전체 통계 요약: {}", summary)

        return summary
    }
}
