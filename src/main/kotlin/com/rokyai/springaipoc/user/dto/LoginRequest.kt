package com.rokyai.springaipoc.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * 로그인 요청 DTO
 *
 * @property email 이메일 주소
 * @property password 패스워드
 */
@Schema(description = "로그인 요청")
data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @Schema(description = "이메일 주소", example = "user@example.com", required = true)
    val email: String,

    @field:NotBlank(message = "패스워드는 필수입니다")
    @Schema(description = "패스워드", example = "password123!", required = true)
    val password: String
)
