package com.rokyai.springaipoc.feedback.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * 피드백 생성 요청 DTO
 * 사용자가 특정 대화에 대한 피드백을 생성할 때 사용
 *
 * @property userId 피드백을 작성하는 사용자 ID (필수)
 * @property chatId 피드백 대상 대화 ID (필수)
 * @property isPositive 긍정(true) 또는 부정(false) 피드백 여부 (필수)
 */
@Schema(description = "피드백 생성 요청")
data class FeedbackCreateRequest(
    @field:NotBlank(message = "사용자 ID는 필수입니다.")
    @Schema(description = "피드백을 작성하는 사용자 ID", example = "user123", required = true)
    val userId: String,

    @field:NotBlank(message = "대화 ID는 필수입니다.")
    @Schema(description = "피드백 대상 대화 ID", example = "chat456", required = true)
    val chatId: String,

    @Schema(description = "긍정(true) 또는 부정(false) 피드백 여부", example = "true", required = true)
    val isPositive: Boolean
)
