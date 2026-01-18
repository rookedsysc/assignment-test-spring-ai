package com.rokyai.springaipoc.user.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * JWT 토큰 응답 DTO
 *
 * @property accessToken JWT 액세스 토큰
 * @property tokenType 토큰 타입 (Bearer)
 * @property expiresIn 토큰 만료 시간 (초)
 */
@Schema(description = "JWT 토큰 응답")
data class TokenResponse(
    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,

    @Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String = "Bearer",

    @Schema(description = "토큰 만료 시간 (초)", example = "3600")
    val expiresIn: Long
)
