package com.rokyai.springaipoc.user.service

import com.rokyai.springaipoc.chat.entity.ChatHistory
import com.rokyai.springaipoc.chat.repository.ChatHistoryRepository
import com.rokyai.springaipoc.user.entity.User
import com.rokyai.springaipoc.user.repository.LoginHistoryRepository
import com.rokyai.springaipoc.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

@DisplayName("Analytics Service 테스트")
class AnalyticsServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var loginHistoryRepository: LoginHistoryRepository
    private lateinit var chatHistoryRepository: ChatHistoryRepository
    private lateinit var analyticsService: AnalyticsService

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        loginHistoryRepository = mockk()
        chatHistoryRepository = mockk()
        analyticsService = AnalyticsService(userRepository, loginHistoryRepository, chatHistoryRepository)
    }

    @Test
    @DisplayName("지난 24시간 동안의 활동 통계를 반환해야 한다")
    fun getDailyActivityStats() {
        // Given
        val signupCount = 10L
        val loginCount = 20L
        val chatCount = 30L

        every { userRepository.countByCreatedAtAfter(any()) } returns Mono.just(signupCount)
        every { loginHistoryRepository.countByCreatedAtAfter(any()) } returns Mono.just(loginCount)
        every { chatHistoryRepository.countByCreatedAtAfter(any()) } returns Mono.just(chatCount)

        // When
        val result = analyticsService.getDailyActivityStats()

        // Then
        StepVerifier.create(result)
            .assertNext { stats ->
                assertEquals(signupCount, stats.signupCount)
                assertEquals(loginCount, stats.loginCount)
                assertEquals(chatCount, stats.chatCount)
            }
            .verifyComplete()

        verify { userRepository.countByCreatedAtAfter(any()) }
        verify { loginHistoryRepository.countByCreatedAtAfter(any()) }
        verify { chatHistoryRepository.countByCreatedAtAfter(any()) }
    }

    @Test
    @DisplayName("지난 24시간 동안의 채팅 보고서(CSV)를 생성해야 한다")
    fun generateDailyChatReport() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = "test@test.com", password = "pw", name = "Test User")
        val chat1 = ChatHistory(
            id = UUID.randomUUID(),
            threadId = UUID.randomUUID(),
            userId = userId.toString(),
            userMessage = "Hello",
            assistantMessage = "Hi",
            createdAt = OffsetDateTime.now()
        )
        val chat2 = ChatHistory(
            id = UUID.randomUUID(),
            threadId = UUID.randomUUID(),
            userId = userId.toString(),
            userMessage = "Question",
            assistantMessage = "Answer",
            createdAt = OffsetDateTime.now().minusHours(1)
        )

        every { chatHistoryRepository.findAllByCreatedAtAfter(any()) } returns Flux.just(chat1, chat2)
        every { userRepository.findById(userId) } returns Mono.just(user)

        // When
        val result = analyticsService.generateDailyChatReport()

        // Then
        StepVerifier.create(result)
            .assertNext { csv ->
                assertTrue(csv.contains("User Email,User Name,User Message,Assistant Message,Created At"))
                assertTrue(csv.contains("test@test.com"))
                assertTrue(csv.contains("Test User"))
                assertTrue(csv.contains("Hello"))
                assertTrue(csv.contains("Question"))
            }
            .verifyComplete()
    }
}
