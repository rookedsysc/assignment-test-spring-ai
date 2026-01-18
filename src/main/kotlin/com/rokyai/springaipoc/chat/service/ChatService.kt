package com.rokyai.springaipoc.chat.service

import com.rokyai.springaipoc.chat.dto.ChatRequest
import com.rokyai.springaipoc.chat.dto.ChatResponse
import com.rokyai.springaipoc.chat.entity.ChatHistory
import com.rokyai.springaipoc.chat.repository.ChatHistoryRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * ChatGPT와 통신하는 서비스
 *
 * Spring AI의 ChatClient를 사용하여 OpenAI API와 비동기 통신을 수행합니다.
 * Context7 MCP를 통합하여 라이브러리 문서를 자동으로 검색하고 더 정확한 답변을 제공합니다.
 * WebFlux 환경에서 Coroutines를 활용하여 Non-blocking 방식으로 동작합니다.
 */
@Service
class ChatService(
    private val chatHistoryRepository: ChatHistoryRepository,
    @Qualifier("openAiChatClient") private val openAiChatClient: ChatClient,
) {
    /**
     * ChatGPT에게 메시지를 전송하고 응답을 받습니다.
     *
     * Context7 MCP를 통해 라이브러리 관련 질문에 대한 최신 문서를 자동으로 검색합니다.
     * WebFlux 환경에서 blocking 호출을 피하기 위해 boundedElastic 스케줄러를 사용합니다.
     *
     * @param request 사용자가 보낼 메시지를 포함한 요청 객체
     * @return ChatGPT의 응답 메시지를 포함한 응답 객체
     */
    suspend fun chat(request: ChatRequest): ChatResponse {
        // ChatClient를 사용하여 Context7 MCP 도구를 자동으로 활용
        // AI가 필요시 자동으로 라이브러리 문서를 검색하여 더 정확한 답변을 생성합니다
        val generatedMessage = Mono.fromCallable {
            openAiChatClient.prompt()
                .user(request.message)
                .call()
                .content()
        }
            .subscribeOn(Schedulers.boundedElastic())
            .awaitSingle()
            ?: throw IllegalStateException("ChatGPT 응답 생성 실패")

        // 채팅 기록을 데이터베이스에 저장
        val chatHistory = ChatHistory(
            userMessage = request.message,
            assistantMessage = generatedMessage,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )
        chatHistoryRepository.save(chatHistory).awaitSingle()

        return ChatResponse(message = generatedMessage)
    }

    /**
     * ChatGPT에게 메시지를 전송하고 스트리밍 방식으로 응답을 받습니다.
     *
     * Context7 MCP를 통해 라이브러리 관련 질문에 대한 최신 문서를 자동으로 검색합니다.
     * SSE (Server-Sent Events)를 통해 실시간으로 생성되는 텍스트를 전송합니다.
     * 사용자는 전체 응답이 완료될 때까지 기다리지 않고 생성되는 즉시 텍스트를 받을 수 있습니다.
     *
     * @param request 사용자가 보낼 메시지를 포함한 요청 객체
     * @return ChatGPT의 응답 메시지 스트림 (Flux<String>)
     */
    fun chatStream(request: ChatRequest): Flux<String> {
        val messageBuilder = StringBuilder()

        return Flux.defer {
            openAiChatClient.prompt()
                .user(request.message)
                .stream()
                .content()
        }
            .filter { text ->
                text.isNotBlank()
            }
            .doOnNext { text ->
                // 스트리밍되는 각 텍스트 청크를 수집
                messageBuilder.append(text)
            }
            .doOnComplete {
                // 스트리밍이 완료되면 채팅 기록을 데이터베이스에 저장
                val chatHistory = ChatHistory(
                    userMessage = request.message,
                    assistantMessage = messageBuilder.toString(),
                    createdAt = OffsetDateTime.now(ZoneOffset.UTC)
                )
                chatHistoryRepository.save(chatHistory).subscribe()
            }
            .onErrorResume { error ->
                Flux.just("Error: ${error.message ?: "알 수 없는 오류가 발생했습니다."}")
            }
    }
}
