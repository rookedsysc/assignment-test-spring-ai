package com.rokyai.springaipoc.feedback.service

import com.rokyai.springaipoc.feedback.dto.FeedbackCreateRequest
import com.rokyai.springaipoc.feedback.entity.Feedback
import com.rokyai.springaipoc.feedback.entity.FeedbackStatus
import com.rokyai.springaipoc.feedback.repository.FeedbackRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * FeedbackService 단위 테스트
 */
@DisplayName("FeedbackService 테스트")
class FeedbackServiceTest {

    private val feedbackRepository: FeedbackRepository = mockk()
    private lateinit var feedbackService: FeedbackService

    @BeforeEach
    fun setup() {
        feedbackService = FeedbackService(feedbackRepository)
    }

    @Test
    @DisplayName("피드백 생성 성공 테스트")
    fun createFeedbackSuccess() = runTest {
        // Given - 피드백 생성 요청 준비
        val request = FeedbackCreateRequest(
            userId = "user123",
            chatId = "chat456",
            isPositive = true
        )

        val savedFeedback = Feedback(
            id = UUID.randomUUID(),
            userId = request.userId,
            chatId = request.chatId,
            isPositive = request.isPositive,
            status = FeedbackStatus.PENDING,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )

        every { feedbackRepository.existsByUserIdAndChatId(request.userId, request.chatId) } returns Mono.just(false)
        every { feedbackRepository.save(any()) } returns Mono.just(savedFeedback)

        // When - 피드백 생성
        val response = feedbackService.createFeedback(request)

        // Then - 응답 검증
        assertNotNull(response)
        assertEquals(request.userId, response.userId)
        assertEquals(request.chatId, response.chatId)
        assertEquals(request.isPositive, response.isPositive)
        assertEquals(FeedbackStatus.PENDING, response.status)

        verify(exactly = 1) { feedbackRepository.existsByUserIdAndChatId(request.userId, request.chatId) }
        verify(exactly = 1) { feedbackRepository.save(any()) }
    }

    @Test
    @DisplayName("중복 피드백 생성 시도 시 예외 발생 테스트")
    fun createFeedbackDuplicateFailure() {
        // Given - 이미 피드백이 존재하는 상황
        val request = FeedbackCreateRequest(
            userId = "user123",
            chatId = "chat456",
            isPositive = true
        )

        every { feedbackRepository.existsByUserIdAndChatId(request.userId, request.chatId) } returns Mono.just(true)

        // When & Then - 중복 피드백 생성 시도 시 예외 발생
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runTest {
                feedbackService.createFeedback(request)
            }
        }

        assertTrue(exception.message!!.contains("이미 해당 대화에 대한 피드백이 존재합니다"))
        verify(exactly = 1) { feedbackRepository.existsByUserIdAndChatId(request.userId, request.chatId) }
        verify(exactly = 0) { feedbackRepository.save(any()) }
    }

    @Test
    @DisplayName("사용자 자신의 피드백 목록 조회 성공 테스트")
    fun getUserFeedbacksSuccess() = runTest {
        // Given - 사용자의 피드백 데이터 준비
        val userId = "user123"
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))

        val feedbacks = listOf(
            Feedback(
                id = UUID.randomUUID(),
                userId = userId,
                chatId = "chat1",
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            ),
            Feedback(
                id = UUID.randomUUID(),
                userId = userId,
                chatId = "chat2",
                isPositive = false,
                status = FeedbackStatus.RESOLVED,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        every { feedbackRepository.findByUserId(userId, pageable) } returns Flux.fromIterable(feedbacks)

        // When - 피드백 목록 조회
        val responses = feedbackService.getUserFeedbacks(userId, null, pageable)

        // Then - 응답 검증
        assertEquals(2, responses.size)
        assertTrue(responses.all { it.userId == userId })

        verify(exactly = 1) { feedbackRepository.findByUserId(userId, pageable) }
    }

    @Test
    @DisplayName("긍정 피드백만 필터링하여 조회 성공 테스트")
    fun getUserFeedbacksWithPositiveFilterSuccess() = runTest {
        // Given - 긍정 피드백만 필터링
        val userId = "user123"
        val isPositive = true
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))

        val feedbacks = listOf(
            Feedback(
                id = UUID.randomUUID(),
                userId = userId,
                chatId = "chat1",
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        every { feedbackRepository.findByUserIdAndIsPositive(userId, isPositive, pageable) } returns Flux.fromIterable(feedbacks)

        // When - 긍정 피드백만 조회
        val responses = feedbackService.getUserFeedbacks(userId, isPositive, pageable)

        // Then - 응답 검증
        assertEquals(1, responses.size)
        assertTrue(responses.all { it.isPositive })

        verify(exactly = 1) { feedbackRepository.findByUserIdAndIsPositive(userId, isPositive, pageable) }
    }

    @Test
    @DisplayName("관리자가 모든 피드백 조회 성공 테스트")
    fun getAllFeedbacksForAdminSuccess() = runTest {
        // Given - 모든 사용자의 피드백 데이터 준비
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))

        val feedbacks = listOf(
            Feedback(
                id = UUID.randomUUID(),
                userId = "user1",
                chatId = "chat1",
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            ),
            Feedback(
                id = UUID.randomUUID(),
                userId = "user2",
                chatId = "chat2",
                isPositive = false,
                status = FeedbackStatus.RESOLVED,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        every { feedbackRepository.findAllBy(pageable) } returns Flux.fromIterable(feedbacks)

        // When - 모든 피드백 조회
        val responses = feedbackService.getAllFeedbacks(null, pageable)

        // Then - 응답 검증
        assertEquals(2, responses.size)

        verify(exactly = 1) { feedbackRepository.findAllBy(pageable) }
    }

    @Test
    @DisplayName("관리자가 긍정 피드백만 필터링하여 조회 성공 테스트")
    fun getAllFeedbacksWithFilterForAdminSuccess() = runTest {
        // Given - 긍정 피드백만 필터링
        val isPositive = true
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))

        val feedbacks = listOf(
            Feedback(
                id = UUID.randomUUID(),
                userId = "user1",
                chatId = "chat1",
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        every { feedbackRepository.findByIsPositive(isPositive, pageable) } returns Flux.fromIterable(feedbacks)

        // When - 긍정 피드백만 조회
        val responses = feedbackService.getAllFeedbacks(isPositive, pageable)

        // Then - 응답 검증
        assertEquals(1, responses.size)
        assertTrue(responses.all { it.isPositive })

        verify(exactly = 1) { feedbackRepository.findByIsPositive(isPositive, pageable) }
    }

    @Test
    @DisplayName("피드백 상태 변경 성공 테스트")
    fun updateFeedbackStatusSuccess() = runTest {
        // Given - 피드백 상태 변경 준비
        val feedbackId = UUID.randomUUID()
        val newStatus = FeedbackStatus.RESOLVED

        val existingFeedback = Feedback(
            id = feedbackId,
            userId = "user123",
            chatId = "chat456",
            isPositive = true,
            status = FeedbackStatus.PENDING,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )

        val updatedFeedback = existingFeedback.copy(status = newStatus)

        every { feedbackRepository.findById(feedbackId) } returns Mono.just(existingFeedback)
        every { feedbackRepository.save(any()) } returns Mono.just(updatedFeedback)

        // When - 피드백 상태 변경
        val response = feedbackService.updateFeedbackStatus(feedbackId, newStatus)

        // Then - 응답 검증
        assertNotNull(response)
        assertEquals(newStatus, response.status)

        verify(exactly = 1) { feedbackRepository.findById(feedbackId) }
        verify(exactly = 1) { feedbackRepository.save(any()) }
    }

    @Test
    @DisplayName("존재하지 않는 피드백 상태 변경 시도 시 예외 발생 테스트")
    fun updateFeedbackStatusNotFoundFailure() {
        // Given - 존재하지 않는 피드백 ID
        val feedbackId = UUID.randomUUID()
        val newStatus = FeedbackStatus.RESOLVED

        every { feedbackRepository.findById(feedbackId) } returns Mono.empty()

        // When & Then - 예외 발생
        val exception = assertThrows(NoSuchElementException::class.java) {
            runTest {
                feedbackService.updateFeedbackStatus(feedbackId, newStatus)
            }
        }

        assertTrue(exception.message!!.contains("피드백을 찾을 수 없습니다"))
        verify(exactly = 1) { feedbackRepository.findById(feedbackId) }
        verify(exactly = 0) { feedbackRepository.save(any()) }
    }
}
