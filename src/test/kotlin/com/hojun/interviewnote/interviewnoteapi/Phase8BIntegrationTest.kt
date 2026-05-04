package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.MockInterviewStatus
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.repository.MockInterviewRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.MockInterviewService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * Phase 8B: 경력 수준 및 UI 개선 통합 테스트
 *
 * 요구사항:
 * - 경력 수준 선택 가능 (4단계)
 * - 경력 수준 저장 및 조회
 * - User 프로필에서 careerLevel 가져오기
 * - 기본값 ENTRY 설정
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 8B: 경력 수준 및 UI 개선 통합 테스트")
class Phase8BIntegrationTest {

    @Autowired
    private lateinit var mockInterviewService: MockInterviewService

    @Autowired
    private lateinit var mockInterviewRepository: MockInterviewRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = User(
            email = "phase8b@test.com",
            passwordHash = "hashed",
            name = "Phase8B Test User",
            role = UserRole.USER,
            jobField = JobField.IT,
            careerLevel = null  // 프로필 설정 없음
        )
        testUser = userRepository.save(testUser)
    }

    @Test
    @DisplayName("경력 수준 ENTRY로 면접 시작")
    fun `should start interview with ENTRY level`() {
        // Given: 경력 수준 ENTRY
        val careerLevel = CareerLevel.ENTRY

        // When: 면접 시작
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT,
            careerLevel = careerLevel
        )

        // Then: 경력 수준 저장됨
        assertThat(interview.careerLevel).isEqualTo(CareerLevel.ENTRY)
        assertThat(interview.selectedJobField).isEqualTo(JobField.IT)
        assertThat(interview.status).isEqualTo(MockInterviewStatus.IN_PROGRESS)
    }

    @Test
    @DisplayName("경력 수준 JUNIOR로 면접 시작")
    fun `should start interview with JUNIOR level`() {
        // Given: 경력 수준 JUNIOR
        val careerLevel = CareerLevel.JUNIOR

        // When: 면접 시작
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT,
            careerLevel = careerLevel
        )

        // Then: 경력 수준 저장됨
        assertThat(interview.careerLevel).isEqualTo(CareerLevel.JUNIOR)
    }

    @Test
    @DisplayName("경력 수준 SENIOR로 면접 시작")
    fun `should start interview with SENIOR level`() {
        // Given: 경력 수준 SENIOR
        val careerLevel = CareerLevel.SENIOR

        // When: 면접 시작
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT,
            careerLevel = careerLevel
        )

        // Then: 경력 수준 저장됨
        assertThat(interview.careerLevel).isEqualTo(CareerLevel.SENIOR)
    }

    @Test
    @DisplayName("경력 수준 SENIOR_PLUS로 면접 시작")
    fun `should start interview with SENIOR_PLUS level`() {
        // Given: 경력 수준 SENIOR_PLUS
        val careerLevel = CareerLevel.SENIOR_PLUS

        // When: 면접 시작
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT,
            careerLevel = careerLevel
        )

        // Then: 경력 수준 저장됨
        assertThat(interview.careerLevel).isEqualTo(CareerLevel.SENIOR_PLUS)
    }

    @Test
    @DisplayName("경력 수준 null로 면접 시작 (기본값 없음)")
    fun `should allow null career level`() {
        // When: 경력 수준 없이 면접 시작
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT,
            careerLevel = null
        )

        // Then: null 허용
        assertThat(interview.careerLevel).isNull()
        assertThat(interview.status).isEqualTo(MockInterviewStatus.IN_PROGRESS)
    }

    @Test
    @DisplayName("CareerLevel enum - displayName 확인")
    fun `should have correct display names`() {
        // Then: displayName이 올바른지 확인
        assertThat(CareerLevel.ENTRY.displayName).isEqualTo("신입")
        assertThat(CareerLevel.JUNIOR.displayName).isEqualTo("주니어(1-3년)")
        assertThat(CareerLevel.SENIOR.displayName).isEqualTo("시니어(3-7년)")
        assertThat(CareerLevel.SENIOR_PLUS.displayName).isEqualTo("시니어+(7년 이상)")
    }

    @Test
    @DisplayName("CareerLevel enum - code로 찾기")
    fun `should find career level by code`() {
        // When/Then: code로 CareerLevel 찾기
        assertThat(CareerLevel.fromCode("ENTRY")).isEqualTo(CareerLevel.ENTRY)
        assertThat(CareerLevel.fromCode("JUNIOR")).isEqualTo(CareerLevel.JUNIOR)
        assertThat(CareerLevel.fromCode("SENIOR")).isEqualTo(CareerLevel.SENIOR)
        assertThat(CareerLevel.fromCode("SENIOR_PLUS")).isEqualTo(CareerLevel.SENIOR_PLUS)
        assertThat(CareerLevel.fromCode("INVALID")).isNull()
    }

    @Test
    @DisplayName("User 프로필에 경력 수준 저장")
    fun `should save career level in user profile`() {
        // Given: User 프로필 업데이트
        testUser.updateJobPreferences(JobField.IT, CareerLevel.JUNIOR)
        val savedUser = userRepository.save(testUser)

        // When: 조회
        val foundUser = userRepository.findById(savedUser.id).orElseThrow()

        // Then: 경력 수준 저장됨
        assertThat(foundUser.careerLevel).isEqualTo(CareerLevel.JUNIOR)
        assertThat(foundUser.jobField).isEqualTo(JobField.IT)
    }

    @Test
    @DisplayName("면접 조회 시 경력 수준 포함")
    fun `should include career level when retrieving interview`() {
        // Given: 경력 수준 SENIOR로 면접 생성
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT,
            careerLevel = CareerLevel.SENIOR
        )

        // When: 면접 조회
        val foundInterview = mockInterviewService.getInterview(interview.id, testUser.id)

        // Then: 경력 수준 포함
        assertThat(foundInterview.careerLevel).isEqualTo(CareerLevel.SENIOR)
        assertThat(foundInterview.selectedJobField).isEqualTo(JobField.IT)
    }

    @Test
    @DisplayName("사용자의 모든 면접 조회 - 경력 수준 포함")
    fun `should retrieve all user interviews with career levels`() {
        // Given: 2개 면접 생성 (다른 경력 수준)
        val interview1 = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT,
            careerLevel = CareerLevel.ENTRY
        )

        val interview2 = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT,
            careerLevel = CareerLevel.SENIOR
        )

        // When: 모든 면접 조회
        val allInterviews = mockInterviewService.getUserInterviews(testUser.id)

        // Then: 경력 수준 포함
        assertThat(allInterviews).hasSize(2)
        assertThat(allInterviews.map { it.careerLevel })
            .containsExactlyInAnyOrder(CareerLevel.ENTRY, CareerLevel.SENIOR)
    }
}
