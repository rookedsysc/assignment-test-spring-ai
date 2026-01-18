package com.rokyai.springaipoc.feedback.controller

import com.rokyai.springaipoc.feedback.dto.FeedbackResponse
import com.rokyai.springaipoc.feedback.dto.FeedbackUpdateStatusRequest
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * AdminFeedbackController 단위 테스트
 */
@DisplayName("AdminFeedbackController 테스트")
class AdminFeedbackControllerTest {

    private val feedbackService: FeedbackService = mockk()
    private lateinit var adminFeedbackController: AdminFeedbackController

    @BeforeEach
    fun setup() {
        adminFeedbackController = AdminFeedbackController(feedbackService)
    }

    @Test
    @DisplayName("관리자가 모든 피드백 조회 성공 테스트")
    fun getAllFeedbacksForAdminSuccess() = runTest {
        // Given - 모든 피드백 조회 준비
        val page = 0
        val size = 10
        val sort = "createdAt,desc"

        val expectedFeedbacks = listOf(
            FeedbackResponse(
                id = UUID.randomUUID(),
                userId = "user1",
                chatId = "chat1",
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            ),
            FeedbackResponse(
                id = UUID.randomUUID(),
                userId = "user2",
                chatId = "chat2",
                isPositive = false,
                status = FeedbackStatus.RESOLVED,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        coEvery {
            feedbackService.getAllFeedbacks(
                isPositive = null,
                pageable = any()
            )
        } returns expectedFeedbacks

        // When - 모든 피드백 조회
        val responses = adminFeedbackController.getAllFeedbacks(null, page, size, sort)

        // Then - 응답 검증
        assertEquals(2, responses.size)

        coVerify(exactly = 1) {
            feedbackService.getAllFeedbacks(
                isPositive = null,
                pageable = any()
            )
        }
    }

    @Test
    @DisplayName("피드백 상태 변경 성공 테스트")
    fun updateFeedbackStatusSuccess() = runTest {
        // Given - 피드백 상태 변경 준비
        val feedbackId = UUID.randomUUID()
        val request = FeedbackUpdateStatusRequest(status = FeedbackStatus.RESOLVED)

        val expectedResponse = FeedbackResponse(
            id = feedbackId,
            userId = "user123",
            chatId = "chat456",
            isPositive = true,
            status = FeedbackStatus.RESOLVED,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )

        coEvery { feedbackService.updateFeedbackStatus(feedbackId, request.status) } returns expectedResponse

        // When - 피드백 상태 변경 API 호출
        val response = adminFeedbackController.updateFeedbackStatus(feedbackId, request)

        // Then - 응답 검증
        assertNotNull(response)
        assertEquals(FeedbackStatus.RESOLVED, response.status)

        coVerify(exactly = 1) { feedbackService.updateFeedbackStatus(feedbackId, request.status) }
    }

    @Test
    @DisplayName("관리자가 긍정 피드백만 필터링하여 조회 성공 테스트")
    fun getAllFeedbacksWithPositiveFilterSuccess() = runTest {
        // Given - 긍정 피드백만 필터링
        val isPositive = true
        val page = 0
        val size = 10
        val sort = "createdAt,desc"

        val expectedFeedbacks = listOf(
            FeedbackResponse(
                id = UUID.randomUUID(),
                userId = "user1",
                chatId = "chat1",
                isPositive = true,
                status = FeedbackStatus.PENDING,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            ),
            FeedbackResponse(
                id = UUID.randomUUID(),
                userId = "user2",
                chatId = "chat2",
                isPositive = true,
                status = FeedbackStatus.RESOLVED,
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        coEvery {
            feedbackService.getAllFeedbacks(
                isPositive = isPositive,
                pageable = any()
            )
        } returns expectedFeedbacks

        // When - 긍정 피드백만 조회
        val responses = adminFeedbackController.getAllFeedbacks(isPositive, page, size, sort)

        // Then - 응답 검증
        assertEquals(2, responses.size)
        assertTrue(responses.all { it.isPositive })

        coVerify(exactly = 1) {
            feedbackService.getAllFeedbacks(
                isPositive = isPositive,
                pageable = any()
            )
        }
    }
}
