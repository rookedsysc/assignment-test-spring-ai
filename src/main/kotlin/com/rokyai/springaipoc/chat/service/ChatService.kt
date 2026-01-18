package com.rokyai.springaipoc.chat.service

import com.rokyai.springaipoc.chat.dto.*
import com.rokyai.springaipoc.chat.entity.ChatHistory
import com.rokyai.springaipoc.chat.entity.Thread
import com.rokyai.springaipoc.chat.enums.SortDirection
import com.rokyai.springaipoc.chat.factory.ChatClientFactory
import com.rokyai.springaipoc.chat.repository.ChatHistoryRepository
import com.rokyai.springaipoc.chat.repository.ThreadRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * ChatGPT와 통신하는 서비스
 *
 * Spring AI의 ChatClient를 사용하여 OpenAI API와 비동기 통신을 수행합니다.
 * 스레드 기반 대화 관리 및 다양한 AI 제공자를 지원합니다.
 */
@Service
class ChatService(
    private val chatHistoryRepository: ChatHistoryRepository,
    private val threadRepository: ThreadRepository,
    private val chatClientFactory: ChatClientFactory
) {
    companion object {
        const val THREAD_TIMEOUT_MINUTES = 30L
    }

    /**
     * AI에게 메시지를 전송하고 응답을 받습니다.
     *
     * isStreaming 파라미터에 따라 완전한 응답 또는 스트리밍 응답을 결정합니다.
     * 스레드 관리 로직(30분 규칙)을 적용하여 대화 컨텍스트를 유지합니다.
     *
     * @param request 사용자가 보낼 메시지를 포함한 요청 객체
     * @param userId 사용자 ID (Spring Security에서 추출)
     * @return AI의 응답 메시지를 포함한 응답 객체
     */
    suspend fun chat(request: ChatRequest, userId: UUID): Flux<ChatResponse> {
        val chatClient = chatClientFactory.getClient(request.provider)
        val chatOptions = chatClientFactory.getOptions(request.provider)

        val thread = getOrCreateThread(userId.toString())

        val previousChats = chatHistoryRepository
            .findAllByThreadIdOrderByCreatedAtAsc(thread.id!!)
            .collectList()
            .awaitSingle()

        val generatedMessage = Mono.fromCallable {
            val promptBuilder = chatClient.prompt()
                .options(chatOptions)

            if (previousChats.isNotEmpty()) {
                previousChats.forEach { chat ->
                    promptBuilder
                        .user(chat.userMessage)
                }
            }

            promptBuilder
                .user(request.message)
                .call()
                .content()
        }
            .subscribeOn(Schedulers.boundedElastic())
            .awaitSingle()
            ?: throw IllegalStateException("AI 응답 생성 실패")

        val chatHistory = ChatHistory(
            threadId = thread.id,
            userId = userId.toString(),
            userMessage = request.message,
            assistantMessage = generatedMessage,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )
        chatHistoryRepository.save(chatHistory).awaitSingle()

        threadRepository.save(
            thread.copy(updatedAt = OffsetDateTime.now(ZoneOffset.UTC))
        ).awaitSingle()

        return Flux.just(ChatResponse(message = generatedMessage, threadId = thread.id))
    }

    /**
     * AI에게 메시지를 전송하고 스트리밍 방식으로 응답을 받습니다.
     *
     * SSE (Server-Sent Events)를 통해 실시간으로 생성되는 텍스트를 전송합니다.
     * 스레드 관리 로직을 적용하여 대화 컨텍스트를 유지합니다.
     *
     * @param request 사용자가 보낼 메시지를 포함한 요청 객체
     * @param userId 사용자 ID (Spring Security에서 추출)
     * @return AI의 응답 메시지 스트림
     */
    fun chatStream(request: ChatRequest, userId: UUID): Flux<ChatResponse> {
        val messageBuilder = StringBuilder()
        val userIdString = userId.toString()

        return Mono.fromCallable {
            val chatClient = chatClientFactory.getClient(request.provider)
            chatClient
        }
            .flatMapMany { chatClient ->
                val chatOptions = chatClientFactory.getOptions(request.provider)

                threadRepository.findLatestByUserId(userIdString)
                    .switchIfEmpty(Mono.defer {
                        val newThread = Thread(
                            userId = userIdString,
                            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
                            updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
                        )
                        threadRepository.save(newThread)
                    })
                    .flatMapMany { thread ->
                        val now = OffsetDateTime.now(ZoneOffset.UTC)
                        val needsNewThread = thread.updatedAt
                            .plusMinutes(THREAD_TIMEOUT_MINUTES)
                            .isBefore(now)

                        val activeThread = if (needsNewThread) {
                            val newThread = Thread(
                                userId = userIdString,
                                createdAt = now,
                                updatedAt = now
                            )
                            threadRepository.save(newThread).block()!!
                        } else {
                            thread
                        }

                        chatHistoryRepository
                            .findAllByThreadIdOrderByCreatedAtAsc(activeThread.id!!)
                            .collectList()
                            .flatMapMany { previousChats ->
                                Flux.defer {
                                    val promptBuilder = chatClient.prompt()
                                        .options(chatOptions)

                                    if (previousChats.isNotEmpty()) {
                                        previousChats.forEach { chat ->
                                            promptBuilder.user(chat.userMessage)
                                        }
                                    }

                                    promptBuilder
                                        .user(request.message)
                                        .stream()
                                        .content()
                                }
                                    .filter { text -> text.isNotBlank() }
                                    .doOnNext { text -> messageBuilder.append(text) }
                                    .map { text -> ChatResponse(message = text, threadId = activeThread.id) }
                                    .doOnComplete {
                                        val chatHistory = ChatHistory(
                                            threadId = activeThread.id,
                                            userId = userIdString,
                                            userMessage = request.message,
                                            assistantMessage = messageBuilder.toString(),
                                            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
                                        )
                                        chatHistoryRepository.save(chatHistory).subscribe()

                                        threadRepository.save(
                                            activeThread.copy(updatedAt = OffsetDateTime.now(ZoneOffset.UTC))
                                        ).subscribe()
                                    }
                            }
                    }
            }
            .onErrorResume { error ->
                val errorMessage = error.message ?: "알 수 없는 오류가 발생했습니다."
                Flux.just(ChatResponse(message = "Error: $errorMessage"))
            }
    }

    /**
     * 채팅 히스토리를 조회합니다.
     *
     * 스레드 단위로 그룹화되어 반환되며, 페이지네이션과 정렬을 지원합니다.
     * 일반 사용자는 본인의 히스토리만 조회할 수 있습니다.
     *
     * @param request 조회 조건을 포함한 요청 객체
     * @return 스레드별로 그룹화된 채팅 히스토리 목록
     */
    suspend fun getChatHistory(request: ChatHistoryListRequest): ChatHistoryListResponse {
        val offset = request.page * request.size
        val userId = request.userId
            ?: throw IllegalArgumentException("userId가 필수입니다.")

        val threads = if (request.sortDirection == SortDirection.DESC) {
            threadRepository.findAllByUserIdWithPaginationDesc(userId, request.size, offset)
        } else {
            threadRepository.findAllByUserIdWithPaginationAsc(userId, request.size, offset)
        }.collectList().awaitSingle()

        val threadsWithChats = threads.map { thread ->
            val chats = chatHistoryRepository
                .findAllByThreadIdOrderByCreatedAtAsc(thread.id!!)
                .map { chatHistory ->
                    ChatHistoryDto(
                        id = chatHistory.id!!,
                        userMessage = chatHistory.userMessage,
                        assistantMessage = chatHistory.assistantMessage,
                        createdAt = chatHistory.createdAt
                    )
                }
                .collectList()
                .awaitSingle()

            ThreadWithChatsDto(
                threadId = thread.id,
                userId = thread.userId,
                createdAt = thread.createdAt,
                updatedAt = thread.updatedAt,
                chats = chats
            )
        }

        return ChatHistoryListResponse(
            threads = threadsWithChats,
            page = request.page,
            size = request.size,
            totalElements = threads.size.toLong()
        )
    }

    /**
     * 스레드를 삭제합니다.
     *
     * 스레드에 속한 모든 채팅 히스토리도 함께 삭제됩니다.
     * 사용자는 본인의 스레드만 삭제할 수 있습니다.
     *
     * @param request 삭제할 스레드 정보를 포함한 요청 객체
     * @throws IllegalArgumentException 권한이 없거나 존재하지 않는 스레드인 경우
     */
    suspend fun deleteThread(request: ThreadDeleteRequest) {
        val exists = threadRepository
            .existsByIdAndUserId(request.threadId, request.userId)
            .awaitSingle()

        if (!exists) {
            throw IllegalArgumentException(
                "권한이 없거나 존재하지 않는 스레드입니다. threadId: ${request.threadId}, userId: ${request.userId}"
            )
        }

        chatHistoryRepository.deleteAllByThreadId(request.threadId).collectList().awaitSingle()
        threadRepository.deleteById(request.threadId).awaitSingleOrNull()
    }

    /**
     * 사용자의 최신 스레드를 가져오거나 새로 생성합니다.
     *
     * 30분 규칙을 적용하여 기존 스레드를 재사용하거나 새 스레드를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 활성 스레드
     */
    private suspend fun getOrCreateThread(userId: String): Thread {
        val latestThread = threadRepository.findLatestByUserId(userId).awaitSingleOrNull()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        return if (latestThread == null) {
            val newThread = Thread(
                userId = userId,
                createdAt = now,
                updatedAt = now
            )
            threadRepository.save(newThread).awaitSingle()
        } else {
            val needsNewThread = latestThread.updatedAt
                .plusMinutes(THREAD_TIMEOUT_MINUTES)
                .isBefore(now)

            if (needsNewThread) {
                val newThread = Thread(
                    userId = userId,
                    createdAt = now,
                    updatedAt = now
                )
                threadRepository.save(newThread).awaitSingle()
            } else {
                latestThread
            }
        }
    }
}
