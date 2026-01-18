package com.rokyai.springaipoc.chat.controller

import com.rokyai.springaipoc.chat.dto.*
import com.rokyai.springaipoc.chat.service.AdminChatService
import com.rokyai.springaipoc.chat.service.ChatService
import com.rokyai.springaipoc.chat.util.SecurityUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

/**
 * AI 채팅 API Controller
 *
 * 다양한 AI 제공자(OpenAI, Perplexity 등)를 활용하여 사용자 메시지에 대한 응답을 생성합니다.
 * 스레드 기반 대화 관리, 스트리밍/비스트리밍 응답, 히스토리 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat API", description = "AI와 대화하는 API")
class ChatController(
    private val chatService: ChatService,
    private val adminChatService: AdminChatService
) {
    /**
     * AI와 대화합니다.
     *
     * isStreaming 파라미터에 따라 완전한 응답 또는 스트리밍 응답을 반환합니다.
     * 스레드 관리 로직(30분 규칙)을 통해 대화 컨텍스트를 유지합니다.
     *
     * @param request 사용자가 보낼 메시지와 옵션을 포함한 요청 객체
     * @return isStreaming=false인 경우 ChatResponse, isStreaming=true인 경우 Flux<ChatResponse>
     */
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE])
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "AI와 대화",
        description = "사용자 메시지를 AI에게 전송하고 응답을 받습니다. " +
                "isStreaming 파라미터에 따라 완전한 응답(JSON) 또는 스트리밍 응답(SSE)을 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상적으로 응답을 받았습니다.",
                content = [
                    Content(schema = Schema(implementation = ChatResponse::class)),
                    Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청입니다. 메시지가 비어있거나 형식이 올바르지 않습니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류가 발생했습니다. AI API 호출 실패 또는 응답 생성 실패",
                content = [Content()]
            )
        ]
    )
    suspend fun chat(@Valid @RequestBody request: ChatRequest): Flux<ChatResponse> {
        val userId = SecurityUtil.requireCurrentUserId()
        return if (request.isStreaming) {
            chatService.chatStream(request, userId)
        } else {
            chatService.chat(request, userId)
        }
    }

    /**
     * 채팅 히스토리를 조회합니다.
     *
     * 스레드 단위로 그룹화된 대화 목록을 반환합니다.
     * 일반 사용자는 본인의 히스토리만, 관리자는 모든 히스토리를 조회할 수 있습니다.
     * 페이지네이션과 정렬을 지원합니다.
     *
     * @param request 조회 조건을 포함한 요청 객체
     * @return 스레드별로 그룹화된 채팅 히스토리 목록
     */
    @PostMapping("/history")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "채팅 히스토리 조회",
        description = "스레드 단위로 그룹화된 채팅 히스토리를 조회합니다. " +
                "일반 사용자는 본인의 히스토리만, 관리자는 모든 히스토리를 조회할 수 있습니다. " +
                "페이지네이션과 정렬(ASC/DESC)을 지원합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정상적으로 히스토리를 조회했습니다.",
                content = [Content(schema = Schema(implementation = ChatHistoryListResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청입니다. userId가 누락되었거나 페이지 파라미터가 올바르지 않습니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류가 발생했습니다.",
                content = [Content()]
            )
        ]
    )
    suspend fun getChatHistory(@Valid @RequestBody request: ChatHistoryListRequest): ChatHistoryListResponse {
        return if (request.isAdmin) {
            adminChatService.getAllChatHistory(request)
        } else {
            val userId = SecurityUtil.requireCurrentUserId().toString()
            chatService.getChatHistory(request.copy(userId = userId))
        }
    }

    /**
     * 스레드를 삭제합니다.
     *
     * 스레드에 속한 모든 채팅 히스토리도 함께 삭제됩니다.
     * 사용자는 본인의 스레드만 삭제할 수 있습니다.
     *
     * @param request 삭제할 스레드 정보를 포함한 요청 객체
     */
    @DeleteMapping("/thread")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "스레드 삭제",
        description = "특정 스레드와 해당 스레드의 모든 채팅 히스토리를 삭제합니다. " +
                "사용자는 본인의 스레드만 삭제할 수 있습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "정상적으로 스레드를 삭제했습니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청입니다. threadId 또는 userId가 누락되었습니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "403",
                description = "권한이 없습니다. 다른 사용자의 스레드는 삭제할 수 없습니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 스레드입니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류가 발생했습니다.",
                content = [Content()]
            )
        ]
    )
    suspend fun deleteThread(@Valid @RequestBody request: ThreadDeleteRequest) {
        val userId = SecurityUtil.requireCurrentUserId().toString()
        chatService.deleteThread(request.copy(userId = userId))
    }
}
