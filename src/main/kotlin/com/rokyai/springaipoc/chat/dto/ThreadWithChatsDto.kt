package com.rokyai.springaipoc.chat.dto

import java.time.OffsetDateTime
import java.util.UUID

/**
 * 스레드와 채팅 목록 DTO
 *
 * @property threadId 스레드 ID
 * @property userId 사용자 ID
 * @property createdAt 스레드 생성일시
 * @property updatedAt 스레드 마지막 업데이트 일시
 * @property chats 스레드에 속한 채팅 히스토리 목록
 */
data class ThreadWithChatsDto(
    val threadId: UUID,
    val userId: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val chats: List<ChatHistoryDto>
)
