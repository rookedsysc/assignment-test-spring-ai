package com.rokyai.springaipoc.chat.dto

import com.rokyai.springaipoc.chat.enums.SortDirection
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * 채팅 히스토리 목록 조회 요청 DTO
 *
 * @property userId 사용자 ID (필수값, 일반 사용자용)
 * @property isAdmin 관리자 여부 (true: 모든 채팅 조회, false: 본인 채팅만 조회, 기본값: false)
 * @property sortDirection 정렬 방향 (ASC: 오름차순, DESC: 내림차순, 기본값: DESC)
 * @property page 페이지 번호 (0부터 시작, 기본값: 0)
 * @property size 페이지 크기 (기본값: 20, 최소값: 1)
 */
data class ChatHistoryListRequest(
    val userId: String? = null,

    val isAdmin: Boolean = false,

    val sortDirection: SortDirection = SortDirection.DESC,

    @field:Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    val page: Int = 0,

    @field:Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    val size: Int = 20
)
