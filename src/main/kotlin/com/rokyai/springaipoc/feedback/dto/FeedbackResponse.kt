package com.rokyai.springaipoc.feedback.dto

import com.rokyai.springaipoc.feedback.entity.Feedback
import com.rokyai.springaipoc.feedback.entity.FeedbackStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 피드백 응답 DTO
 * 피드백 정보를 클라이언트에 전달할 때 사용
 *
 * @property id 피드백 고유 ID
 * @property userId 피드백 작성자 사용자 ID
 * @property chatId 피드백 대상 대화 ID
 * @property isPositive 긍정/부정 피드백 여부
 * @property status 피드백 상태
 * @property createdAt 피드백 생성 시간 (UTC)
 */
@Schema(description = "피드백 응답")
data class FeedbackResponse(
    @Schema(description = "피드백 고유 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,

    @Schema(description = "피드백 작성자 사용자 ID", example = "user123")
    val userId: String,

    @Schema(description = "피드백 대상 대화 ID", example = "chat456")
    val chatId: String,

    @Schema(description = "긍정(true) 또는 부정(false) 피드백 여부", example = "true")
    val isPositive: Boolean,

    @Schema(description = "피드백 상태 (PENDING: 대기 중, RESOLVED: 해결됨)", example = "PENDING")
    val status: FeedbackStatus,

    @Schema(description = "피드백 생성 시간 (UTC)", example = "2025-01-18T10:30:00Z")
    val createdAt: OffsetDateTime
) {
    companion object {
        /**
         * Feedback 엔티티를 FeedbackResponse DTO로 변환
         *
         * @param feedback 변환할 Feedback 엔티티
         * @return 변환된 FeedbackResponse DTO
         */
        fun from(feedback: Feedback): FeedbackResponse {
            return FeedbackResponse(
                id = feedback.id!!,
                userId = feedback.userId,
                chatId = feedback.chatId,
                isPositive = feedback.isPositive,
                status = feedback.status,
                createdAt = feedback.createdAt
            )
        }
    }
}
