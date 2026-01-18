package com.rokyai.springaipoc.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.*

/**
 * 회원 관리자 승격 요청 DTO
 *
 * @property userId 관리자로 승격할 회원의 ID
 */
@Schema(description = "회원 관리자 승격 요청")
data class PromoteUserRequest(
    @field:NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "승격할 사용자 ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    val userId: UUID
)
