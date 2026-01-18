package com.rokyai.springaipoc.chat.service

import com.rokyai.springaipoc.chat.dto.ChatHistoryDto
import com.rokyai.springaipoc.chat.dto.ChatHistoryListRequest
import com.rokyai.springaipoc.chat.dto.ChatHistoryListResponse
import com.rokyai.springaipoc.chat.dto.ThreadWithChatsDto
import com.rokyai.springaipoc.chat.enums.SortDirection
import com.rokyai.springaipoc.chat.repository.ChatHistoryRepository
import com.rokyai.springaipoc.chat.repository.ThreadRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

/**
 * 관리자용 채팅 서비스
 *
 * 관리자가 모든 사용자의 채팅 히스토리를 조회하거나 관리하는 기능을 제공합니다.
 */
@Service
class AdminChatService(
    private val chatHistoryRepository: ChatHistoryRepository,
    private val threadRepository: ThreadRepository
) {

    /**
     * 모든 채팅 히스토리를 조회합니다.
     *
     * 관리자는 모든 사용자의 스레드와 채팅 히스토리를 조회할 수 있습니다.
     * 페이지네이션과 정렬을 지원합니다.
     *
     * @param request 조회 조건을 포함한 요청 객체
     * @return 스레드별로 그룹화된 채팅 히스토리 목록
     */
    suspend fun getAllChatHistory(request: ChatHistoryListRequest): ChatHistoryListResponse {
        val offset = request.page * request.size

        val threads = if (request.sortDirection == SortDirection.DESC) {
            threadRepository.findAllWithPaginationDesc(request.size, offset)
        } else {
            threadRepository.findAllWithPaginationAsc(request.size, offset)
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
}
