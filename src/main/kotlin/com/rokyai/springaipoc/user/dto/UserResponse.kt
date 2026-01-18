package com.rokyai.springaipoc.user.dto

import com.rokyai.springaipoc.user.entity.Role
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.*

/**
 * 사용자 응답 DTO
 *
 * @property id 사용자 고유 ID
 * @property email 이메일 주소
 * @property name 사용자 이름
 * @property role 사용자 권한
 * @property createdAt 생성일시
 */
@Schema(description = "사용자 정보 응답")
data class UserResponse(
    @Schema(description = "사용자 ID", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: UUID,

    @Schema(description = "이메일 주소", example = "user@example.com")
    val email: String,

    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String,

    @Schema(description = "사용자 권한", example = "MEMBER")
    val role: Role,

    @Schema(description = "생성일시", example = "2024-01-01T00:00:00Z")
    val createdAt: Instant
)
