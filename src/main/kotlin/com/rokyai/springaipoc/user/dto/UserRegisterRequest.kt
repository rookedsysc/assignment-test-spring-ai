package com.rokyai.springaipoc.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 회원가입 요청 DTO
 *
 * @property email 이메일 주소 (유효한 이메일 형식 필수)
 * @property password 패스워드 (8자 이상)
 * @property name 사용자 이름
 */
@Schema(description = "회원가입 요청")
data class UserRegisterRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @Schema(description = "이메일 주소", example = "user@example.com", required = true)
    val email: String,

    @field:NotBlank(message = "패스워드는 필수입니다")
    @field:Size(min = 8, message = "패스워드는 최소 8자 이상이어야 합니다")
    @Schema(description = "패스워드 (8자 이상)", example = "password123!", required = true)
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다")
    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    val name: String
)
