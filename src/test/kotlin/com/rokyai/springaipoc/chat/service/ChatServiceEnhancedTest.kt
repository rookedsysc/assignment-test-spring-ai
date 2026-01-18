package com.rokyai.springaipoc.chat.service

import com.rokyai.springaipoc.chat.dto.ChatHistoryListRequest
import com.rokyai.springaipoc.chat.dto.ChatRequest
import com.rokyai.springaipoc.chat.dto.ThreadDeleteRequest
import com.rokyai.springaipoc.chat.entity.ChatHistory
import com.rokyai.springaipoc.chat.entity.Thread
import com.rokyai.springaipoc.chat.enums.ChatProvider
import com.rokyai.springaipoc.chat.enums.SortDirection
import com.rokyai.springaipoc.chat.factory.ChatClientFactory
import com.rokyai.springaipoc.chat.repository.ChatHistoryRepository
import com.rokyai.springaipoc.chat.repository.ThreadRepository
import com.rokyai.springaipoc.chat.dto.ChatResponse
import io.mockk.*
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.openai.OpenAiChatOptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * ChatService 확장 기능 단위 테스트
 *
 * TDD 방식으로 작성된 테스트:
 * - 스레드 관리 (30분 규칙)
 * - 스트리밍/비스트리밍 통합 응답
 * - 채팅 히스토리 조회 (페이지네이션, 정렬)
 * - 스레드 삭제
 */
@DisplayName("ChatService 확장 기능 테스트")
class ChatServiceEnhancedTest {

    private val chatHistoryRepository: ChatHistoryRepository = mockk()
    private val threadRepository: ThreadRepository = mockk()
    private val chatClientFactory: ChatClientFactory = mockk()
    private val openAiChatClient: ChatClient = mockk()
    private val perplexityChatClient: ChatClient = mockk()
    private lateinit var chatService: ChatService
    private lateinit var adminChatService: AdminChatService

    @BeforeEach
    fun setup() {
        clearAllMocks()
        chatService = ChatService(
            chatHistoryRepository = chatHistoryRepository,
            threadRepository = threadRepository,
            chatClientFactory = chatClientFactory
        )
        adminChatService = AdminChatService(
            chatHistoryRepository = chatHistoryRepository,
            threadRepository = threadRepository
        )
    }

    // ========== 스레드 관리 테스트 ==========

    @Test
    @DisplayName("첫 질문 시 새 스레드가 생성되어야 함")
    fun shouldCreateNewThreadForFirstQuestion() = runTest {
        // Given - 사용자의 첫 질문
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val request = ChatRequest(
            message = "첫 번째 질문입니다",
            isStreaming = false
        )

        val newThreadId = UUID.randomUUID()
        val newThread = Thread(
            id = newThreadId,
            userId = userId.toString(),
            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
            updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
        )

        every { threadRepository.findLatestByUserId(userId.toString()) } returns Mono.empty()
        every { threadRepository.save(any()) } returns Mono.just(newThread)
        every { chatHistoryRepository.save(any()) } returns Mono.just(mockk())
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(any()) } returns Flux.empty()

        every { chatClientFactory.getClient(any()) } returns openAiChatClient
        every { chatClientFactory.getOptions(any()) } returns OpenAiChatOptions.builder().model("gpt-4o").build()

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)
        val mockCallResponseSpec: ChatClient.CallResponseSpec = mockk(relaxed = true)

        every { openAiChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.call() } returns mockCallResponseSpec
        every { mockCallResponseSpec.content() } returns "응답입니다"

        // When
        val response = chatService.chat(request, userId).collectList().awaitSingle().first()

        // Then - 새 스레드가 생성되었는지 확인 (생성 1회 + 업데이트 1회 = 총 2회)
        verify(exactly = 2) { threadRepository.save(any()) }
        assertNotNull(response)
    }

    @Test
    @DisplayName("마지막 질문 후 30분 이내 질문 시 기존 스레드를 재사용해야 함")
    fun shouldReuseThreadWithin30Minutes() = runTest {
        // Given - 29분 전에 마지막 질문이 있었던 상황
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val existingThreadId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val twentyNineMinutesAgo = now.minusMinutes(29)

        val existingThread = Thread(
            id = existingThreadId,
            userId = userId.toString(),
            createdAt = twentyNineMinutesAgo.minusHours(1),
            updatedAt = twentyNineMinutesAgo
        )

        val request = ChatRequest(
            message = "두 번째 질문입니다",
            isStreaming = false
        )

        every { threadRepository.findLatestByUserId(userId.toString()) } returns Mono.just(existingThread)
        every { threadRepository.save(any()) } returns Mono.just(existingThread.copy(updatedAt = now))
        every { chatHistoryRepository.save(any()) } returns Mono.just(mockk())
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(existingThreadId) } returns Flux.empty()

        every { chatClientFactory.getClient(any()) } returns openAiChatClient
        every { chatClientFactory.getOptions(any()) } returns OpenAiChatOptions.builder().model("gpt-4o").build()

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)
        val mockCallResponseSpec: ChatClient.CallResponseSpec = mockk(relaxed = true)

        every { openAiChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.call() } returns mockCallResponseSpec
        every { mockCallResponseSpec.content() } returns "응답입니다"

        // When
        val response = chatService.chat(request, userId).collectList().awaitSingle().first()

        // Then - 기존 스레드가 재사용되고, updatedAt만 갱신되었는지 확인 (업데이트 1회만)
        verify(exactly = 1) { threadRepository.save(match { it.id == existingThreadId }) }
        assertNotNull(response)
    }

    @Test
    @DisplayName("마지막 질문 후 30분 초과 시 새 스레드가 생성되어야 함")
    fun shouldCreateNewThreadAfter30Minutes() = runTest {
        // Given - 31분 전에 마지막 질문이 있었던 상황
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val oldThreadId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val thirtyOneMinutesAgo = now.minusMinutes(31)

        val oldThread = Thread(
            id = oldThreadId,
            userId = userId.toString(),
            createdAt = thirtyOneMinutesAgo.minusHours(1),
            updatedAt = thirtyOneMinutesAgo
        )

        val newThreadId = UUID.randomUUID()
        val newThread = Thread(
            id = newThreadId,
            userId = userId.toString(),
            createdAt = now,
            updatedAt = now
        )

        val request = ChatRequest(
            message = "30분 후 새 질문입니다",
            isStreaming = false
        )

        every { threadRepository.findLatestByUserId(userId.toString()) } returns Mono.just(oldThread)
        every { threadRepository.save(any()) } returns Mono.just(newThread)
        every { chatHistoryRepository.save(any()) } returns Mono.just(mockk())
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(any()) } returns Flux.empty()

        every { chatClientFactory.getClient(any()) } returns openAiChatClient
        every { chatClientFactory.getOptions(any()) } returns OpenAiChatOptions.builder().model("gpt-4o").build()

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)
        val mockCallResponseSpec: ChatClient.CallResponseSpec = mockk(relaxed = true)

        every { openAiChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.call() } returns mockCallResponseSpec
        every { mockCallResponseSpec.content() } returns "응답입니다"

        // When
        val response = chatService.chat(request, userId).collectList().awaitSingle().first()

        // Then - 새 스레드가 생성되었는지 확인 (생성 1회 + 업데이트 1회 = 총 2회, 기존 스레드 ID와 다름)
        verify(atLeast = 1) { threadRepository.save(match { it.id != oldThreadId }) }
        assertNotNull(response)
    }

    // ========== 채팅 생성 테스트 (스트리밍/비스트리밍) ==========

    @Test
    @DisplayName("isStreaming=false일 때 완전한 응답을 반환해야 함")
    fun shouldReturnCompleteResponseWhenNotStreaming() = runTest {
        // Given
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val threadId = UUID.randomUUID()
        val request = ChatRequest(
            message = "테스트 질문",
            isStreaming = false
        )

        val thread = Thread(id = threadId, userId = userId.toString())

        every { threadRepository.findLatestByUserId(userId.toString()) } returns Mono.empty()
        every { threadRepository.save(any()) } returns Mono.just(thread)
        every { chatHistoryRepository.save(any()) } returns Mono.just(mockk())
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(any()) } returns Flux.empty()

        every { chatClientFactory.getClient(any()) } returns openAiChatClient
        every { chatClientFactory.getOptions(any()) } returns OpenAiChatOptions.builder().model("gpt-4o").build()

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)
        val mockCallResponseSpec: ChatClient.CallResponseSpec = mockk(relaxed = true)

        every { openAiChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.call() } returns mockCallResponseSpec
        every { mockCallResponseSpec.content() } returns "완전한 응답"

        // When
        val response = chatService.chat(request, userId).collectList().awaitSingle().first()

        // Then
        assertNotNull(response)
        assertEquals("완전한 응답", response.message)
        verify(exactly = 1) { mockRequestSpec.call() }
    }

    @Test
    @DisplayName("isStreaming=true일 때 스트리밍 응답을 반환해야 함")
    fun shouldReturnStreamingResponseWhenStreaming() {
        // Given
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val threadId = UUID.randomUUID()
        val request = ChatRequest(
            message = "테스트 질문",
            isStreaming = true
        )

        val thread = Thread(id = threadId, userId = userId.toString())

        every { threadRepository.findLatestByUserId(userId.toString()) } returns Mono.empty()
        every { threadRepository.save(any()) } returns Mono.just(thread)
        every { chatHistoryRepository.save(any()) } returns Mono.just(mockk())
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(any()) } returns Flux.empty()

        every { chatClientFactory.getClient(any()) } returns openAiChatClient
        every { chatClientFactory.getOptions(any()) } returns OpenAiChatOptions.builder().model("gpt-4o").build()

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)
        val mockStreamResponseSpec: ChatClient.StreamResponseSpec = mockk(relaxed = true)

        every { openAiChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.stream() } returns mockStreamResponseSpec
        every { mockStreamResponseSpec.content() } returns Flux.just("스트리밍 ", "응답")

        // When
        val responseFlux = chatService.chatStream(request, userId)

        // Then
        StepVerifier.create(responseFlux)
            .expectNext(ChatResponse("스트리밍 ", threadId))
            .expectNext(ChatResponse("응답", threadId))
            .verifyComplete()
    }

    @Test
    @DisplayName("모델을 지정하면 해당 ChatClient를 사용해야 함")
    fun shouldUseDifferentChatClientForSpecifiedModel() = runTest {
        // Given - Perplexity 모델 지정
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val threadId = UUID.randomUUID()
        val request = ChatRequest(
            message = "테스트 질문",
            isStreaming = false,
            provider = ChatProvider.PERPLEXITY_SONAR
        )

        val thread = Thread(id = threadId, userId = userId.toString())

        every { threadRepository.findLatestByUserId(userId.toString()) } returns Mono.empty()
        every { threadRepository.save(any()) } returns Mono.just(thread)
        every { chatHistoryRepository.save(any()) } returns Mono.just(mockk())
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(any()) } returns Flux.empty()

        every { chatClientFactory.getClient(ChatProvider.PERPLEXITY_SONAR) } returns perplexityChatClient
        every { chatClientFactory.getOptions(ChatProvider.PERPLEXITY_SONAR) } returns OpenAiChatOptions.builder().model("sonar").build()

        val mockRequestSpec: ChatClient.ChatClientRequestSpec = mockk(relaxed = true)
        val mockCallResponseSpec: ChatClient.CallResponseSpec = mockk(relaxed = true)

        every { perplexityChatClient.prompt() } returns mockRequestSpec
        every { mockRequestSpec.options(any()) } returns mockRequestSpec
        every { mockRequestSpec.user(any<String>()) } returns mockRequestSpec
        every { mockRequestSpec.call() } returns mockCallResponseSpec
        every { mockCallResponseSpec.content() } returns "Perplexity 응답"

        // When
        val response = chatService.chat(request, userId).collectList().awaitSingle().first()

        // Then - OpenAI가 아닌 Perplexity ChatClient가 사용되었는지 확인
        verify(exactly = 1) { perplexityChatClient.prompt() }
        verify(exactly = 0) { openAiChatClient.prompt() }
        assertNotNull(response)
    }

    // ========== 채팅 히스토리 조회 테스트 ==========

    @Test
    @DisplayName("사용자는 본인의 채팅 히스토리만 조회할 수 있어야 함")
    fun shouldRetrieveOnlyOwnChatHistory() = runTest {
        // Given
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val threadId1 = UUID.randomUUID()
        val threadId2 = UUID.randomUUID()

        val request = ChatHistoryListRequest(
            userId = userId.toString(),
            isAdmin = false,
            page = 0,
            size = 10
        )

        val threads = listOf(
            Thread(id = threadId1, userId = userId.toString()),
            Thread(id = threadId2, userId = userId.toString())
        )

        every { threadRepository.findAllByUserIdWithPaginationDesc(userId.toString(), 10, 0) } returns Flux.fromIterable(threads)
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(threadId1) } returns Flux.just(
            ChatHistory(
                id = UUID.randomUUID(),
                threadId = threadId1,
                userId = userId.toString(),
                userMessage = "질문1",
                assistantMessage = "답변1",
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(threadId2) } returns Flux.just(
            ChatHistory(
                id = UUID.randomUUID(),
                threadId = threadId2,
                userId = userId.toString(),
                userMessage = "질문2",
                assistantMessage = "답변2",
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        // When
        val response = chatService.getChatHistory(request)

        // Then
        assertNotNull(response)
        assertEquals(2, response.threads.size)
        assertEquals(userId.toString(), response.threads[0].userId)
        verify(exactly = 1) { threadRepository.findAllByUserIdWithPaginationDesc(userId.toString(), 10, 0) }
    }

    @Test
    @DisplayName("관리자는 모든 채팅 히스토리를 조회할 수 있어야 함")
    fun adminShouldRetrieveAllChatHistory() = runTest {
        // Given
        val request = ChatHistoryListRequest(
            userId = null,
            isAdmin = true,
            page = 0,
            size = 10
        )

        val threadId = UUID.randomUUID()
        val threads = listOf(Thread(id = threadId, userId = "anyUser"))

        every { threadRepository.findAllWithPaginationDesc(10, 0) } returns Flux.fromIterable(threads)
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(threadId) } returns Flux.just(
            ChatHistory(
                id = UUID.randomUUID(),
                threadId = threadId,
                userId = "anyUser",
                userMessage = "질문",
                assistantMessage = "답변",
                createdAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )

        // When
        val response = adminChatService.getAllChatHistory(request)

        // Then
        assertNotNull(response)
        verify(exactly = 1) { threadRepository.findAllWithPaginationDesc(10, 0) }
    }

    @Test
    @DisplayName("정렬 방향을 ASC로 지정하면 오래된 순서로 조회해야 함")
    fun shouldRetrieveInAscendingOrder() = runTest {
        // Given
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val request = ChatHistoryListRequest(
            userId = userId.toString(),
            isAdmin = false,
            sortDirection = SortDirection.ASC,
            page = 0,
            size = 10
        )

        val threadId = UUID.randomUUID()
        val threads = listOf(Thread(id = threadId, userId = userId.toString()))

        every { threadRepository.findAllByUserIdWithPaginationAsc(userId.toString(), 10, 0) } returns Flux.fromIterable(threads)
        every { chatHistoryRepository.findAllByThreadIdOrderByCreatedAtAsc(threadId) } returns Flux.empty()

        // When
        chatService.getChatHistory(request)

        // Then
        verify(exactly = 1) { threadRepository.findAllByUserIdWithPaginationAsc(userId.toString(), 10, 0) }
    }

    // ========== 스레드 삭제 테스트 ==========

    @Test
    @DisplayName("사용자는 본인의 스레드만 삭제할 수 있어야 함")
    fun shouldDeleteOwnThreadOnly() = runTest {
        // Given
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val threadId = UUID.randomUUID()
        val request = ThreadDeleteRequest(userId = userId.toString(), threadId = threadId)

        every { threadRepository.existsByIdAndUserId(threadId, userId.toString()) } returns Mono.just(true)
        every { chatHistoryRepository.deleteAllByThreadId(threadId) } returns Flux.empty()
        every { threadRepository.deleteById(any<UUID>()) } returns Mono.empty()

        // When
        chatService.deleteThread(request)

        // Then
        verify(exactly = 1) { threadRepository.existsByIdAndUserId(threadId, userId.toString()) }
        verify(exactly = 1) { chatHistoryRepository.deleteAllByThreadId(threadId) }
        verify(exactly = 1) { threadRepository.deleteById(threadId) }
    }

    @Test
    @DisplayName("다른 사용자의 스레드 삭제 시 예외가 발생해야 함")
    fun shouldThrowExceptionWhenDeletingOtherUsersThread() {
        // Given
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000123")
        val threadId = UUID.randomUUID()
        val request = ThreadDeleteRequest(userId = userId.toString(), threadId = threadId)

        every { threadRepository.existsByIdAndUserId(threadId, userId.toString()) } returns Mono.just(false)

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runTest {
                chatService.deleteThread(request)
            }
        }

        assertTrue(exception.message!!.contains("권한이 없거나 존재하지 않는 스레드"))
        verify(exactly = 0) { chatHistoryRepository.deleteAllByThreadId(any()) }
        verify(exactly = 0) { threadRepository.deleteById(any<UUID>()) }
    }
}
