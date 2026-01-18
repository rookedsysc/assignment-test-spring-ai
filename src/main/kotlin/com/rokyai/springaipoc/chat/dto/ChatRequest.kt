package com.rokyai.springaipoc.chat.dto

import com.rokyai.springaipoc.chat.enums.ChatProvider
import jakarta.validation.constraints.NotBlank

/**
 * AI 채팅 요청 DTO
 *
 * @property message 사용자가 AI에게 보낼 메시지 (필수값, 공백 불가)
 * @property isStreaming 스트리밍 응답 여부 (true: 스트리밍, false: 완전한 응답, 기본값: true)
 * @property provider AI 제공자 및 모델 (기본값: OPENAI_GPT4O)
 */
data class ChatRequest(
    @field:NotBlank(message = "메시지는 비어있을 수 없습니다.")
    val message: String,

    val isStreaming: Boolean = true,

    val provider: ChatProvider = ChatProvider.OPENAI_GPT4O
)
