package com.rokyai.springaipoc.chat.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

/**
 * 스레드 삭제 요청 DTO
 *
 * @property userId 사용자 ID (필수값)
 * @property threadId 삭제할 스레드 ID (필수값)
 */
data class ThreadDeleteRequest(
    @field:NotBlank(message = "사용자 ID는 비어있을 수 없습니다.")
    val userId: String,

    @field:NotNull(message = "스레드 ID는 필수입니다.")
    val threadId: UUID
)
