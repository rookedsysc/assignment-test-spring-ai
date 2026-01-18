package com.rokyai.springaipoc.chat.dto

import java.time.OffsetDateTime
import java.util.UUID

/**
 * 채팅 히스토리 DTO
 *
 * @property id 채팅 히스토리 ID
 * @property userMessage 사용자 메시지
 * @property assistantMessage AI 응답 메시지
 * @property createdAt 생성일시
 */
data class ChatHistoryDto(
    val id: UUID,
    val userMessage: String,
    val assistantMessage: String,
    val createdAt: OffsetDateTime
)
