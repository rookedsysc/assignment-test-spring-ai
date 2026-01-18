package com.rokyai.springaipoc.feedback.controller

import com.rokyai.springaipoc.feedback.dto.FeedbackCreateRequest
import com.rokyai.springaipoc.feedback.dto.FeedbackResponse
import com.rokyai.springaipoc.feedback.entity.FeedbackStatus
import com.rokyai.springaipoc.feedback.service.FeedbackService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * FeedbackController 단위 테스트
 */
@DisplayName("FeedbackController 테스트")
class FeedbackControllerTest {

    private val feedbackService: FeedbackService = mockk()
    private lateinit var feedbackController: FeedbackController

    @BeforeEach
    fun setup() {
        feedbackController = FeedbackController(feedbackService)
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

        val expectedResponse = FeedbackResponse(
            id = UUID.randomUUID(),
            userId = request.userId,
            chatId = request.chatId,
            isPositive = request.isPositive,
            status = FeedbackStatus.PENDING,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )

        coEvery { feedbackService.createFeedback(request) } returns expectedResponse

        // When - 피드백 생성 API 호출
        val response = feedbackController.createFeedback(request)

        // Then - 응답 검증
        assertNotNull(response)
        assertEquals(expectedResponse.userId, response.userId)
        assertEquals(expectedResponse.chatId, response.chatId)
        assertEquals(expectedResponse.isPositive, response.isPositive)

        coVerify(exactly = 1) { feedbackService.createFeedback(request) }
    }

    @Test
    @DisplayName("사용자 피드백 목록 조회 성공 테스트")
    fun getUserFeedbacksSuccess() = runTest {
        // Given - 사용자 피드백 조회 요청 준비
        val userId = "user123"
        val page = 0
        val size = 10
        val sort = "createdAt,desc"

        val expectedFeedbacks = listOf(
            FeedbackResponse(
                id = UUID.randomUUID(),
                userId = userId,
                chatId = "chat1",
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            ),
            FeedbackResponse(
                id = UUID.randomUUID(),
                userId = userId,
                chatId = "chat2",
                isPositive = false,
                status = FeedbackStatus.RESOLVED,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        coEvery {
            feedbackService.getUserFeedbacks(
                userId = userId,
                isPositive = null,
                pageable = any()
            )
        } returns expectedFeedbacks

        // When - 피드백 목록 조회 API 호출
        val responses = feedbackController.getUserFeedbacks(userId, null, page, size, sort)

        // Then - 응답 검증
        assertEquals(2, responses.size)
        assertTrue(responses.all { it.userId == userId })

        coVerify(exactly = 1) {
            feedbackService.getUserFeedbacks(
                userId = userId,
                isPositive = null,
                pageable = any()
            )
        }
    }

    @Test
    @DisplayName("긍정 피드백만 필터링하여 조회 성공 테스트")
    fun getUserFeedbacksWithFilterSuccess() = runTest {
        // Given - 긍정 피드백만 필터링
        val userId = "user123"
        val isPositive = true
        val page = 0
        val size = 10
        val sort = "createdAt,desc"

        val expectedFeedbacks = listOf(
            FeedbackResponse(
                id = UUID.randomUUID(),
                userId = userId,
                chatId = "chat1",
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        coEvery {
            feedbackService.getUserFeedbacks(
                userId = userId,
                isPositive = isPositive,
                pageable = any()
            )
        } returns expectedFeedbacks

        // When - 긍정 피드백만 조회
        val responses = feedbackController.getUserFeedbacks(userId, isPositive, page, size, sort)

        // Then - 응답 검증
        assertEquals(1, responses.size)
        assertTrue(responses.all { it.isPositive })

        coVerify(exactly = 1) {
            feedbackService.getUserFeedbacks(
                userId = userId,
                isPositive = isPositive,
                pageable = any()
            )
        }
    }

}
