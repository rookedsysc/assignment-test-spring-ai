package com.rokyai.springaipoc.chat.service

import com.rokyai.springaipoc.chat.dto.ChatRequest
import com.rokyai.springaipoc.chat.entity.Thread
import com.rokyai.springaipoc.chat.enums.ChatProvider
import com.rokyai.springaipoc.chat.factory.ChatClientFactory
import com.rokyai.springaipoc.chat.repository.ChatHistoryRepository
import com.rokyai.springaipoc.chat.repository.ThreadRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatOptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * ChatService 단위 테스트
 */
@DisplayName("ChatService 테스트")
class ChatServiceTest {

    private val chatHistoryRepository: ChatHistoryRepository = mockk()
    private val threadRepository: ThreadRepository = mockk()
    private val openAiChatClient: ChatClient = mockk()
    private val chatClientFactory: ChatClientFactory = mockk()
    private lateinit var chatService: ChatService

    @BeforeEach
    fun setup() {
        every { chatHistoryRepository.save(any()) } returns Mono.just(mockk())
        every { threadRepository.findLatestByUserId(any()) } returns Mono.empty()
        every { threadRepository.save(any()) } returns Mono.just(
            Thread(
                id = UUID.randomUUID(),
                userId = "testUser",
                createdAt = OffsetDateTime.now(ZoneOffset.UTC),
                updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(any()) } returns Flux.empty()
        every { chatClientFactory.getClient(any()) } returns openAiChatClient
        every { chatClientFactory.getOptions(any()) } returns OpenAiChatOptions.builder().model("gpt-4o").build()

        chatService = ChatService(
            chatHistoryRepository = chatHistoryRepository,
            threadRepository = threadRepository,
            chatClientFactory = chatClientFactory
        )
    }

    @Test
    @DisplayName("정상적인 메시지 전송 및 응답 수신 테스트")
    fun chatSuccess() = runTest {
        // Given - AI에게 전송할 메시지 준비
        val userId = UUID.randomUUID()
        val request = ChatRequest(
            message = "안녕하세요!",
            isStreaming = false
        )
        val expectedResponse = "안녕하세요! 무엇을 도와드릴까요?"

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)
        val mockCallResponseSpec: ChatClient.CallResponseSpec = mockk(relaxed = true)

        every { openAiChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.call() } returns mockCallResponseSpec
        every { mockCallResponseSpec.content() } returns expectedResponse

        // When - ChatService를 통해 메시지 전송
        val response = chatService.chat(request, userId)

        // Then - 응답이 정상적으로 반환되는지 검증
        assertNotNull(response)
        assertEquals(expectedResponse, response.message)
    }

    @Test
    @DisplayName("AI API 호출 실패 시 예외 전파 테스트")
    fun chatFailure() {
        // Given - API 호출이 실패하는 상황 설정
        val userId = UUID.randomUUID()
        val request = ChatRequest(
            message = "테스트 메시지",
            isStreaming = false
        )

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)

        every { openAiChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.call() } throws RuntimeException("API 호출 실패")

        // When & Then - 예외가 전파되는지 검증
        val exception = assertThrows(RuntimeException::class.java) {
            runTest {
                chatService.chat(request, userId)
            }
        }

        assertEquals("API 호출 실패", exception.message)
    }

    @Test
    @DisplayName("빈 메시지 전송 시 정상 동작 테스트")
    fun chatWithEmptyMessage() = runTest {
        // Given - 빈 메시지 전송
        val userId = UUID.randomUUID()
        val request = ChatRequest(
            message = "",
            isStreaming = false
        )
        val expectedResponse = "죄송하지만, 메시지를 이해하지 못했습니다."

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)
        val mockCallResponseSpec: ChatClient.CallResponseSpec = mockk(relaxed = true)

        every { openAiChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.call() } returns mockCallResponseSpec
        every { mockCallResponseSpec.content() } returns expectedResponse

        // When - 빈 메시지 전송
        val response = chatService.chat(request, userId)

        // Then - 응답이 정상적으로 반환되는지 검증
        assertNotNull(response)
        assertEquals(expectedResponse, response.message)
    }
}
