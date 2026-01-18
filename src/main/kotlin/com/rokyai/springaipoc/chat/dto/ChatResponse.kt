package com.rokyai.springaipoc.chat.dto

import java.util.UUID

/**
 * ChatGPT로부터 받은 응답 DTO
 *
 * @property message ChatGPT가 생성한 응답 메시지
 * @property threadId 대화가 속한 스레드 ID
 */
data class ChatResponse(
    val message: String,
    val threadId: UUID? = null
)
