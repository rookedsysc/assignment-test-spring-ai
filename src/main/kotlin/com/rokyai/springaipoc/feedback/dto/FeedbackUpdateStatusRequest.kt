package com.rokyai.springaipoc.feedback.dto

import com.rokyai.springaipoc.feedback.entity.FeedbackStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * 피드백 상태 변경 요청 DTO
 * 관리자가 피드백의 상태를 변경할 때 사용
 *
 * @property status 변경할 피드백 상태 (필수)
 */
@Schema(description = "피드백 상태 변경 요청")
data class FeedbackUpdateStatusRequest(
    @field:NotNull(message = "상태는 필수입니다.")
    @Schema(description = "변경할 피드백 상태 (PENDING 또는 RESOLVED)", example = "RESOLVED", required = true)
    val status: FeedbackStatus
)
