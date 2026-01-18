package com.rokyai.springaipoc.chat.dto

/**
 * 채팅 히스토리 목록 조회 응답 DTO
 *
 * @property threads 스레드별로 그룹화된 채팅 히스토리 목록
 * @property page 현재 페이지 번호
 * @property size 페이지 크기
 * @property totalElements 전체 스레드 개수
 */
data class ChatHistoryListResponse(
    val threads: List<ThreadWithChatsDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long
)
